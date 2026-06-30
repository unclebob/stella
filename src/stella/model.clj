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
   :simulation {:time 0.0 :stock-values {}}
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

(defn control-panel-visible?
  [_shell]
  true)

(defn step-button-visible?
  [_shell]
  true)

(defn format-display-number
  [n]
  (let [rounded (/ (Math/round (* n 10)) 10.0)]
    (if (= rounded (double (long rounded)))
      (str (long rounded))
      (str rounded))))

(defn simulation-time-display
  [shell]
  (let [time (get-in shell [:diagram :simulation :time] 0.0)
        rounded (/ (Math/round (* time 10)) 10.0)]
    (if (zero? rounded) "0" (format-display-number rounded))))

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

(defn- clear-link-drafts
  [diagram]
  (-> diagram
      (assoc :flow-draft nil)
      (assoc :connector-draft nil)))

(defn disarm-placement
  [diagram]
  (-> diagram
      (assoc :placement-mode :idle)
      clear-link-drafts))

(defn- arm-placement-mode
  [diagram mode]
  (-> diagram
      (assoc :placement-mode mode)
      clear-link-drafts))

(def ^:private converter-preview-label-width 100)
(def ^:private converter-preview-radius 25)
(def ^:private cloud-preview-inset 16)

(defn preview-anchor-insets
  "Minimum canvas coordinates so placement previews stay inside the canvas pane."
  [placement-mode]
  (case placement-mode
    :converter {:min-x (+ converter-preview-radius
                          (quot converter-preview-label-width 2))
                :min-y 0}
    (:source :sink) {:min-x cloud-preview-inset :min-y 0}
    {:min-x 0 :min-y 0}))

(defn arm-stock-placement
  [diagram]
  (arm-placement-mode diagram :stock))

(defn place-stock
  [diagram x y]
  (if (= :stock (:placement-mode diagram))
    (let [num (:next-stock-num diagram)
          name (str "Stock" num)
          id (keyword (str "stock-" num))
          stock {:name name :initial-value "0" :min-value "0" :max-value "100" :x x :y y}]
      (-> diagram
          (assoc-in [:stocks id] stock)
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
                {:initial-value "0" :min-value "0" :max-value "100"}))

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
  (arm-placement-mode diagram :source))

(defn arm-sink-placement
  [diagram]
  (arm-placement-mode diagram :sink))

(defn- place-cloud
  [diagram mode collection next-key id-prefix label-prefix x y]
  (if (= mode (:placement-mode diagram))
    (let [num (get diagram next-key)
          name (str label-prefix num)
          id (keyword (str id-prefix num))]
      (-> diagram
          (assoc-in [collection id] {:name name :x x :y y})
          (update next-key inc)
          disarm-placement))
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

(defn fixture-flow-from-source
  [diagram flow-name source-name stock-name]
  (fixture-endpoint-link diagram "Flow" :next-flow-num "flow-" :flows flow-name
                         :source source-name :stock stock-name {:rate "0"}))

(defn fixture-flow-to-sink
  [diagram flow-name stock-name sink-name]
  (fixture-endpoint-link diagram "Flow" :next-flow-num "flow-" :flows flow-name
                         :stock stock-name :sink sink-name {:rate "0"}))

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
        (assoc draft-key nil)
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

(defn arm-flow-placement
  [diagram]
  (arm-placement-mode diagram :flow))

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
  (arm-placement-mode diagram :converter))

(defn place-converter
  [diagram x y]
  (if (= :converter (:placement-mode diagram))
    (let [num (:next-converter-num diagram)
          name (str "Converter" num)
          id (keyword (str "converter-" num))]
      (-> diagram
          (assoc-in [:converters id] {:name name :value "0" :x x :y y})
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
  (arm-placement-mode diagram :connector))

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
      :flow (ellipse-boundary-point center target 12.0 12.0)
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

(defn- point-near-connector-handle?
  [diagram connector radius x y]
  (when-let [[mx my] (:midpoint (connector-curve-points diagram connector))]
    (let [dx (- x mx)
          dy (- y my)]
      (<= (+ (* dx dx) (* dy dy)) (* radius radius)))))

(defn object-at-canvas-point
  [diagram x y]
  (let [click-radius 5.0]
    (some identity
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
                 :when (point-near-connector-handle? diagram connector (+ 10.0 click-radius) x y)]
             (object-ref :connector name))))))

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
     (when-let [[mx my] (:midpoint (connector-curve-points diagram connector))]
       [(object-ref :connector name)
        [(- mx 15) (- my 15) (+ mx 15) (+ my 15)]]))
   (for [[_ {:keys [name] :as source}] (:sources diagram)]
     [(object-ref :source name) (cloud-bounds source)])
   (for [[_ {:keys [name] :as sink}] (:sinks diagram)]
     [(object-ref :sink name) (cloud-bounds sink)])))

(defn- selection-update-allowed?
  [diagram kind]
  (or (placement-disarmed? diagram)
      (= kind :connector)))

(defn- update-selection-when-allowed
  [diagram kind update-fn]
  (if (selection-update-allowed? diagram kind)
    (update-fn diagram)
    diagram))

(defn click-select
  [diagram kind name]
  (update-selection-when-allowed
   diagram
   kind
   (fn [diagram]
     (if (endpoint-exists? diagram kind name)
       (let [ref (object-ref kind name)
             selection (:selection diagram #{})]
         (if (= selection #{ref})
           (assoc diagram :selection (disj selection ref))
           (assoc diagram :selection #{ref})))
       diagram))))

(defn click-select-at
  [diagram x y]
  (if-let [{:keys [kind id]} (object-at-canvas-point diagram x y)]
    (click-select diagram kind id)
    diagram))

(defn shift-click-select
  [diagram kind name]
  (update-selection-when-allowed
   diagram
   kind
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
  (update-selection-when-allowed
   diagram
   nil
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
;; {:version 1, :tested-at "2026-06-30T08:38:59.056895-05:00", :module-hash "385045793", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-289228109"} {:id "defn-/menu-item", :kind "defn-", :line 3, :end-line 4, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 6, :end-line 7, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 9, :end-line 16, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 18, :end-line 25, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 27, :end-line 31, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 33, :end-line 35, :hash "229400292"} {:id "defn/default-diagram", :kind "defn", :line 37, :end-line 54, :hash "-1752611020"} {:id "defn/default-shell", :kind "defn", :line 56, :end-line 65, :hash "-1757765176"} {:id "defn/top-level-menus", :kind "defn", :line 67, :end-line 69, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 71, :end-line 73, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 75, :end-line 77, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 79, :end-line 83, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 85, :end-line 87, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 89, :end-line 91, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 93, :end-line 95, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 97, :end-line 99, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 101, :end-line 103, :hash "-256542367"} {:id "defn/control-panel-visible?", :kind "defn", :line 105, :end-line 107, :hash "1412247417"} {:id "defn/step-button-visible?", :kind "defn", :line 109, :end-line 111, :hash "429715676"} {:id "defn/format-display-number", :kind "defn", :line 113, :end-line 118, :hash "-161819044"} {:id "defn/simulation-time-display", :kind "defn", :line 120, :end-line 124, :hash "1960864982"} {:id "defn/stock-count", :kind "defn", :line 126, :end-line 128, :hash "-1679213761"} {:id "defn-/stock-entry-by-name", :kind "defn-", :line 130, :end-line 132, :hash "407514139"} {:id "defn/stock-exists?", :kind "defn", :line 134, :end-line 136, :hash "2041739431"} {:id "defn/stock-position", :kind "defn", :line 138, :end-line 141, :hash "69915530"} {:id "def/stock-icon-width", :kind "def", :line 143, :end-line 143, :hash "-1231315932"} {:id "def/stock-icon-height", :kind "def", :line 144, :end-line 144, :hash "-1007163347"} {:id "defn/stock-at-canvas-point", :kind "defn", :line 146, :end-line 153, :hash "786701798"} {:id "defn-/stock-field", :kind "defn-", :line 155, :end-line 158, :hash "-2043085379"} {:id "defn/move-stock", :kind "defn", :line 160, :end-line 164, :hash "990066087"} {:id "defn/stock-initial-value", :kind "defn", :line 166, :end-line 168, :hash "-567431284"} {:id "defn/stock-min-value", :kind "defn", :line 170, :end-line 172, :hash "1340209325"} {:id "defn/stock-max-value", :kind "defn", :line 174, :end-line 176, :hash "684410828"} {:id "defn/stock-named?", :kind "defn", :line 178, :end-line 180, :hash "-166675305"} {:id "defn-/numeric-value", :kind "defn-", :line 182, :end-line 184, :hash "-1365632356"} {:id "defn-/value-within-bounds?", :kind "defn-", :line 186, :end-line 192, :hash "-1066502942"} {:id "defn-/rename-endpoint-id", :kind "defn-", :line 194, :end-line 198, :hash "-1464052815"} {:id "defn-/rename-stock-endpoints", :kind "defn-", :line 200, :end-line 215, :hash "1076302708"} {:id "defn-/rename-blocked?", :kind "defn-", :line 217, :end-line 222, :hash "192871783"} {:id "defn/set-stock-name", :kind "defn", :line 224, :end-line 231, :hash "1651947829"} {:id "defn/set-stock-initial-value", :kind "defn", :line 233, :end-line 239, :hash "982226397"} {:id "defn-/set-stock-bound", :kind "defn-", :line 241, :end-line 247, :hash "2008645735"} {:id "defn/set-stock-min", :kind "defn", :line 249, :end-line 256, :hash "-405763036"} {:id "defn/set-stock-max", :kind "defn", :line 258, :end-line 263, :hash "218751066"} {:id "defn/clear-stock-max", :kind "defn", :line 265, :end-line 269, :hash "398894882"} {:id "defn/apply-stock-edit", :kind "defn", :line 271, :end-line 297, :hash "1749599298"} {:id "defn/placement-disarmed?", :kind "defn", :line 299, :end-line 301, :hash "351569028"} {:id "defn-/clear-link-drafts", :kind "defn-", :line 303, :end-line 307, :hash "-893099665"} {:id "defn/disarm-placement", :kind "defn", :line 309, :end-line 313, :hash "-1246862111"} {:id "defn-/arm-placement-mode", :kind "defn-", :line 315, :end-line 319, :hash "-1941633341"} {:id "def/converter-preview-label-width", :kind "def", :line 321, :end-line 321, :hash "-910293531"} {:id "def/converter-preview-radius", :kind "def", :line 322, :end-line 322, :hash "1360968763"} {:id "def/cloud-preview-inset", :kind "def", :line 323, :end-line 323, :hash "1480483855"} {:id "defn/preview-anchor-insets", :kind "defn", :line 325, :end-line 333, :hash "-371116550"} {:id "defn/arm-stock-placement", :kind "defn", :line 335, :end-line 337, :hash "-1988976081"} {:id "defn/place-stock", :kind "defn", :line 339, :end-line 349, :hash "-972441214"} {:id "defn/stocks", :kind "defn", :line 351, :end-line 353, :hash "-801020159"} {:id "defn-/num-from-name", :kind "defn-", :line 355, :end-line 357, :hash "1864201808"} {:id "defn-/fixture-item", :kind "defn-", :line 359, :end-line 365, :hash "-1104049080"} {:id "defn/fixture-stock", :kind "defn", :line 367, :end-line 370, :hash "1548349638"} {:id "defn-/endpoint-ref", :kind "defn-", :line 372, :end-line 374, :hash "916366387"} {:id "defn-/source-entry-by-name", :kind "defn-", :line 376, :end-line 378, :hash "1345064248"} {:id "defn-/sink-entry-by-name", :kind "defn-", :line 380, :end-line 382, :hash "-2143000880"} {:id "defn/source-exists?", :kind "defn", :line 384, :end-line 386, :hash "1146108815"} {:id "defn/sink-exists?", :kind "defn", :line 388, :end-line 390, :hash "-2079647085"} {:id "defn/source-position", :kind "defn", :line 392, :end-line 395, :hash "1966756"} {:id "defn/sink-position", :kind "defn", :line 397, :end-line 400, :hash "856729128"} {:id "defn-/move-cloud", :kind "defn-", :line 402, :end-line 406, :hash "520543625"} {:id "defn/move-source", :kind "defn", :line 408, :end-line 410, :hash "1619637978"} {:id "defn/move-sink", :kind "defn", :line 412, :end-line 414, :hash "-1730874184"} {:id "form/71/declare", :kind "declare", :line 416, :end-line 416, :hash "-1373642061"} {:id "defn-/endpoint-position-resolvers", :kind "defn-", :line 418, :end-line 424, :hash "-1370097299"} {:id "defn/endpoint-position", :kind "defn", :line 426, :end-line 429, :hash "-1526732716"} {:id "def/endpoint-anchor-offsets", :kind "def", :line 431, :end-line 439, :hash "-1752866723"} {:id "defn/endpoint-anchor", :kind "defn", :line 441, :end-line 444, :hash "-1604573613"} {:id "defn/source-count", :kind "defn", :line 446, :end-line 448, :hash "913527496"} {:id "defn/sink-count", :kind "defn", :line 450, :end-line 452, :hash "1591552436"} {:id "defn/sources", :kind "defn", :line 454, :end-line 456, :hash "-973921734"} {:id "defn/sinks", :kind "defn", :line 458, :end-line 460, :hash "1773380131"} {:id "defn-/cloud-at-canvas-point-in", :kind "defn-", :line 462, :end-line 468, :hash "-558702525"} {:id "defn/source-at-canvas-point", :kind "defn", :line 470, :end-line 473, :hash "-720631851"} {:id "defn/sink-at-canvas-point", :kind "defn", :line 475, :end-line 478, :hash "-1057973209"} {:id "defn/cloud-at-canvas-point", :kind "defn", :line 480, :end-line 486, :hash "1471420906"} {:id "defn/fixture-source", :kind "defn", :line 488, :end-line 490, :hash "-1647492037"} {:id "defn/fixture-sink", :kind "defn", :line 492, :end-line 494, :hash "453893490"} {:id "defn/arm-source-placement", :kind "defn", :line 496, :end-line 498, :hash "1286577556"} {:id "defn/arm-sink-placement", :kind "defn", :line 500, :end-line 502, :hash "1149357822"} {:id "defn-/place-cloud", :kind "defn-", :line 504, :end-line 513, :hash "-613638060"} {:id "defn/place-source", :kind "defn", :line 515, :end-line 517, :hash "394591180"} {:id "defn/place-sink", :kind "defn", :line 519, :end-line 521, :hash "785372302"} {:id "defn/source-placement-disarmed?", :kind "defn", :line 523, :end-line 525, :hash "1135284315"} {:id "defn/sink-placement-disarmed?", :kind "defn", :line 527, :end-line 529, :hash "-1769136352"} {:id "defn-/flow-entry-by-name", :kind "defn-", :line 531, :end-line 533, :hash "-1730283296"} {:id "defn/flow-exists?", :kind "defn", :line 535, :end-line 537, :hash "905629296"} {:id "defn-/flow-attribute", :kind "defn-", :line 539, :end-line 542, :hash "1273430238"} {:id "defn/flow-from", :kind "defn", :line 544, :end-line 546, :hash "-751374872"} {:id "defn/flow-to", :kind "defn", :line 548, :end-line 550, :hash "1500420793"} {:id "defn/flow-endpoints", :kind "defn", :line 552, :end-line 557, :hash "1470373818"} {:id "defn/flow-rate", :kind "defn", :line 559, :end-line 561, :hash "-2102829222"} {:id "defn-/rename-endpoints-in-connectors", :kind "defn-", :line 563, :end-line 568, :hash "-1425675650"} {:id "defn/set-flow-name", :kind "defn", :line 570, :end-line 577, :hash "-79078843"} {:id "defn-/parseable-number?", :kind "defn-", :line 579, :end-line 585, :hash "928557303"} {:id "defn/set-flow-rate", :kind "defn", :line 587, :end-line 593, :hash "-112745979"} {:id "defn/apply-flow-edit", :kind "defn", :line 595, :end-line 605, :hash "1518416297"} {:id "defn/flow-count", :kind "defn", :line 607, :end-line 609, :hash "-2009265396"} {:id "defn/flows", :kind "defn", :line 611, :end-line 613, :hash "608017970"} {:id "defn-/fixture-named-link", :kind "defn-", :line 615, :end-line 621, :hash "999899682"} {:id "defn-/fixture-endpoint-link", :kind "defn-", :line 623, :end-line 629, :hash "-1371478247"} {:id "defn/fixture-flow", :kind "defn", :line 631, :end-line 634, :hash "614964979"} {:id "defn/fixture-flow-from-source", :kind "defn", :line 636, :end-line 639, :hash "-126009109"} {:id "defn/fixture-flow-to-sink", :kind "defn", :line 641, :end-line 644, :hash "1289106061"} {:id "defn-/valid-flow-pair?", :kind "defn-", :line 646, :end-line 652, :hash "-269751023"} {:id "defn-/create-collection-link!", :kind "defn-", :line 654, :end-line 662, :hash "310313692"} {:id "defn-/create-flow!", :kind "defn-", :line 664, :end-line 673, :hash "-1588176197"} {:id "defn/arm-flow-placement", :kind "defn", :line 675, :end-line 677, :hash "508715994"} {:id "defn/flow-placement-armed?", :kind "defn", :line 679, :end-line 681, :hash "-943631268"} {:id "defn-/draft-from-endpoint", :kind "defn-", :line 683, :end-line 692, :hash "1978981143"} {:id "defn/select-flow-source", :kind "defn", :line 694, :end-line 700, :hash "-323164585"} {:id "defn-/clear-flow-draft", :kind "defn-", :line 702, :end-line 704, :hash "-371891314"} {:id "form/120/declare", :kind "declare", :line 706, :end-line 706, :hash "988362584"} {:id "defn-/endpoint-existence-checks", :kind "defn-", :line 708, :end-line 715, :hash "1146175053"} {:id "defn-/endpoint-exists?", :kind "defn-", :line 717, :end-line 720, :hash "-1897963117"} {:id "defn-/try-connect-flow", :kind "defn-", :line 722, :end-line 730, :hash "822432826"} {:id "defn/connect-flow", :kind "defn", :line 732, :end-line 736, :hash "-817203079"} {:id "defn/flow-placement-disarmed?", :kind "defn", :line 738, :end-line 740, :hash "31272842"} {:id "defn-/converter-entry-by-name", :kind "defn-", :line 742, :end-line 744, :hash "-1383344081"} {:id "defn/converter-exists?", :kind "defn", :line 746, :end-line 748, :hash "300239202"} {:id "defn/converter-position", :kind "defn", :line 750, :end-line 753, :hash "843288393"} {:id "def/converter-icon-size", :kind "def", :line 755, :end-line 755, :hash "1952596549"} {:id "defn/converter-at-canvas-point", :kind "defn", :line 757, :end-line 764, :hash "-59067732"} {:id "defn/move-converter", :kind "defn", :line 766, :end-line 770, :hash "-1179681609"} {:id "defn/converter-value", :kind "defn", :line 772, :end-line 775, :hash "13277447"} {:id "defn/converter-count", :kind "defn", :line 777, :end-line 779, :hash "267660665"} {:id "defn/converters", :kind "defn", :line 781, :end-line 783, :hash "-1686748773"} {:id "defn/fixture-converter", :kind "defn", :line 785, :end-line 788, :hash "-475935667"} {:id "defn/arm-converter-placement", :kind "defn", :line 790, :end-line 792, :hash "-743641004"} {:id "defn/place-converter", :kind "defn", :line 794, :end-line 803, :hash "-89935502"} {:id "defn/converter-placement-disarmed?", :kind "defn", :line 805, :end-line 807, :hash "887795504"} {:id "defn/flow-midpoint", :kind "defn", :line 809, :end-line 816, :hash "1396828306"} {:id "def/clickable-kinds-by-mode", :kind "def", :line 818, :end-line 820, :hash "733363462"} {:id "defn/endpoint-clickable?", :kind "defn", :line 822, :end-line 824, :hash "1518566357"} {:id "defn-/connector-entry-by-name", :kind "defn-", :line 826, :end-line 828, :hash "971711688"} {:id "defn/connector-exists?", :kind "defn", :line 830, :end-line 832, :hash "314481541"} {:id "defn-/connector-attribute", :kind "defn-", :line 834, :end-line 837, :hash "709631432"} {:id "defn/connector-from", :kind "defn", :line 839, :end-line 841, :hash "524525571"} {:id "defn/connector-to", :kind "defn", :line 843, :end-line 845, :hash "799906750"} {:id "defn/connector-formula", :kind "defn", :line 847, :end-line 849, :hash "-1029787925"} {:id "defn/set-converter-name", :kind "defn", :line 851, :end-line 858, :hash "-847157151"} {:id "defn-/converter-to-flow-connector-id", :kind "defn-", :line 860, :end-line 867, :hash "-195957966"} {:id "defn/set-converter-formula", :kind "defn", :line 869, :end-line 875, :hash "-823051875"} {:id "defn/converter-connector-formula", :kind "defn", :line 877, :end-line 880, :hash "1867041656"} {:id "defn/apply-converter-edit", :kind "defn", :line 882, :end-line 893, :hash "-1686629901"} {:id "defn-/fixture-connector-endpoints", :kind "defn-", :line 895, :end-line 905, :hash "-179157893"} {:id "defn/fixture-connector", :kind "defn", :line 907, :end-line 910, :hash "-1936572057"} {:id "defn/fixture-stock-connector", :kind "defn", :line 912, :end-line 915, :hash "1964218954"} {:id "defn/connector-count", :kind "defn", :line 917, :end-line 919, :hash "-1826970045"} {:id "defn/connectors", :kind "defn", :line 921, :end-line 923, :hash "-1048990130"} {:id "defn-/valid-connector-pair?", :kind "defn-", :line 925, :end-line 930, :hash "1385416327"} {:id "defn-/create-connector!", :kind "defn-", :line 932, :end-line 941, :hash "364741780"} {:id "defn/arm-connector-placement", :kind "defn", :line 943, :end-line 945, :hash "-1175395888"} {:id "defn/connector-placement-armed?", :kind "defn", :line 947, :end-line 949, :hash "-675985363"} {:id "defn/select-connector-origin", :kind "defn", :line 951, :end-line 959, :hash "1914302439"} {:id "defn-/try-connect-connector", :kind "defn-", :line 961, :end-line 973, :hash "1616656565"} {:id "defn/connect-connector", :kind "defn", :line 975, :end-line 980, :hash "1621379443"} {:id "defn/connector-placement-disarmed?", :kind "defn", :line 982, :end-line 984, :hash "-1401416734"} {:id "defn-/object-ref", :kind "defn-", :line 986, :end-line 988, :hash "256234122"} {:id "defn/selected?", :kind "defn", :line 990, :end-line 992, :hash "780210026"} {:id "defn/selection-count", :kind "defn", :line 994, :end-line 996, :hash "102084099"} {:id "defn/nothing-selected?", :kind "defn", :line 998, :end-line 1000, :hash "1570407953"} {:id "defn-/normalize-rect", :kind "defn-", :line 1002, :end-line 1004, :hash "-886761888"} {:id "defn-/rects-intersect?", :kind "defn-", :line 1006, :end-line 1008, :hash "465071508"} {:id "defn-/point-in-rect?", :kind "defn-", :line 1010, :end-line 1013, :hash "658438042"} {:id "defn-/circle-intersects-rect?", :kind "defn-", :line 1015, :end-line 1021, :hash "384019027"} {:id "defn-/stock-bounds", :kind "defn-", :line 1023, :end-line 1025, :hash "1578532971"} {:id "defn-/converter-bounds", :kind "defn-", :line 1027, :end-line 1029, :hash "1578934291"} {:id "defn-/cloud-bounds", :kind "defn-", :line 1031, :end-line 1033, :hash "1755981880"} {:id "defn-/link-bounds", :kind "defn-", :line 1035, :end-line 1042, :hash "545244540"} {:id "defn-/point-segment-distance", :kind "defn-", :line 1044, :end-line 1060, :hash "-1837605759"} {:id "def/canvas-center", :kind "def", :line 1062, :end-line 1062, :hash "-823088025"} {:id "defn-/connector-default-control-offset", :kind "defn-", :line 1064, :end-line 1078, :hash "107222231"} {:id "defn-/connector-control-point", :kind "defn-", :line 1080, :end-line 1085, :hash "258390474"} {:id "defn-/quadratic-point", :kind "defn-", :line 1087, :end-line 1098, :hash "-1382276052"} {:id "defn-/connector-curve-midpoint", :kind "defn-", :line 1100, :end-line 1102, :hash "-668851218"} {:id "defn-/endpoint-center", :kind "defn-", :line 1104, :end-line 1110, :hash "-652861683"} {:id "defn-/ellipse-boundary-point", :kind "defn-", :line 1112, :end-line 1120, :hash "-1806779819"} {:id "defn-/rectangle-boundary-point", :kind "defn-", :line 1122, :end-line 1130, :hash "878119706"} {:id "defn-/endpoint-boundary-point", :kind "defn-", :line 1132, :end-line 1140, :hash "55404980"} {:id "defn-/visible-link-endpoints", :kind "defn-", :line 1142, :end-line 1147, :hash "-1884022774"} {:id "defn-/point-near-link?", :kind "defn-", :line 1149, :end-line 1155, :hash "-407336029"} {:id "defn-/connector-curve-points", :kind "defn-", :line 1157, :end-line 1167, :hash "592816946"} {:id "defn-/point-near-connector-handle?", :kind "defn-", :line 1169, :end-line 1174, :hash "-185775857"} {:id "defn/object-at-canvas-point", :kind "defn", :line 1176, :end-line 1198, :hash "-1991665536"} {:id "defn/connector-handle-position", :kind "defn", :line 1200, :end-line 1203, :hash "-1533459181"} {:id "defn/move-connector-handle", :kind "defn", :line 1205, :end-line 1216, :hash "-1062521520"} {:id "defn-/selectable-objects-with-bounds", :kind "defn-", :line 1218, :end-line 1235, :hash "5577208"} {:id "defn-/selection-update-allowed?", :kind "defn-", :line 1237, :end-line 1240, :hash "-1752512220"} {:id "defn-/update-selection-when-allowed", :kind "defn-", :line 1242, :end-line 1246, :hash "491192258"} {:id "defn/click-select", :kind "defn", :line 1248, :end-line 1260, :hash "583368951"} {:id "defn/click-select-at", :kind "defn", :line 1262, :end-line 1266, :hash "1167079823"} {:id "defn/shift-click-select", :kind "defn", :line 1268, :end-line 1281, :hash "-2113900096"} {:id "defn/shift-click-select-at", :kind "defn", :line 1283, :end-line 1287, :hash "959922948"} {:id "defn/marquee-select", :kind "defn", :line 1289, :end-line 1301, :hash "-629623796"} {:id "defn/clear-selection", :kind "defn", :line 1303, :end-line 1305, :hash "1454538573"} {:id "defn-/links-referencing-ref", :kind "defn-", :line 1307, :end-line 1313, :hash "864942845"} {:id "defn-/cascade-additions-for-ref", :kind "defn-", :line 1315, :end-line 1324, :hash "1156344263"} {:id "defn-/expand-delete-set", :kind "defn-", :line 1326, :end-line 1333, :hash "-1194035989"} {:id "defn-/remove-object-ref", :kind "defn-", :line 1335, :end-line 1356, :hash "1823015851"} {:id "defn/delete-selection", :kind "defn", :line 1358, :end-line 1366, :hash "1915710064"}]}
;; clj-mutate-manifest-end
