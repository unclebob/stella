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

(def stock-icon-width 80)
(def stock-icon-height 50)

(defn stock-at-canvas-point
  "Returns the stock name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (some (fn [{:keys [name x y]}]
          (when (and (<= x cx (+ x stock-icon-width))
                     (<= y cy (+ y stock-icon-height)))
            name))
        (vals (:stocks diagram))))

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

(defn- move-cloud
  [diagram collection entry-by-name name x y]
  (if-let [[id cloud] (entry-by-name diagram name)]
    (assoc-in diagram [collection id] (assoc cloud :x x :y y))
    diagram))

(defn move-source
  [diagram name x y]
  (move-cloud diagram :sources source-entry-by-name name x y))

(defn move-sink
  [diagram name x y]
  (move-cloud diagram :sinks sink-entry-by-name name x y))

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

(defn- cloud-at-canvas-point-in
  [clouds cx cy]
  (some (fn [{:keys [name x y]}]
          (when (and (<= x cx (+ x 80))
                     (<= y cy (+ y 50)))
            name))
        clouds))

(defn source-at-canvas-point
  "Returns the source name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (cloud-at-canvas-point-in (vals (:sources diagram)) cx cy))

(defn sink-at-canvas-point
  "Returns the sink name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (cloud-at-canvas-point-in (vals (:sinks diagram)) cx cy))

(defn cloud-at-canvas-point
  "Returns {:kind k :name n} for a source/sink at canvas-local point [x y], or nil."
  [diagram cx cy]
  (or (some->> (source-at-canvas-point diagram cx cy)
               (hash-map :kind :source :name))
      (some->> (sink-at-canvas-point diagram cx cy)
               (hash-map :kind :sink :name))))

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

(declare converter-exists? connector-exists?)

(defn- endpoint-existence-checks
  []
  {:stock stock-exists?
   :sink sink-exists?
   :converter converter-exists?
   :flow flow-exists?
   :connector connector-exists?
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

(def converter-icon-size 50)

(defn converter-at-canvas-point
  "Returns the converter name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (some (fn [{:keys [name x y]}]
          (when (and (<= x cx (+ x converter-icon-size))
                     (<= y cy (+ y converter-icon-size)))
            name))
        (vals (:converters diagram))))

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

(defn- point-in-rect?
  [[x1 y1 x2 y2] x y]
  (and (<= x1 x x2)
       (<= y1 y y2)))

(defn- circle-intersects-rect?
  [[x1 y1 x2 y2] cx cy radius]
  (let [nearest-x (-> cx (max x1) (min x2))
        nearest-y (-> cy (max y1) (min y2))]
    (<= (+ (* (- cx nearest-x) (- cx nearest-x))
           (* (- cy nearest-y) (- cy nearest-y)))
        (* radius radius))))

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

(defn- point-segment-distance
  [px py ax ay bx by]
  (let [dx (- bx ax)
        dy (- by ay)
        length-squared (+ (* dx dx) (* dy dy))]
    (if (zero? length-squared)
      (Math/sqrt (+ (* (- px ax) (- px ax))
                    (* (- py ay) (- py ay))))
      (let [t (-> (/ (+ (* (- px ax) dx)
                        (* (- py ay) dy))
                     length-squared)
                  (max 0.0)
                  (min 1.0))
            cx (+ ax (* t dx))
            cy (+ ay (* t dy))]
        (Math/sqrt (+ (* (- px cx) (- px cx))
                      (* (- py cy) (- py cy))))))))

(def ^:private canvas-center [2000.0 2000.0])

(defn- connector-default-control-offset
  [start-x start-y end-x end-y]
  (let [mid-x (/ (+ start-x end-x) 2.0)
        mid-y (/ (+ start-y end-y) 2.0)
        chord (Math/sqrt (+ (Math/pow (- end-x start-x) 2)
                            (Math/pow (- end-y start-y) 2)))
        away-x (- mid-x (first canvas-center))
        away-y (- mid-y (second canvas-center))
        away-length (Math/sqrt (+ (* away-x away-x) (* away-y away-y)))
        offset (-> (* chord 0.2) (max 20.0) (min 80.0))
        [ux uy] (if (zero? away-length)
                  [0.0 -1.0]
                  [(/ away-x away-length) (/ away-y away-length)])]
    [(* ux offset)
     (* uy offset)]))

(defn- connector-control-point
  [start-x start-y end-x end-y control-offset]
  (let [[offset-x offset-y] (or control-offset
                                (connector-default-control-offset start-x start-y end-x end-y))]
    [(+ (/ (+ start-x end-x) 2.0) offset-x)
     (+ (/ (+ start-y end-y) 2.0) offset-y)]))

(defn- quadratic-point
  [start-x start-y control-x control-y end-x end-y t]
  (let [one-minus-t (- 1.0 t)
        start-scale (* one-minus-t one-minus-t)
        control-scale (* 2.0 one-minus-t t)
        end-scale (* t t)]
    [(+ (* start-scale start-x)
        (* control-scale control-x)
        (* end-scale end-x))
     (+ (* start-scale start-y)
        (* control-scale control-y)
        (* end-scale end-y))]))

(defn- point-near-quadratic?
  [padding x y start-x start-y control-x control-y end-x end-y]
  (let [steps 24]
    (loop [index 1
           [prior-x prior-y] [start-x start-y]]
      (if (> index steps)
        false
        (let [t (/ index steps)
              [next-x next-y] (quadratic-point start-x start-y control-x control-y end-x end-y t)]
          (if (<= (point-segment-distance x y prior-x prior-y next-x next-y) padding)
            true
            (recur (inc index) [next-x next-y])))))))

(defn- connector-curve-midpoint
  [start-x start-y control-x control-y end-x end-y]
  (quadratic-point start-x start-y control-x control-y end-x end-y 0.5))

(defn- endpoint-center
  [[x y] kind]
  (case kind
    (:stock :source :sink) [(+ x 40.0) (+ y 25.0)]
    :converter [(+ x 25.0) (+ y 25.0)]
    :flow [x y]
    [x y]))

(defn- ellipse-boundary-point
  [[cx cy] [tx ty] radius-x radius-y]
  (let [dx (- tx cx)
        dy (- ty cy)
        scale (Math/sqrt (+ (/ (* dx dx) (* radius-x radius-x))
                            (/ (* dy dy) (* radius-y radius-y))))]
    (if (zero? scale)
      [cx cy]
      [(+ cx (/ dx scale)) (+ cy (/ dy scale))])))

(defn- rectangle-boundary-point
  [[cx cy] [tx ty] half-width half-height]
  (let [dx (- tx cx)
        dy (- ty cy)
        scale (max (if (zero? half-width) 0.0 (/ (Math/abs dx) half-width))
                   (if (zero? half-height) 0.0 (/ (Math/abs dy) half-height)))]
    (if (zero? scale)
      [cx cy]
      [(+ cx (/ dx scale)) (+ cy (/ dy scale))])))

(defn- endpoint-boundary-point
  [pos kind target]
  (let [center (endpoint-center pos kind)]
    (case kind
      :stock (rectangle-boundary-point center target 40.0 25.0)
      (:source :sink) (ellipse-boundary-point center target 40.0 25.0)
      :converter (ellipse-boundary-point center target 25.0 25.0)
      :flow (ellipse-boundary-point center target 5.0 5.0)
      center)))

(defn- visible-link-endpoints
  [from-pos from-kind to-pos to-kind]
  (let [from-center (endpoint-center from-pos from-kind)
        to-center (endpoint-center to-pos to-kind)]
    [(endpoint-boundary-point from-pos from-kind to-center)
     (endpoint-boundary-point to-pos to-kind from-center)]))

(defn- point-near-link?
  [diagram from to padding x y]
  (when-let [from-pos (endpoint-position diagram from)]
    (when-let [to-pos (endpoint-position diagram to)]
      (let [[[fx fy] [tx ty]] (visible-link-endpoints from-pos (:kind from)
                                                      to-pos (:kind to))]
        (<= (point-segment-distance x y fx fy tx ty) padding)))))

(defn- connector-curve-points
  [diagram {:keys [from to control-offset]}]
  (when-let [from-pos (endpoint-position diagram from)]
    (when-let [to-pos (endpoint-position diagram to)]
      (let [[fx fy] (endpoint-center from-pos (:kind from))
            [tx ty] (endpoint-center to-pos (:kind to))
            [cx cy] (connector-control-point fx fy tx ty control-offset)]
        {:start [fx fy]
         :control [cx cy]
         :end [tx ty]
         :midpoint (connector-curve-midpoint fx fy cx cy tx ty)}))))

(defn- point-near-connector?
  [diagram connector padding x y]
  (when-let [{[fx fy] :start
              [cx cy] :control
              [tx ty] :end} (connector-curve-points diagram connector)]
    (point-near-quadratic? padding x y fx fy cx cy tx ty)))

(defn- first-object-at
  [objects]
  (some identity objects))

(defn object-at-canvas-point
  [diagram x y]
  (let [click-radius 5.0]
    (first-object-at
     (concat
      (for [[_ {:keys [name] :as stock}] (:stocks diagram)
            :when (circle-intersects-rect? (stock-bounds stock) x y click-radius)]
        (object-ref :stock name))
      (for [[_ {:keys [name] :as source}] (:sources diagram)
            :when (circle-intersects-rect? (cloud-bounds source) x y click-radius)]
        (object-ref :source name))
      (for [[_ {:keys [name] :as sink}] (:sinks diagram)
            :when (circle-intersects-rect? (cloud-bounds sink) x y click-radius)]
        (object-ref :sink name))
      (for [[_ {:keys [name from to]}] (:flows diagram)
            :when (point-near-link? diagram from to (+ 5.0 click-radius) x y)]
        (object-ref :flow name))
      (for [[_ {:keys [name] :as converter}] (:converters diagram)
            :when (circle-intersects-rect? (converter-bounds converter) x y click-radius)]
        (object-ref :converter name))
      (for [[_ {:keys [name] :as connector}] (:connectors diagram)
            :when (point-near-connector? diagram connector (+ 10.0 click-radius) x y)]
        (object-ref :connector name))))))

(defn- connector-bounds
  [diagram connector padding]
  (when-let [{[fx fy] :start
              [cx cy] :control
              [tx ty] :end} (connector-curve-points diagram connector)]
    [(- (min fx cx tx) padding)
     (- (min fy cy ty) padding)
     (+ (max fx cx tx) padding)
     (+ (max fy cy ty) padding)]))

(defn connector-handle-position
  [diagram name]
  (when-let [[_ connector] (connector-entry-by-name diagram name)]
    (:midpoint (connector-curve-points diagram connector))))

(defn move-connector-handle
  [diagram name x y]
  (if-let [[connector-id connector] (connector-entry-by-name diagram name)]
    (if-let [{[start-x start-y] :start
              [end-x end-y] :end} (connector-curve-points diagram connector)]
      (let [chord-midpoint-x (/ (+ start-x end-x) 2.0)
            chord-midpoint-y (/ (+ start-y end-y) 2.0)
            offset-x (* 2.0 (- x chord-midpoint-x))
            offset-y (* 2.0 (- y chord-midpoint-y))]
        (assoc-in diagram [:connectors connector-id :control-offset] [offset-x offset-y]))
      diagram)
    diagram))

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
   (for [[_ {:keys [name] :as connector}] (:connectors diagram)]
     (when-let [bounds (connector-bounds diagram connector 15)]
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
         (if (= selection #{ref})
           diagram
           (assoc diagram :selection #{ref})))
       diagram))))

(defn click-select-at
  [diagram x y]
  (if-let [{:keys [kind id]} (object-at-canvas-point diagram x y)]
    (click-select diagram kind id)
    diagram))

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

(defn shift-click-select-at
  [diagram x y]
  (if-let [{:keys [kind id]} (object-at-canvas-point diagram x y)]
    (shift-click-select diagram kind id)
    diagram))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:26:55.815016-05:00", :module-hash "-674940889", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-289228109"} {:id "defn-/menu-item", :kind "defn-", :line 3, :end-line 4, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 6, :end-line 7, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 9, :end-line 16, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 18, :end-line 25, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 27, :end-line 31, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 33, :end-line 35, :hash "229400292"} {:id "defn/default-diagram", :kind "defn", :line 37, :end-line 53, :hash "-1698396050"} {:id "defn/default-shell", :kind "defn", :line 55, :end-line 64, :hash "-1757765176"} {:id "defn/top-level-menus", :kind "defn", :line 66, :end-line 68, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 70, :end-line 72, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 74, :end-line 76, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 78, :end-line 82, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 84, :end-line 86, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 88, :end-line 90, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 92, :end-line 94, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 96, :end-line 98, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 100, :end-line 102, :hash "-256542367"} {:id "defn/stock-count", :kind "defn", :line 104, :end-line 106, :hash "-1679213761"} {:id "defn-/stock-entry-by-name", :kind "defn-", :line 108, :end-line 110, :hash "407514139"} {:id "defn/stock-exists?", :kind "defn", :line 112, :end-line 114, :hash "2041739431"} {:id "defn/stock-position", :kind "defn", :line 116, :end-line 119, :hash "69915530"} {:id "defn-/stock-field", :kind "defn-", :line 121, :end-line 124, :hash "-2043085379"} {:id "defn/move-stock", :kind "defn", :line 126, :end-line 130, :hash "990066087"} {:id "defn/stock-initial-value", :kind "defn", :line 132, :end-line 134, :hash "-567431284"} {:id "defn/stock-min-value", :kind "defn", :line 136, :end-line 138, :hash "1340209325"} {:id "defn/stock-max-value", :kind "defn", :line 140, :end-line 142, :hash "684410828"} {:id "defn/stock-named?", :kind "defn", :line 144, :end-line 146, :hash "-166675305"} {:id "defn-/numeric-value", :kind "defn-", :line 148, :end-line 150, :hash "-1365632356"} {:id "defn-/value-within-bounds?", :kind "defn-", :line 152, :end-line 158, :hash "-1066502942"} {:id "defn-/rename-endpoint-id", :kind "defn-", :line 160, :end-line 164, :hash "-1464052815"} {:id "defn-/rename-stock-endpoints", :kind "defn-", :line 166, :end-line 181, :hash "1076302708"} {:id "defn-/rename-blocked?", :kind "defn-", :line 183, :end-line 188, :hash "192871783"} {:id "defn/set-stock-name", :kind "defn", :line 190, :end-line 197, :hash "1651947829"} {:id "defn/set-stock-initial-value", :kind "defn", :line 199, :end-line 205, :hash "982226397"} {:id "defn-/set-stock-bound", :kind "defn-", :line 207, :end-line 213, :hash "2008645735"} {:id "defn/set-stock-min", :kind "defn", :line 215, :end-line 222, :hash "-405763036"} {:id "defn/set-stock-max", :kind "defn", :line 224, :end-line 229, :hash "218751066"} {:id "defn/clear-stock-max", :kind "defn", :line 231, :end-line 235, :hash "398894882"} {:id "defn/placement-disarmed?", :kind "defn", :line 237, :end-line 239, :hash "351569028"} {:id "defn/arm-stock-placement", :kind "defn", :line 241, :end-line 243, :hash "1928911371"} {:id "defn/place-stock", :kind "defn", :line 245, :end-line 256, :hash "1114170010"} {:id "defn/stocks", :kind "defn", :line 258, :end-line 260, :hash "-801020159"} {:id "defn-/num-from-name", :kind "defn-", :line 262, :end-line 264, :hash "1864201808"} {:id "defn-/fixture-item", :kind "defn-", :line 266, :end-line 272, :hash "-1104049080"} {:id "defn/fixture-stock", :kind "defn", :line 274, :end-line 277, :hash "1548349638"} {:id "defn-/endpoint-ref", :kind "defn-", :line 279, :end-line 281, :hash "916366387"} {:id "defn-/source-entry-by-name", :kind "defn-", :line 283, :end-line 285, :hash "1345064248"} {:id "defn-/sink-entry-by-name", :kind "defn-", :line 287, :end-line 289, :hash "-2143000880"} {:id "defn/source-exists?", :kind "defn", :line 291, :end-line 293, :hash "1146108815"} {:id "defn/sink-exists?", :kind "defn", :line 295, :end-line 297, :hash "-2079647085"} {:id "defn/source-position", :kind "defn", :line 299, :end-line 302, :hash "1966756"} {:id "defn/sink-position", :kind "defn", :line 304, :end-line 307, :hash "856729128"} {:id "form/53/declare", :kind "declare", :line 309, :end-line 309, :hash "-1373642061"} {:id "defn-/endpoint-position-resolvers", :kind "defn-", :line 311, :end-line 317, :hash "-1370097299"} {:id "defn/endpoint-position", :kind "defn", :line 319, :end-line 322, :hash "-1526732716"} {:id "def/endpoint-anchor-offsets", :kind "def", :line 324, :end-line 332, :hash "-1752866723"} {:id "defn/endpoint-anchor", :kind "defn", :line 334, :end-line 337, :hash "-1604573613"} {:id "defn/source-count", :kind "defn", :line 339, :end-line 341, :hash "913527496"} {:id "defn/sink-count", :kind "defn", :line 343, :end-line 345, :hash "1591552436"} {:id "defn/sources", :kind "defn", :line 347, :end-line 349, :hash "-973921734"} {:id "defn/sinks", :kind "defn", :line 351, :end-line 353, :hash "1773380131"} {:id "defn/fixture-source", :kind "defn", :line 355, :end-line 357, :hash "-1647492037"} {:id "defn/fixture-sink", :kind "defn", :line 359, :end-line 361, :hash "453893490"} {:id "defn/arm-source-placement", :kind "defn", :line 363, :end-line 365, :hash "-1296780729"} {:id "defn/arm-sink-placement", :kind "defn", :line 367, :end-line 369, :hash "1869647698"} {:id "defn-/place-cloud", :kind "defn-", :line 371, :end-line 381, :hash "752912211"} {:id "defn/place-source", :kind "defn", :line 383, :end-line 385, :hash "394591180"} {:id "defn/place-sink", :kind "defn", :line 387, :end-line 389, :hash "785372302"} {:id "defn/source-placement-disarmed?", :kind "defn", :line 391, :end-line 393, :hash "1135284315"} {:id "defn/sink-placement-disarmed?", :kind "defn", :line 395, :end-line 397, :hash "-1769136352"} {:id "defn-/flow-entry-by-name", :kind "defn-", :line 399, :end-line 401, :hash "-1730283296"} {:id "defn/flow-exists?", :kind "defn", :line 403, :end-line 405, :hash "905629296"} {:id "defn-/flow-attribute", :kind "defn-", :line 407, :end-line 410, :hash "1273430238"} {:id "defn/flow-from", :kind "defn", :line 412, :end-line 414, :hash "-751374872"} {:id "defn/flow-to", :kind "defn", :line 416, :end-line 418, :hash "1500420793"} {:id "defn/flow-endpoints", :kind "defn", :line 420, :end-line 425, :hash "1470373818"} {:id "defn/flow-rate", :kind "defn", :line 427, :end-line 429, :hash "-2102829222"} {:id "defn-/rename-endpoints-in-connectors", :kind "defn-", :line 431, :end-line 436, :hash "-1425675650"} {:id "defn/set-flow-name", :kind "defn", :line 438, :end-line 445, :hash "-79078843"} {:id "defn-/parseable-number?", :kind "defn-", :line 447, :end-line 453, :hash "928557303"} {:id "defn/set-flow-rate", :kind "defn", :line 455, :end-line 461, :hash "-112745979"} {:id "defn/flow-count", :kind "defn", :line 463, :end-line 465, :hash "-2009265396"} {:id "defn/flows", :kind "defn", :line 467, :end-line 469, :hash "608017970"} {:id "defn-/fixture-named-link", :kind "defn-", :line 471, :end-line 477, :hash "999899682"} {:id "defn-/fixture-endpoint-link", :kind "defn-", :line 479, :end-line 485, :hash "-1371478247"} {:id "defn/fixture-flow", :kind "defn", :line 487, :end-line 490, :hash "614964979"} {:id "defn-/valid-flow-pair?", :kind "defn-", :line 492, :end-line 498, :hash "-269751023"} {:id "defn-/create-collection-link!", :kind "defn-", :line 500, :end-line 508, :hash "-1154634412"} {:id "defn-/create-flow!", :kind "defn-", :line 510, :end-line 519, :hash "-1588176197"} {:id "defn-/arm-link-placement", :kind "defn-", :line 521, :end-line 525, :hash "1605520295"} {:id "defn/arm-flow-placement", :kind "defn", :line 527, :end-line 529, :hash "402481411"} {:id "defn/flow-placement-armed?", :kind "defn", :line 531, :end-line 533, :hash "-943631268"} {:id "defn-/draft-from-endpoint", :kind "defn-", :line 535, :end-line 544, :hash "1978981143"} {:id "defn/select-flow-source", :kind "defn", :line 546, :end-line 552, :hash "-323164585"} {:id "defn-/clear-flow-draft", :kind "defn-", :line 554, :end-line 556, :hash "-371891314"} {:id "form/96/declare", :kind "declare", :line 558, :end-line 558, :hash "1070947401"} {:id "defn-/endpoint-existence-checks", :kind "defn-", :line 560, :end-line 566, :hash "-1016995454"} {:id "defn-/endpoint-exists?", :kind "defn-", :line 568, :end-line 571, :hash "-1897963117"} {:id "defn-/try-connect-flow", :kind "defn-", :line 573, :end-line 581, :hash "822432826"} {:id "defn/connect-flow", :kind "defn", :line 583, :end-line 587, :hash "-817203079"} {:id "defn/flow-placement-disarmed?", :kind "defn", :line 589, :end-line 591, :hash "31272842"} {:id "defn-/converter-entry-by-name", :kind "defn-", :line 593, :end-line 595, :hash "-1383344081"} {:id "defn/converter-exists?", :kind "defn", :line 597, :end-line 599, :hash "300239202"} {:id "defn/converter-position", :kind "defn", :line 601, :end-line 604, :hash "843288393"} {:id "defn/move-converter", :kind "defn", :line 606, :end-line 610, :hash "-1179681609"} {:id "defn/converter-value", :kind "defn", :line 612, :end-line 615, :hash "13277447"} {:id "defn/converter-count", :kind "defn", :line 617, :end-line 619, :hash "267660665"} {:id "defn/converters", :kind "defn", :line 621, :end-line 623, :hash "-1686748773"} {:id "defn/fixture-converter", :kind "defn", :line 625, :end-line 628, :hash "-475935667"} {:id "defn/arm-converter-placement", :kind "defn", :line 630, :end-line 632, :hash "-2068224481"} {:id "defn/place-converter", :kind "defn", :line 634, :end-line 644, :hash "1356484642"} {:id "defn/converter-placement-disarmed?", :kind "defn", :line 646, :end-line 648, :hash "887795504"} {:id "defn/flow-midpoint", :kind "defn", :line 650, :end-line 657, :hash "1396828306"} {:id "def/clickable-kinds-by-mode", :kind "def", :line 659, :end-line 661, :hash "733363462"} {:id "defn/endpoint-clickable?", :kind "defn", :line 663, :end-line 665, :hash "1518566357"} {:id "defn-/connector-entry-by-name", :kind "defn-", :line 667, :end-line 669, :hash "971711688"} {:id "defn/connector-exists?", :kind "defn", :line 671, :end-line 673, :hash "314481541"} {:id "defn-/connector-attribute", :kind "defn-", :line 675, :end-line 678, :hash "709631432"} {:id "defn/connector-from", :kind "defn", :line 680, :end-line 682, :hash "524525571"} {:id "defn/connector-to", :kind "defn", :line 684, :end-line 686, :hash "799906750"} {:id "defn/connector-formula", :kind "defn", :line 688, :end-line 690, :hash "-1029787925"} {:id "defn/set-converter-name", :kind "defn", :line 692, :end-line 699, :hash "-847157151"} {:id "defn-/converter-to-flow-connector-id", :kind "defn-", :line 701, :end-line 708, :hash "-195957966"} {:id "defn/set-converter-formula", :kind "defn", :line 710, :end-line 716, :hash "-823051875"} {:id "defn-/fixture-connector-endpoints", :kind "defn-", :line 718, :end-line 728, :hash "-179157893"} {:id "defn/fixture-connector", :kind "defn", :line 730, :end-line 733, :hash "-1936572057"} {:id "defn/fixture-stock-connector", :kind "defn", :line 735, :end-line 738, :hash "1964218954"} {:id "defn/connector-count", :kind "defn", :line 740, :end-line 742, :hash "-1826970045"} {:id "defn/connectors", :kind "defn", :line 744, :end-line 746, :hash "-1048990130"} {:id "defn-/valid-connector-pair?", :kind "defn-", :line 748, :end-line 753, :hash "1385416327"} {:id "defn-/create-connector!", :kind "defn-", :line 755, :end-line 764, :hash "364741780"} {:id "defn/arm-connector-placement", :kind "defn", :line 766, :end-line 768, :hash "-1475706923"} {:id "defn/connector-placement-armed?", :kind "defn", :line 770, :end-line 772, :hash "-675985363"} {:id "defn/select-connector-origin", :kind "defn", :line 774, :end-line 782, :hash "1914302439"} {:id "defn-/try-connect-connector", :kind "defn-", :line 784, :end-line 796, :hash "1616656565"} {:id "defn/connect-connector", :kind "defn", :line 798, :end-line 803, :hash "1621379443"} {:id "defn/connector-placement-disarmed?", :kind "defn", :line 805, :end-line 807, :hash "-1401416734"} {:id "defn-/object-ref", :kind "defn-", :line 809, :end-line 811, :hash "256234122"} {:id "defn/selected?", :kind "defn", :line 813, :end-line 815, :hash "780210026"} {:id "defn/selection-count", :kind "defn", :line 817, :end-line 819, :hash "102084099"} {:id "defn/nothing-selected?", :kind "defn", :line 821, :end-line 823, :hash "1570407953"} {:id "defn-/normalize-rect", :kind "defn-", :line 825, :end-line 827, :hash "-886761888"} {:id "defn-/rects-intersect?", :kind "defn-", :line 829, :end-line 831, :hash "465071508"} {:id "defn-/stock-bounds", :kind "defn-", :line 833, :end-line 835, :hash "1578532971"} {:id "defn-/converter-bounds", :kind "defn-", :line 837, :end-line 839, :hash "1578934291"} {:id "defn-/cloud-bounds", :kind "defn-", :line 841, :end-line 843, :hash "1755981880"} {:id "defn-/link-bounds", :kind "defn-", :line 845, :end-line 852, :hash "545244540"} {:id "defn-/selectable-objects-with-bounds", :kind "defn-", :line 854, :end-line 870, :hash "-357421952"} {:id "defn-/update-selection-when-idle", :kind "defn-", :line 872, :end-line 876, :hash "1823099991"} {:id "defn/click-select", :kind "defn", :line 878, :end-line 890, :hash "1711349759"} {:id "defn/shift-click-select", :kind "defn", :line 892, :end-line 904, :hash "1555171200"} {:id "defn/marquee-select", :kind "defn", :line 906, :end-line 917, :hash "1674158571"} {:id "defn/clear-selection", :kind "defn", :line 919, :end-line 921, :hash "1454538573"} {:id "defn-/links-referencing-ref", :kind "defn-", :line 923, :end-line 929, :hash "864942845"} {:id "defn-/cascade-additions-for-ref", :kind "defn-", :line 931, :end-line 940, :hash "1156344263"} {:id "defn-/expand-delete-set", :kind "defn-", :line 942, :end-line 949, :hash "-1194035989"} {:id "defn-/remove-object-ref", :kind "defn-", :line 951, :end-line 972, :hash "1823015851"} {:id "defn/delete-selection", :kind "defn", :line 974, :end-line 982, :hash "1915710064"}]}
;; clj-mutate-manifest-end
