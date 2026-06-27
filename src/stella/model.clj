(ns stella.model)

(defn- menu-item [label disabled]
  {:label label :disabled disabled})

(defn- separator []
  {:separator true})

(defn- file-menu []
  {:label "File"
   :items [(menu-item "New" true)
           (menu-item "Open..." true)
           (menu-item "Save" true)
           (menu-item "Save As..." true)
           (separator)
           (menu-item "Quit" false)]})

(defn- edit-menu []
  {:label "Edit"
   :items [(menu-item "Undo" true)
           (menu-item "Redo" true)
           (separator)
           (menu-item "Cut" true)
           (menu-item "Copy" true)
           (menu-item "Paste" true)]})

(defn- view-menu []
  {:label "View"
   :items [(menu-item "Zoom In" true)
           (menu-item "Zoom Out" true)
           (menu-item "Reset Zoom" true)]})

(defn- help-menu []
  {:label "Help"
   :items [(menu-item "About Stella" false)]})

(defn default-diagram []
  {:stocks {}
   :sources {}
   :sinks {}
   :flows {}
   :converters {}
   :connectors {}
   :placement-mode :idle
   :flow-draft nil
   :connector-draft nil
   :next-stock-num 1
   :next-source-num 1
   :next-sink-num 1
   :next-flow-num 1
   :next-converter-num 1
   :next-connector-num 1})

(defn default-shell []
  {:showing true
   :window-title "Stella"
   :about-visible false
   :about-text ""
   :menu-bar [(file-menu)
              (edit-menu)
              (view-menu)
              (help-menu)]
   :diagram (default-diagram)})

(defn top-level-menus
  [shell]
  (mapv :label (:menu-bar shell)))

(defn menu-includes?
  [shell menu-label]
  (some #(= menu-label (:label %)) (:menu-bar shell)))

(defn- menu-items
  [shell]
  (mapcat :items (:menu-bar shell)))

(defn menu-item-disabled?
  [shell item-label]
  (when-let [item (first (filter #(and (= (:label %) item-label) (not (:separator %)))
                                 (menu-items shell)))]
    (:disabled item)))

(defn window-title
  [shell]
  (:window-title shell))

(defn showing?
  [shell]
  (:showing shell))

(defn about-visible?
  [shell]
  (:about-visible shell))

(defn about-text
  [shell]
  (:about-text shell))

(defn diagram-empty?
  [shell]
  (empty? (:stocks (:diagram shell))))

(defn stock-count
  [diagram]
  (count (:stocks diagram)))

(defn- stock-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:stocks diagram))))

(defn stock-exists?
  [diagram name]
  (some? (stock-entry-by-name diagram name)))

(defn stock-position
  [diagram name]
  (when-let [[_ stock] (stock-entry-by-name diagram name)]
    [(:x stock) (:y stock)]))

(defn- stock-field
  [diagram name key]
  (when-let [[_ stock] (stock-entry-by-name diagram name)]
    (get stock key)))

(defn stock-initial-value
  [diagram name]
  (stock-field diagram name :initial-value))

(defn stock-min-value
  [diagram name]
  (stock-field diagram name :min-value))

(defn stock-max-value
  [diagram name]
  (stock-field diagram name :max-value))

(defn stock-named?
  [diagram name]
  (stock-exists? diagram name))

(defn- numeric-value
  [value]
  (Double/parseDouble (str value)))

(defn- value-within-bounds?
  [value min-value max-value]
  (let [num (numeric-value value)
        min-num (numeric-value min-value)]
    (and (>= num min-num)
         (or (nil? max-value)
             (<= num (numeric-value max-value))))))

(defn- rename-endpoint-id
  [endpoint kind old-name new-name]
  (if (and (= kind (:kind endpoint)) (= old-name (:id endpoint)))
    (assoc endpoint :id new-name)
    endpoint))

(defn- rename-stock-endpoints
  [diagram old-name new-name]
  (let [rename (fn [endpoint]
                 (rename-endpoint-id endpoint :stock old-name new-name))
        update-item (fn [item]
                      (-> item
                          (update :from rename)
                          (update :to rename)))]
    (-> diagram
        (update :flows #(reduce-kv (fn [m id flow] (assoc m id (update-item flow)))
                                 {}
                                 %))
        (update :connectors #(reduce-kv (fn [m id connector]
                                          (assoc m id (update-item connector)))
                                       {}
                                       %)))))

(defn- rename-blocked?
  [exists? diagram old-name new-name]
  (or (not (seq (str new-name)))
      (= old-name new-name)
      (not (exists? diagram old-name))
      (exists? diagram new-name)))

(defn set-stock-name
  [diagram old-name new-name]
  (if (rename-blocked? stock-exists? diagram old-name new-name)
    diagram
    (let [[id _] (stock-entry-by-name diagram old-name)]
      (-> diagram
          (assoc-in [:stocks id :name] new-name)
          (rename-stock-endpoints old-name new-name)))))

(defn set-stock-initial-value
  [diagram name value]
  (if-let [[id stock] (stock-entry-by-name diagram name)]
    (if (value-within-bounds? value (:min-value stock "0") (:max-value stock))
      (assoc-in diagram [:stocks id :initial-value] (str value))
      diagram)
    diagram))

(defn- set-stock-bound
  [diagram name bound-key value valid?]
  (if-let [[id stock] (stock-entry-by-name diagram name)]
    (if (valid? stock value)
      (assoc-in diagram [:stocks id bound-key] (str value))
      diagram)
    diagram))

(defn set-stock-min
  [diagram name min-value]
  (set-stock-bound diagram name :min-value min-value
                   (fn [stock value]
                     (let [min-num (numeric-value value)
                           max-num (when-let [max-v (:max-value stock)]
                                     (numeric-value max-v))]
                       (or (nil? max-num) (<= min-num max-num))))))

(defn set-stock-max
  [diagram name max-value]
  (set-stock-bound diagram name :max-value max-value
                   (fn [stock value]
                     (>= (numeric-value value)
                         (numeric-value (:min-value stock "0"))))))

(defn clear-stock-max
  [diagram name]
  (if-let [[id _] (stock-entry-by-name diagram name)]
    (assoc-in diagram [:stocks id :max-value] nil)
    diagram))

(defn placement-disarmed?
  [diagram]
  (= :idle (:placement-mode diagram)))

(defn arm-stock-placement
  [diagram]
  (assoc diagram :placement-mode :stock))

(defn place-stock
  [diagram x y]
  (if (= :stock (:placement-mode diagram))
    (let [num (:next-stock-num diagram)
          name (str "Stock" num)
          id (keyword (str "stock-" num))
          stock {:name name :initial-value "0" :min-value "0" :max-value nil :x x :y y}]
      (-> diagram
          (assoc-in [:stocks id] stock)
          (assoc :placement-mode :idle)
          (update :next-stock-num inc)))
    diagram))

(defn stocks
  [diagram]
  (vals (:stocks diagram)))

(defn- num-from-name
  [prefix name]
  (Integer/parseInt (subs name (count prefix))))

(defn- fixture-item
  [diagram collection next-key id-prefix name-prefix name x y entry]
  (let [num (num-from-name name-prefix name)
        id (keyword (str id-prefix num))]
    (-> diagram
        (assoc-in [collection id] (assoc entry :name name :x x :y y))
        (update next-key #(max % (inc num))))))

(defn fixture-stock
  [diagram name x y]
  (fixture-item diagram :stocks :next-stock-num "stock-" "Stock" name x y
                {:initial-value "0" :min-value "0" :max-value nil}))

(defn- endpoint-ref
  [kind id]
  {:kind kind :id id})

(defn- source-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:sources diagram))))

(defn- sink-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:sinks diagram))))

(defn source-exists?
  [diagram name]
  (some? (source-entry-by-name diagram name)))

(defn sink-exists?
  [diagram name]
  (some? (sink-entry-by-name diagram name)))

(defn source-position
  [diagram name]
  (when-let [[_ source] (source-entry-by-name diagram name)]
    [(:x source) (:y source)]))

(defn sink-position
  [diagram name]
  (when-let [[_ sink] (sink-entry-by-name diagram name)]
    [(:x sink) (:y sink)]))

(declare flow-midpoint converter-position)

(defn- endpoint-position-resolvers
  []
  {:stock stock-position
   :source source-position
   :sink sink-position
   :converter converter-position
   :flow flow-midpoint})

(defn endpoint-position
  [diagram {:keys [kind id]}]
  (when-let [resolve (get (endpoint-position-resolvers) kind)]
    (resolve diagram id)))

(def ^:private endpoint-anchor-offsets
  {[:stock :right] [80.0 25.0]
   [:stock :left] [0.0 25.0]
   [:source :right] [80.0 25.0]
   [:sink :left] [0.0 25.0]
   [:converter :right] [50.0 25.0]
   [:converter :left] [0.0 25.0]
   [:flow :right] [10.0 0.0]
   [:flow :left] [-10.0 0.0]})

(defn endpoint-anchor
  [[x y] kind side]
  (let [[dx dy] (get endpoint-anchor-offsets [kind side] [0.0 25.0])]
    [(+ x dx) (+ y dy)]))

(defn source-count
  [diagram]
  (count (:sources diagram)))

(defn sink-count
  [diagram]
  (count (:sinks diagram)))

(defn sources
  [diagram]
  (vals (:sources diagram)))

(defn sinks
  [diagram]
  (vals (:sinks diagram)))

(defn fixture-source
  [diagram name x y]
  (fixture-item diagram :sources :next-source-num "source-" "Source" name x y {}))

(defn fixture-sink
  [diagram name x y]
  (fixture-item diagram :sinks :next-sink-num "sink-" "Sink" name x y {}))

(defn arm-source-placement
  [diagram]
  (assoc diagram :placement-mode :source))

(defn arm-sink-placement
  [diagram]
  (assoc diagram :placement-mode :sink))

(defn- place-cloud
  [diagram mode collection next-key id-prefix label-prefix x y]
  (if (= mode (:placement-mode diagram))
    (let [num (get diagram next-key)
          name (str label-prefix num)
          id (keyword (str id-prefix num))]
      (-> diagram
          (assoc-in [collection id] {:name name :x x :y y})
          (assoc :placement-mode :idle)
          (update next-key inc)))
    diagram))

(defn place-source
  [diagram x y]
  (place-cloud diagram :source :sources :next-source-num "source-" "Source" x y))

(defn place-sink
  [diagram x y]
  (place-cloud diagram :sink :sinks :next-sink-num "sink-" "Sink" x y))

(defn source-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn sink-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn- flow-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:flows diagram))))

(defn flow-exists?
  [diagram name]
  (some? (flow-entry-by-name diagram name)))

(defn- flow-attribute
  [diagram name attribute]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (get flow attribute)))

(defn flow-from
  [diagram name]
  (flow-attribute diagram name :from))

(defn flow-to
  [diagram name]
  (flow-attribute diagram name :to))

(defn flow-endpoints
  [diagram name]
  (let [from (flow-from diagram name)
        to (flow-to diagram name)]
    (when (and from to)
      [(:id from) (:id to)])))

(defn flow-rate
  [diagram name]
  (flow-attribute diagram name :rate))

(defn- rename-endpoints-in-connectors
  [diagram kind old-name new-name]
  (let [rename #(rename-endpoint-id % kind old-name new-name)
        update-link #(-> % (update :from rename) (update :to rename))]
    (update diagram :connectors
            #(reduce-kv (fn [m id link] (assoc m id (update-link link))) {} %))))

(defn set-flow-name
  [diagram old-name new-name]
  (if (rename-blocked? flow-exists? diagram old-name new-name)
    diagram
    (let [[id _] (flow-entry-by-name diagram old-name)]
      (-> diagram
          (assoc-in [:flows id :name] new-name)
          (rename-endpoints-in-connectors :flow old-name new-name)))))

(defn- parseable-number?
  [value]
  (and (seq (str value))
       (try
         (numeric-value value)
         true
         (catch Exception _ false))))

(defn set-flow-rate
  [diagram name rate]
  (if-let [[id _] (flow-entry-by-name diagram name)]
    (if (parseable-number? rate)
      (assoc-in diagram [:flows id :rate] (str rate))
      diagram)
    diagram))

(defn flow-count
  [diagram]
  (count (:flows diagram)))

(defn flows
  [diagram]
  (vals (:flows diagram)))

(defn- fixture-named-link
  [diagram name-prefix counter-key id-prefix collection name attrs]
  (let [num (num-from-name name-prefix name)
        id (keyword (str id-prefix num))]
    (-> diagram
        (assoc-in [collection id] (assoc attrs :name name))
        (update counter-key #(max % (inc num))))))

(defn- fixture-endpoint-link
  [diagram name-prefix counter-key id-prefix collection name
   from-kind from-id to-kind to-id extra-attrs]
  (fixture-named-link diagram name-prefix counter-key id-prefix collection name
                      (merge {:from (endpoint-ref from-kind from-id)
                              :to (endpoint-ref to-kind to-id)}
                             extra-attrs)))

(defn fixture-flow
  [diagram flow-name from-stock to-stock]
  (fixture-endpoint-link diagram "Flow" :next-flow-num "flow-" :flows flow-name
                         :stock from-stock :stock to-stock {:rate "0"}))

(defn- valid-flow-pair?
  [from to]
  (case [(:kind from) (:kind to)]
    [:source :stock] true
    [:stock :stock] (not= (:id from) (:id to))
    [:stock :sink] true
    false))

(defn- create-collection-link!
  [diagram {:keys [collection counter name-prefix id-prefix item draft-key]} from to]
  (let [num (get diagram counter)
        name (str name-prefix num)
        id (keyword (str id-prefix num))]
    (-> diagram
        (assoc-in [collection id] (assoc item :name name :from from :to to))
        (assoc :placement-mode :idle draft-key nil)
        (update counter inc))))

(defn- create-flow!
  [diagram from to]
  (create-collection-link!
   diagram {:collection :flows
            :counter :next-flow-num
            :name-prefix "Flow"
            :id-prefix "flow-"
            :item {:rate "0"}
            :draft-key :flow-draft}
   from to))

(defn- arm-link-placement
  [diagram mode draft-key]
  (-> diagram
      (assoc :placement-mode mode)
      (assoc draft-key nil)))

(defn arm-flow-placement
  [diagram]
  (arm-link-placement diagram :flow :flow-draft))

(defn flow-placement-armed?
  [diagram]
  (= :flow (:placement-mode diagram)))

(defn- draft-from-endpoint
  [diagram kind name]
  (cond
    (and (= kind :source) (source-exists? diagram name))
    (assoc diagram :flow-draft {:from (endpoint-ref :source name)})

    (and (= kind :stock) (stock-exists? diagram name))
    (assoc diagram :flow-draft {:from (endpoint-ref :stock name)})

    :else diagram))

(defn select-flow-source
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram))
           (nil? (:flow-draft diagram))
           (not= kind :sink))
    (draft-from-endpoint diagram kind name)
    diagram))

(defn- clear-flow-draft
  [diagram]
  (assoc diagram :flow-draft nil))

(declare converter-exists?)

(defn- endpoint-existence-checks
  []
  {:stock stock-exists?
   :sink sink-exists?
   :converter converter-exists?
   :flow flow-exists?
   :source source-exists?})

(defn- endpoint-exists?
  [diagram kind name]
  (when-let [check (get (endpoint-existence-checks) kind)]
    (check diagram name)))

(defn- try-connect-flow
  [diagram from kind name]
  (let [to (endpoint-ref kind name)]
    (cond
      (= kind :source) (clear-flow-draft diagram)
      (and (#{:stock :sink} kind)
           (endpoint-exists? diagram kind name)
           (valid-flow-pair? from to)) (create-flow! diagram from to)
      :else (clear-flow-draft diagram))))

(defn connect-flow
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram)) (:flow-draft diagram))
    (try-connect-flow diagram (:from (:flow-draft diagram)) kind name)
    diagram))

(defn flow-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn- converter-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:converters diagram))))

(defn converter-exists?
  [diagram name]
  (some? (converter-entry-by-name diagram name)))

(defn converter-position
  [diagram name]
  (when-let [[_ converter] (converter-entry-by-name diagram name)]
    [(:x converter) (:y converter)]))

(defn converter-value
  [diagram name]
  (when-let [[_ converter] (converter-entry-by-name diagram name)]
    (:value converter)))

(defn converter-count
  [diagram]
  (count (:converters diagram)))

(defn converters
  [diagram]
  (vals (:converters diagram)))

(defn fixture-converter
  [diagram name x y]
  (fixture-item diagram :converters :next-converter-num "converter-" "Converter" name x y
                {:value "0"}))

(defn arm-converter-placement
  [diagram]
  (assoc diagram :placement-mode :converter))

(defn place-converter
  [diagram x y]
  (if (= :converter (:placement-mode diagram))
    (let [num (:next-converter-num diagram)
          name (str "Converter" num)
          id (keyword (str "converter-" num))]
      (-> diagram
          (assoc-in [:converters id] {:name name :value "0" :x x :y y})
          (assoc :placement-mode :idle)
          (update :next-converter-num inc)))
    diagram))

(defn converter-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn flow-midpoint
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (when-let [from-pos (endpoint-position diagram (:from flow))]
      (when-let [to-pos (endpoint-position diagram (:to flow))]
        (let [[fx fy] (endpoint-anchor from-pos (:kind (:from flow)) :right)
              [tx ty] (endpoint-anchor to-pos (:kind (:to flow)) :left)]
          [(/ (+ fx tx) 2.0) (/ (+ fy ty) 2.0)])))))

(def ^:private clickable-kinds-by-mode
  {:flow #{:stock :source :sink}
   :connector #{:stock :converter :flow}})

(defn endpoint-clickable?
  [diagram kind]
  (contains? (get clickable-kinds-by-mode (:placement-mode diagram)) kind))

(defn- connector-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:connectors diagram))))

(defn connector-exists?
  [diagram name]
  (some? (connector-entry-by-name diagram name)))

(defn- connector-attribute
  [diagram name attribute]
  (when-let [[_ connector] (connector-entry-by-name diagram name)]
    (get connector attribute)))

(defn connector-from
  [diagram name]
  (connector-attribute diagram name :from))

(defn connector-to
  [diagram name]
  (connector-attribute diagram name :to))

(defn connector-formula
  [diagram name]
  (connector-attribute diagram name :formula))

(defn set-converter-name
  [diagram old-name new-name]
  (if (rename-blocked? converter-exists? diagram old-name new-name)
    diagram
    (let [[id _] (converter-entry-by-name diagram old-name)]
      (-> diagram
          (assoc-in [:converters id :name] new-name)
          (rename-endpoints-in-connectors :converter old-name new-name)))))

(defn- converter-to-flow-connector-id
  [diagram converter-name]
  (some (fn [[id {:keys [from to]}]]
          (when (and (= :converter (:kind from))
                     (= converter-name (:id from))
                     (= :flow (:kind to)))
            id))
        (:connectors diagram)))

(defn set-converter-formula
  [diagram converter-name formula]
  (if (seq (str formula))
    (if-let [id (converter-to-flow-connector-id diagram converter-name)]
      (assoc-in diagram [:connectors id :formula] (str formula))
      diagram)
    diagram))

(defn fixture-connector
  [diagram connector-name from-converter to-flow]
  (fixture-endpoint-link diagram "Connector" :next-connector-num "connector-" :connectors
                         connector-name :converter from-converter :flow to-flow {:formula ""}))

(defn connector-count
  [diagram]
  (count (:connectors diagram)))

(defn connectors
  [diagram]
  (vals (:connectors diagram)))

(defn- valid-connector-pair?
  [from to]
  (case [(:kind from) (:kind to)]
    [:converter :flow] true
    [:stock :converter] true
    false))

(defn- create-connector!
  [diagram from to]
  (create-collection-link!
   diagram {:collection :connectors
            :counter :next-connector-num
            :name-prefix "Connector"
            :id-prefix "connector-"
            :item {:formula ""}
            :draft-key :connector-draft}
   from to))

(defn arm-connector-placement
  [diagram]
  (arm-link-placement diagram :connector :connector-draft))

(defn connector-placement-armed?
  [diagram]
  (= :connector (:placement-mode diagram)))

(defn select-connector-origin
  [diagram kind name]
  (if (and (= :connector (:placement-mode diagram))
           (nil? (:connector-draft diagram))
           (not= kind :flow)
           (not (contains? #{:source :sink} kind))
           (endpoint-exists? diagram kind name))
    (assoc diagram :connector-draft {:from (endpoint-ref kind name)})
    diagram))

(defn- try-connect-connector
  [diagram from kind name]
  (let [to (endpoint-ref kind name)]
    (cond
      (contains? #{:source :sink :stock} kind)
      (assoc diagram :connector-draft nil)

      (and (endpoint-exists? diagram kind name)
           (valid-connector-pair? from to))
      (create-connector! diagram from to)

      :else
      (assoc diagram :connector-draft nil))))

(defn connect-connector
  [diagram kind name]
  (if (and (= :connector (:placement-mode diagram))
           (:connector-draft diagram))
    (try-connect-connector diagram (:from (:connector-draft diagram)) kind name)
    diagram))

(defn connector-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:17:14.340962-05:00", :module-hash "-1920959411", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-289228109"} {:id "defn-/menu-item", :kind "defn-", :line 3, :end-line 4, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 6, :end-line 7, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 9, :end-line 16, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 18, :end-line 25, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 27, :end-line 31, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 33, :end-line 35, :hash "229400292"} {:id "defn/default-diagram", :kind "defn", :line 37, :end-line 52, :hash "1388292886"} {:id "defn/default-shell", :kind "defn", :line 54, :end-line 63, :hash "-1757765176"} {:id "defn/top-level-menus", :kind "defn", :line 65, :end-line 67, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 69, :end-line 71, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 73, :end-line 75, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 77, :end-line 81, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 83, :end-line 85, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 87, :end-line 89, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 91, :end-line 93, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 95, :end-line 97, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 99, :end-line 101, :hash "-256542367"} {:id "defn/stock-count", :kind "defn", :line 103, :end-line 105, :hash "-1679213761"} {:id "defn-/stock-entry-by-name", :kind "defn-", :line 107, :end-line 109, :hash "407514139"} {:id "defn/stock-exists?", :kind "defn", :line 111, :end-line 113, :hash "2041739431"} {:id "defn/stock-position", :kind "defn", :line 115, :end-line 118, :hash "69915530"} {:id "defn-/stock-field", :kind "defn-", :line 120, :end-line 123, :hash "-2043085379"} {:id "defn/stock-initial-value", :kind "defn", :line 125, :end-line 127, :hash "-567431284"} {:id "defn/stock-min-value", :kind "defn", :line 129, :end-line 131, :hash "1340209325"} {:id "defn/stock-max-value", :kind "defn", :line 133, :end-line 135, :hash "684410828"} {:id "defn/stock-named?", :kind "defn", :line 137, :end-line 139, :hash "-166675305"} {:id "defn-/numeric-value", :kind "defn-", :line 141, :end-line 143, :hash "-1365632356"} {:id "defn-/value-within-bounds?", :kind "defn-", :line 145, :end-line 151, :hash "-1066502942"} {:id "defn-/rename-endpoint-id", :kind "defn-", :line 153, :end-line 157, :hash "-1464052815"} {:id "defn-/rename-stock-endpoints", :kind "defn-", :line 159, :end-line 174, :hash "1076302708"} {:id "defn-/rename-blocked?", :kind "defn-", :line 176, :end-line 181, :hash "192871783"} {:id "defn/set-stock-name", :kind "defn", :line 183, :end-line 190, :hash "1651947829"} {:id "defn/set-stock-initial-value", :kind "defn", :line 192, :end-line 198, :hash "982226397"} {:id "defn-/set-stock-bound", :kind "defn-", :line 200, :end-line 206, :hash "2008645735"} {:id "defn/set-stock-min", :kind "defn", :line 208, :end-line 215, :hash "-405763036"} {:id "defn/set-stock-max", :kind "defn", :line 217, :end-line 222, :hash "218751066"} {:id "defn/clear-stock-max", :kind "defn", :line 224, :end-line 228, :hash "398894882"} {:id "defn/placement-disarmed?", :kind "defn", :line 230, :end-line 232, :hash "351569028"} {:id "defn/arm-stock-placement", :kind "defn", :line 234, :end-line 236, :hash "1928911371"} {:id "defn/place-stock", :kind "defn", :line 238, :end-line 249, :hash "1114170010"} {:id "defn/stocks", :kind "defn", :line 251, :end-line 253, :hash "-801020159"} {:id "defn-/num-from-name", :kind "defn-", :line 255, :end-line 257, :hash "1864201808"} {:id "defn-/fixture-item", :kind "defn-", :line 259, :end-line 265, :hash "-1104049080"} {:id "defn/fixture-stock", :kind "defn", :line 267, :end-line 270, :hash "1548349638"} {:id "defn-/endpoint-ref", :kind "defn-", :line 272, :end-line 274, :hash "916366387"} {:id "defn-/source-entry-by-name", :kind "defn-", :line 276, :end-line 278, :hash "1345064248"} {:id "defn-/sink-entry-by-name", :kind "defn-", :line 280, :end-line 282, :hash "-2143000880"} {:id "defn/source-exists?", :kind "defn", :line 284, :end-line 286, :hash "1146108815"} {:id "defn/sink-exists?", :kind "defn", :line 288, :end-line 290, :hash "-2079647085"} {:id "defn/source-position", :kind "defn", :line 292, :end-line 295, :hash "1966756"} {:id "defn/sink-position", :kind "defn", :line 297, :end-line 300, :hash "856729128"} {:id "form/52/declare", :kind "declare", :line 302, :end-line 302, :hash "-1373642061"} {:id "defn-/endpoint-position-resolvers", :kind "defn-", :line 304, :end-line 310, :hash "-1370097299"} {:id "defn/endpoint-position", :kind "defn", :line 312, :end-line 315, :hash "-1526732716"} {:id "def/endpoint-anchor-offsets", :kind "def", :line 317, :end-line 325, :hash "-1752866723"} {:id "defn/endpoint-anchor", :kind "defn", :line 327, :end-line 330, :hash "-1604573613"} {:id "defn/source-count", :kind "defn", :line 332, :end-line 334, :hash "913527496"} {:id "defn/sink-count", :kind "defn", :line 336, :end-line 338, :hash "1591552436"} {:id "defn/sources", :kind "defn", :line 340, :end-line 342, :hash "-973921734"} {:id "defn/sinks", :kind "defn", :line 344, :end-line 346, :hash "1773380131"} {:id "defn/fixture-source", :kind "defn", :line 348, :end-line 350, :hash "-1647492037"} {:id "defn/fixture-sink", :kind "defn", :line 352, :end-line 354, :hash "453893490"} {:id "defn/arm-source-placement", :kind "defn", :line 356, :end-line 358, :hash "-1296780729"} {:id "defn/arm-sink-placement", :kind "defn", :line 360, :end-line 362, :hash "1869647698"} {:id "defn-/place-cloud", :kind "defn-", :line 364, :end-line 374, :hash "752912211"} {:id "defn/place-source", :kind "defn", :line 376, :end-line 378, :hash "394591180"} {:id "defn/place-sink", :kind "defn", :line 380, :end-line 382, :hash "785372302"} {:id "defn/source-placement-disarmed?", :kind "defn", :line 384, :end-line 386, :hash "1135284315"} {:id "defn/sink-placement-disarmed?", :kind "defn", :line 388, :end-line 390, :hash "-1769136352"} {:id "defn-/flow-entry-by-name", :kind "defn-", :line 392, :end-line 394, :hash "-1730283296"} {:id "defn/flow-exists?", :kind "defn", :line 396, :end-line 398, :hash "905629296"} {:id "defn-/flow-attribute", :kind "defn-", :line 400, :end-line 403, :hash "1273430238"} {:id "defn/flow-from", :kind "defn", :line 405, :end-line 407, :hash "-751374872"} {:id "defn/flow-to", :kind "defn", :line 409, :end-line 411, :hash "1500420793"} {:id "defn/flow-endpoints", :kind "defn", :line 413, :end-line 418, :hash "1470373818"} {:id "defn/flow-rate", :kind "defn", :line 420, :end-line 422, :hash "-2102829222"} {:id "defn-/rename-endpoints-in-connectors", :kind "defn-", :line 424, :end-line 429, :hash "-1425675650"} {:id "defn/set-flow-name", :kind "defn", :line 431, :end-line 438, :hash "-79078843"} {:id "defn-/parseable-number?", :kind "defn-", :line 440, :end-line 446, :hash "928557303"} {:id "defn/set-flow-rate", :kind "defn", :line 448, :end-line 454, :hash "-112745979"} {:id "defn/flow-count", :kind "defn", :line 456, :end-line 458, :hash "-2009265396"} {:id "defn/flows", :kind "defn", :line 460, :end-line 462, :hash "608017970"} {:id "defn-/fixture-named-link", :kind "defn-", :line 464, :end-line 470, :hash "999899682"} {:id "defn-/fixture-endpoint-link", :kind "defn-", :line 472, :end-line 478, :hash "-1371478247"} {:id "defn/fixture-flow", :kind "defn", :line 480, :end-line 483, :hash "614964979"} {:id "defn-/valid-flow-pair?", :kind "defn-", :line 485, :end-line 491, :hash "-269751023"} {:id "defn-/create-collection-link!", :kind "defn-", :line 493, :end-line 501, :hash "-1154634412"} {:id "defn-/create-flow!", :kind "defn-", :line 503, :end-line 512, :hash "-1588176197"} {:id "defn-/arm-link-placement", :kind "defn-", :line 514, :end-line 518, :hash "1605520295"} {:id "defn/arm-flow-placement", :kind "defn", :line 520, :end-line 522, :hash "402481411"} {:id "defn/flow-placement-armed?", :kind "defn", :line 524, :end-line 526, :hash "-943631268"} {:id "defn-/draft-from-endpoint", :kind "defn-", :line 528, :end-line 537, :hash "1978981143"} {:id "defn/select-flow-source", :kind "defn", :line 539, :end-line 545, :hash "-323164585"} {:id "defn-/clear-flow-draft", :kind "defn-", :line 547, :end-line 549, :hash "-371891314"} {:id "form/95/declare", :kind "declare", :line 551, :end-line 551, :hash "1070947401"} {:id "defn-/endpoint-existence-checks", :kind "defn-", :line 553, :end-line 559, :hash "-1016995454"} {:id "defn-/endpoint-exists?", :kind "defn-", :line 561, :end-line 564, :hash "-1897963117"} {:id "defn-/try-connect-flow", :kind "defn-", :line 566, :end-line 574, :hash "822432826"} {:id "defn/connect-flow", :kind "defn", :line 576, :end-line 580, :hash "-817203079"} {:id "defn/flow-placement-disarmed?", :kind "defn", :line 582, :end-line 584, :hash "31272842"} {:id "defn-/converter-entry-by-name", :kind "defn-", :line 586, :end-line 588, :hash "-1383344081"} {:id "defn/converter-exists?", :kind "defn", :line 590, :end-line 592, :hash "300239202"} {:id "defn/converter-position", :kind "defn", :line 594, :end-line 597, :hash "843288393"} {:id "defn/converter-value", :kind "defn", :line 599, :end-line 602, :hash "13277447"} {:id "defn/converter-count", :kind "defn", :line 604, :end-line 606, :hash "267660665"} {:id "defn/converters", :kind "defn", :line 608, :end-line 610, :hash "-1686748773"} {:id "defn/fixture-converter", :kind "defn", :line 612, :end-line 615, :hash "-475935667"} {:id "defn/arm-converter-placement", :kind "defn", :line 617, :end-line 619, :hash "-2068224481"} {:id "defn/place-converter", :kind "defn", :line 621, :end-line 631, :hash "1356484642"} {:id "defn/converter-placement-disarmed?", :kind "defn", :line 633, :end-line 635, :hash "887795504"} {:id "defn/flow-midpoint", :kind "defn", :line 637, :end-line 644, :hash "1396828306"} {:id "def/clickable-kinds-by-mode", :kind "def", :line 646, :end-line 648, :hash "733363462"} {:id "defn/endpoint-clickable?", :kind "defn", :line 650, :end-line 652, :hash "1518566357"} {:id "defn-/connector-entry-by-name", :kind "defn-", :line 654, :end-line 656, :hash "971711688"} {:id "defn/connector-exists?", :kind "defn", :line 658, :end-line 660, :hash "314481541"} {:id "defn-/connector-attribute", :kind "defn-", :line 662, :end-line 665, :hash "709631432"} {:id "defn/connector-from", :kind "defn", :line 667, :end-line 669, :hash "524525571"} {:id "defn/connector-to", :kind "defn", :line 671, :end-line 673, :hash "799906750"} {:id "defn/connector-formula", :kind "defn", :line 675, :end-line 677, :hash "-1029787925"} {:id "defn/set-converter-name", :kind "defn", :line 679, :end-line 686, :hash "-847157151"} {:id "defn-/converter-to-flow-connector-id", :kind "defn-", :line 688, :end-line 695, :hash "-195957966"} {:id "defn/set-converter-formula", :kind "defn", :line 697, :end-line 703, :hash "-823051875"} {:id "defn/fixture-connector", :kind "defn", :line 705, :end-line 708, :hash "248601847"} {:id "defn/connector-count", :kind "defn", :line 710, :end-line 712, :hash "-1826970045"} {:id "defn/connectors", :kind "defn", :line 714, :end-line 716, :hash "-1048990130"} {:id "defn-/valid-connector-pair?", :kind "defn-", :line 718, :end-line 723, :hash "1385416327"} {:id "defn-/create-connector!", :kind "defn-", :line 725, :end-line 734, :hash "364741780"} {:id "defn/arm-connector-placement", :kind "defn", :line 736, :end-line 738, :hash "-1475706923"} {:id "defn/connector-placement-armed?", :kind "defn", :line 740, :end-line 742, :hash "-675985363"} {:id "defn/select-connector-origin", :kind "defn", :line 744, :end-line 752, :hash "1914302439"} {:id "defn-/try-connect-connector", :kind "defn-", :line 754, :end-line 766, :hash "1616656565"} {:id "defn/connect-connector", :kind "defn", :line 768, :end-line 773, :hash "1621379443"} {:id "defn/connector-placement-disarmed?", :kind "defn", :line 775, :end-line 777, :hash "-1401416734"}]}
;; clj-mutate-manifest-end
