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
   :selection #{}
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

(defn move-stock
  [diagram name x y]
  (if-let [[id stock] (stock-entry-by-name diagram name)]
    (assoc-in diagram [:stocks id] (assoc stock :x x :y y))
    diagram))

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

(defn apply-stock-edit
  [diagram stock-name {:keys [name initial-value min-value max-value]}]
  (let [after-name (if (= name stock-name)
                     diagram
                     (set-stock-name diagram stock-name name))]
    (if (and (not= name stock-name) (= after-name diagram))
      diagram
      (let [target (if (= after-name diagram) stock-name name)
            prior-min (stock-min-value after-name target)
            after-min (set-stock-min after-name target min-value)]
        (if (and (not= (str min-value) (str prior-min))
                 (= (stock-min-value after-min target) prior-min))
          after-name
          (let [prior-max (stock-max-value after-min target)
                after-max (if (seq (str max-value))
                            (set-stock-max after-min target max-value)
                            (clear-stock-max after-min target))]
            (if (and (seq (str max-value))
                     (not= (str max-value) (str prior-max))
                     (= (stock-max-value after-max target) prior-max))
              after-min
              (let [prior-init (stock-initial-value after-max target)
                    after-init (set-stock-initial-value after-max target initial-value)]
                (if (and (not= (str initial-value) (str prior-init))
                         (= (stock-initial-value after-init target) prior-init))
                  after-max
                  after-init)))))))))

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

(defn apply-flow-edit
  [diagram flow-name {:keys [name rate]}]
  (let [after-name (if (= name flow-name)
                     diagram
                     (set-flow-name diagram flow-name name))]
    (if (and (not= name flow-name) (= after-name diagram))
      diagram
      (let [target (if (= after-name diagram) flow-name name)]
        (if (parseable-number? rate)
          (set-flow-rate after-name target rate)
          after-name)))))

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

(defn move-converter
  [diagram name x y]
  (if-let [[id converter] (converter-entry-by-name diagram name)]
    (assoc-in diagram [:converters id] (assoc converter :x x :y y))
    diagram))

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

(defn- fixture-connector-endpoints
  [diagram connector-name from-kind from-name to-kind to-name]
  (let [num (num-from-name "Connector" connector-name)
        id (keyword (str "connector-" num))]
    (-> diagram
        (assoc-in [:connectors id]
                  {:name connector-name
                   :from (endpoint-ref from-kind from-name)
                   :to (endpoint-ref to-kind to-name)
                   :formula ""})
        (update :next-connector-num #(max % (inc num))))))

(defn converter-connector-formula
  [diagram converter-name]
  (when-let [id (converter-to-flow-connector-id diagram converter-name)]
    (get-in diagram [:connectors id :formula] "")))

(defn apply-converter-edit
  [diagram converter-name {:keys [name formula]}]
  (let [after-name (if (= name converter-name)
                     diagram
                     (set-converter-name diagram converter-name name))]
    (if (and (not= name converter-name) (= after-name diagram))
      diagram
      (let [target (if (= after-name diagram) converter-name name)
            prior-formula (or (converter-connector-formula after-name target) "")]
        (if (= (str formula) (str prior-formula))
          after-name
          (set-converter-formula after-name target formula))))))

(defn fixture-connector
  [diagram connector-name from-converter to-flow]
  (fixture-connector-endpoints diagram connector-name
                               :converter from-converter :flow to-flow))

(defn fixture-stock-connector
  [diagram connector-name from-stock to-converter]
  (fixture-connector-endpoints diagram connector-name
                               :stock from-stock :converter to-converter))

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

(defn- object-ref
  [kind name]
  {:kind kind :id name})

(defn selected?
  [diagram kind name]
  (contains? (:selection diagram #{}) (object-ref kind name)))

(defn selection-count
  [diagram]
  (count (:selection diagram #{})))

(defn nothing-selected?
  [diagram]
  (zero? (selection-count diagram)))

(defn- normalize-rect
  [x1 y1 x2 y2]
  [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)])

(defn- rects-intersect?
  [[mx1 my1 mx2 my2] [ox1 oy1 ox2 oy2]]
  (and (< mx1 ox2) (> mx2 ox1) (< my1 oy2) (> my2 oy1)))

(defn- stock-bounds
  [{:keys [x y]}]
  [x y (+ x 80) (+ y 50)])

(defn- converter-bounds
  [{:keys [x y]}]
  [x y (+ x 50) (+ y 50)])

(defn- cloud-bounds
  [{:keys [x y]}]
  [x y (+ x 80) (+ y 50)])

(defn- link-bounds
  [diagram from to padding]
  (when-let [from-pos (endpoint-position diagram from)]
    (when-let [to-pos (endpoint-position diagram to)]
      (let [[fx fy] (endpoint-anchor from-pos (:kind from) :right)
            [tx ty] (endpoint-anchor to-pos (:kind to) :left)]
        [(min fx tx) (- (min fy ty) padding)
         (max fx tx) (+ (max fy ty) padding)]))))

(defn- selectable-objects-with-bounds
  [diagram]
  (concat
   (for [[_ {:keys [name] :as stock}] (:stocks diagram)]
     [(object-ref :stock name) (stock-bounds stock)])
   (for [[_ {:keys [name] :as converter}] (:converters diagram)]
     [(object-ref :converter name) (converter-bounds converter)])
   (for [[_ {:keys [name from to]}] (:flows diagram)]
     (when-let [bounds (link-bounds diagram from to 20)]
       [(object-ref :flow name) bounds]))
   (for [[_ {:keys [name from to]}] (:connectors diagram)]
     (when-let [bounds (link-bounds diagram from to 15)]
       [(object-ref :connector name) bounds]))
   (for [[_ {:keys [name] :as source}] (:sources diagram)]
     [(object-ref :source name) (cloud-bounds source)])
   (for [[_ {:keys [name] :as sink}] (:sinks diagram)]
     [(object-ref :sink name) (cloud-bounds sink)])))

(defn- update-selection-when-idle
  [diagram update-fn]
  (if (placement-disarmed? diagram)
    (update-fn diagram)
    diagram))

(defn click-select
  [diagram kind name]
  (update-selection-when-idle
   diagram
   (fn [diagram]
     (if (endpoint-exists? diagram kind name)
       (let [ref (object-ref kind name)
             selection (:selection diagram #{})]
         (assoc diagram :selection
                (if (contains? selection ref)
                  (disj selection ref)
                  #{ref})))
       diagram))))

(defn shift-click-select
  [diagram kind name]
  (update-selection-when-idle
   diagram
   (fn [diagram]
     (if (endpoint-exists? diagram kind name)
       (let [ref (object-ref kind name)
             selection (:selection diagram #{})]
         (assoc diagram :selection
                (if (contains? selection ref)
                  (disj selection ref)
                  (conj selection ref))))
       diagram))))

(defn marquee-select
  [diagram x1 y1 x2 y2]
  (update-selection-when-idle
   diagram
   (fn [diagram]
     (let [marquee (normalize-rect x1 y1 x2 y2)
           selected (into #{}
                          (keep (fn [[ref bounds]]
                                  (when (rects-intersect? marquee bounds)
                                    ref))
                                (selectable-objects-with-bounds diagram)))]
       (assoc diagram :selection selected)))))

(defn clear-selection
  [diagram]
  (assoc diagram :selection #{}))

(defn- links-referencing-ref
  [diagram collection ref]
  (into #{}
        (keep (fn [[_ {:keys [name from to]}]]
                (when (or (= from ref) (= to ref))
                  (object-ref (if (= collection :flows) :flow :connector) name)))
              (get diagram collection))))

(defn- cascade-additions-for-ref
  [diagram ref]
  (case (:kind ref)
    :stock (into (links-referencing-ref diagram :flows ref)
                 (links-referencing-ref diagram :connectors ref))
    (:source :sink) (links-referencing-ref diagram :flows ref)
    :flow (links-referencing-ref diagram :connectors ref)
    :converter (links-referencing-ref diagram :connectors ref)
    :connector #{}
    #{}))

(defn- expand-delete-set
  [diagram refs]
  (loop [current refs]
    (let [additions (into #{} (mapcat #(cascade-additions-for-ref diagram %) current))
          expanded (into current additions)]
      (if (= expanded current)
        expanded
        (recur expanded)))))

(defn- remove-object-ref
  [diagram {:keys [kind id]}]
  (case kind
    :stock (if-let [[object-id _] (stock-entry-by-name diagram id)]
             (update diagram :stocks dissoc object-id)
             diagram)
    :flow (if-let [[object-id _] (flow-entry-by-name diagram id)]
            (update diagram :flows dissoc object-id)
            diagram)
    :converter (if-let [[object-id _] (converter-entry-by-name diagram id)]
                 (update diagram :converters dissoc object-id)
                 diagram)
    :connector (if-let [[object-id _] (connector-entry-by-name diagram id)]
                 (update diagram :connectors dissoc object-id)
                 diagram)
    :source (if-let [[object-id _] (source-entry-by-name diagram id)]
              (update diagram :sources dissoc object-id)
              diagram)
    :sink (if-let [[object-id _] (sink-entry-by-name diagram id)]
             (update diagram :sinks dissoc object-id)
             diagram)
    diagram))

(defn delete-selection
  [diagram]
  (if (and (placement-disarmed? diagram)
           (seq (:selection diagram #{})))
    (let [to-delete (expand-delete-set diagram (:selection diagram))]
      (reduce remove-object-ref
              (assoc diagram :selection #{})
              to-delete))
    diagram))
