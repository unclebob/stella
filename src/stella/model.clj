(ns stella.model
  (:require [clojure.string :as str]
            [stella.formula :as formula]
            [stella.numbers :as numbers]))

(declare refresh-converter-rates)

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
   :simulation-running? false
   :simulation-tick-delay "1"
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

(defn speed-slider-visible?
  [_shell]
  true)

(defn simulation-tick-delay-display
  [shell]
  (str (:simulation-tick-delay shell "1")))

(defn set-simulation-tick-delay
  [shell delay]
  (assoc shell :simulation-tick-delay (str delay)))

(defn simulation-running?
  [shell]
  (true? (:simulation-running? shell)))

(defn run-button-label
  [shell]
  (if (simulation-running? shell) "Stop" "Run"))

(defn start-simulation-run
  [shell]
  (assoc shell :simulation-running? true))

(defn stop-simulation-run
  [shell]
  (assoc shell :simulation-running? false))

(def ^:private palette-tool-modes
  {"Stock" :stock
   "Flow" :flow
   "Source" :source
   "Sink" :sink
   "Converter" :converter
   "Connector" :connector})

(defn palette-tool-active?
  [shell tool-label]
  (when-let [mode (get palette-tool-modes tool-label)]
    (= mode (get-in shell [:diagram :placement-mode]))))

(defn no-palette-tool-active?
  [shell]
  (= :idle (get-in shell [:diagram :placement-mode])))

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
  (numbers/parse-number value))

(defn stock-numeric-value
  [diagram name]
  (let [raw (or (get-in diagram [:simulation :stock-values name])
                (stock-initial-value diagram name)
                "0")]
    (if (number? raw)
      (double raw)
      (numeric-value raw))))

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
      (-> diagram
          (assoc-in [:stocks id :initial-value]
                    (numbers/normalize-number-string value))
          refresh-converter-rates)
      diagram)
    diagram))

(defn- set-stock-bound
  [diagram name bound-key value valid?]
  (if-let [[id stock] (stock-entry-by-name diagram name)]
    (if (valid? stock value)
      (assoc-in diagram [:stocks id bound-key]
                (numbers/normalize-number-string value))
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

(defn set-flow-rate
  [diagram name rate]
  (if-let [[id _] (flow-entry-by-name diagram name)]
    (if (numbers/parseable-number? rate)
      (assoc-in diagram [:flows id :rate] (numbers/normalize-number-string rate))
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
        (if (numbers/parseable-number? rate)
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

(defn- stock-names
  [diagram]
  (set (map :name (stocks diagram))))

(defn- formula-stock-value
  [diagram]
  (fn [stock-name]
    (stock-numeric-value diagram stock-name)))

(defn- refresh-converter
  [diagram converter-name]
  (if-let [connector-id (converter-to-flow-connector-id diagram converter-name)]
    (let [formula-text (get-in diagram [:connectors connector-id :formula] "")
          [converter-id _] (converter-entry-by-name diagram converter-name)
          flow-name (:id (:to (get-in diagram [:connectors connector-id])))
          [flow-id _] (flow-entry-by-name diagram flow-name)
          computed (if (seq formula-text)
                     (format-display-number
                      (formula/evaluate formula-text (formula-stock-value diagram)))
                     "0")]
      (-> diagram
          (assoc-in [:converters converter-id :value] computed)
          (assoc-in [:flows flow-id :rate] computed)))
    diagram))

(defn refresh-converter-rates
  [diagram]
  (reduce refresh-converter diagram (map :name (converters diagram))))

(defn set-converter-formula
  [diagram converter-name formula]
  (if-let [id (converter-to-flow-connector-id diagram converter-name)]
    (let [formula-text (str/trim (str formula))]
      (cond
        (empty? formula-text)
        diagram

        (not (formula/valid-for-stocks? formula-text (stock-names diagram)))
        diagram

        :else
        (-> diagram
            (assoc-in [:connectors id :formula] formula-text)
            (refresh-converter converter-name))))
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
;; {:version 1, :tested-at "2026-06-30T11:27:42.314908-05:00", :module-hash "-1777584369", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "1625206636"} {:id "form/1/declare", :kind "declare", :line 6, :end-line 6, :hash "-1429393515"} {:id "defn-/menu-item", :kind "defn-", :line 8, :end-line 9, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 11, :end-line 12, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 14, :end-line 21, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 23, :end-line 30, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 32, :end-line 36, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 38, :end-line 40, :hash "229400292"} {:id "defn/default-diagram", :kind "defn", :line 42, :end-line 59, :hash "-1752611020"} {:id "defn/default-shell", :kind "defn", :line 61, :end-line 70, :hash "-1757765176"} {:id "defn/top-level-menus", :kind "defn", :line 72, :end-line 74, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 76, :end-line 78, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 80, :end-line 82, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 84, :end-line 88, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 90, :end-line 92, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 94, :end-line 96, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 98, :end-line 100, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 102, :end-line 104, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 106, :end-line 108, :hash "-256542367"} {:id "defn/control-panel-visible?", :kind "defn", :line 110, :end-line 112, :hash "1412247417"} {:id "defn/step-button-visible?", :kind "defn", :line 114, :end-line 116, :hash "429715676"} {:id "defn/round-to-tenth", :kind "defn", :line 118, :end-line 120, :hash "-1922427386"} {:id "def/palette-tool-modes", :kind "def", :line 122, :end-line 128, :hash "-421860568"} {:id "defn/palette-tool-active?", :kind "defn", :line 130, :end-line 133, :hash "-1525629561"} {:id "defn/no-palette-tool-active?", :kind "defn", :line 135, :end-line 137, :hash "-339321813"} {:id "defn/format-display-number", :kind "defn", :line 139, :end-line 144, :hash "-1773325740"} {:id "defn/simulation-time-display", :kind "defn", :line 146, :end-line 149, :hash "1724039993"} {:id "defn/stock-count", :kind "defn", :line 151, :end-line 153, :hash "-1679213761"} {:id "defn-/stock-entry-by-name", :kind "defn-", :line 155, :end-line 157, :hash "407514139"} {:id "defn/stock-exists?", :kind "defn", :line 159, :end-line 161, :hash "2041739431"} {:id "defn/stock-position", :kind "defn", :line 163, :end-line 166, :hash "69915530"} {:id "def/stock-icon-width", :kind "def", :line 168, :end-line 168, :hash "-1231315932"} {:id "def/stock-icon-height", :kind "def", :line 169, :end-line 169, :hash "-1007163347"} {:id "defn/stock-at-canvas-point", :kind "defn", :line 171, :end-line 178, :hash "786701798"} {:id "defn-/stock-field", :kind "defn-", :line 180, :end-line 183, :hash "-2043085379"} {:id "defn/move-stock", :kind "defn", :line 185, :end-line 189, :hash "990066087"} {:id "defn/stock-initial-value", :kind "defn", :line 191, :end-line 193, :hash "-567431284"} {:id "defn/stock-min-value", :kind "defn", :line 195, :end-line 197, :hash "1340209325"} {:id "defn/stock-max-value", :kind "defn", :line 199, :end-line 201, :hash "684410828"} {:id "defn/stock-named?", :kind "defn", :line 203, :end-line 205, :hash "-166675305"} {:id "defn-/numeric-value", :kind "defn-", :line 207, :end-line 209, :hash "-1861362132"} {:id "defn/stock-numeric-value", :kind "defn", :line 211, :end-line 218, :hash "1173218134"} {:id "defn-/value-within-bounds?", :kind "defn-", :line 220, :end-line 226, :hash "-1066502942"} {:id "defn-/rename-endpoint-id", :kind "defn-", :line 228, :end-line 232, :hash "-1464052815"} {:id "defn-/rename-stock-endpoints", :kind "defn-", :line 234, :end-line 249, :hash "1076302708"} {:id "defn-/rename-blocked?", :kind "defn-", :line 251, :end-line 256, :hash "192871783"} {:id "defn/set-stock-name", :kind "defn", :line 258, :end-line 265, :hash "1651947829"} {:id "defn/set-stock-initial-value", :kind "defn", :line 267, :end-line 276, :hash "-1190612462"} {:id "defn-/set-stock-bound", :kind "defn-", :line 278, :end-line 285, :hash "-2081078046"} {:id "defn/set-stock-min", :kind "defn", :line 287, :end-line 294, :hash "-405763036"} {:id "defn/set-stock-max", :kind "defn", :line 296, :end-line 301, :hash "218751066"} {:id "defn/clear-stock-max", :kind "defn", :line 303, :end-line 307, :hash "398894882"} {:id "defn/apply-stock-edit", :kind "defn", :line 309, :end-line 335, :hash "1749599298"} {:id "defn/placement-disarmed?", :kind "defn", :line 337, :end-line 339, :hash "351569028"} {:id "defn-/clear-link-drafts", :kind "defn-", :line 341, :end-line 345, :hash "-893099665"} {:id "defn/disarm-placement", :kind "defn", :line 347, :end-line 351, :hash "-1246862111"} {:id "defn-/arm-placement-mode", :kind "defn-", :line 353, :end-line 357, :hash "-1941633341"} {:id "def/converter-preview-label-width", :kind "def", :line 359, :end-line 359, :hash "-910293531"} {:id "def/converter-preview-radius", :kind "def", :line 360, :end-line 360, :hash "1360968763"} {:id "def/cloud-preview-inset", :kind "def", :line 361, :end-line 361, :hash "1480483855"} {:id "defn/preview-anchor-insets", :kind "defn", :line 363, :end-line 371, :hash "-371116550"} {:id "defn/arm-stock-placement", :kind "defn", :line 373, :end-line 375, :hash "-1988976081"} {:id "defn/place-stock", :kind "defn", :line 377, :end-line 387, :hash "489507867"} {:id "defn/stocks", :kind "defn", :line 389, :end-line 391, :hash "-801020159"} {:id "defn-/num-from-name", :kind "defn-", :line 393, :end-line 395, :hash "1864201808"} {:id "defn-/fixture-item", :kind "defn-", :line 397, :end-line 403, :hash "-1104049080"} {:id "defn/fixture-stock", :kind "defn", :line 405, :end-line 408, :hash "34880668"} {:id "defn-/endpoint-ref", :kind "defn-", :line 410, :end-line 412, :hash "916366387"} {:id "defn-/source-entry-by-name", :kind "defn-", :line 414, :end-line 416, :hash "1345064248"} {:id "defn-/sink-entry-by-name", :kind "defn-", :line 418, :end-line 420, :hash "-2143000880"} {:id "defn/source-exists?", :kind "defn", :line 422, :end-line 424, :hash "1146108815"} {:id "defn/sink-exists?", :kind "defn", :line 426, :end-line 428, :hash "-2079647085"} {:id "defn/source-position", :kind "defn", :line 430, :end-line 433, :hash "1966756"} {:id "defn/sink-position", :kind "defn", :line 435, :end-line 438, :hash "856729128"} {:id "defn-/move-cloud", :kind "defn-", :line 440, :end-line 444, :hash "520543625"} {:id "defn/move-source", :kind "defn", :line 446, :end-line 448, :hash "1619637978"} {:id "defn/move-sink", :kind "defn", :line 450, :end-line 452, :hash "-1730874184"} {:id "form/77/declare", :kind "declare", :line 454, :end-line 454, :hash "-1373642061"} {:id "defn-/endpoint-position-resolvers", :kind "defn-", :line 456, :end-line 462, :hash "-1370097299"} {:id "defn/endpoint-position", :kind "defn", :line 464, :end-line 467, :hash "-1526732716"} {:id "def/endpoint-anchor-offsets", :kind "def", :line 469, :end-line 477, :hash "-1752866723"} {:id "defn/endpoint-anchor", :kind "defn", :line 479, :end-line 482, :hash "-1604573613"} {:id "defn/source-count", :kind "defn", :line 484, :end-line 486, :hash "913527496"} {:id "defn/sink-count", :kind "defn", :line 488, :end-line 490, :hash "1591552436"} {:id "defn/sources", :kind "defn", :line 492, :end-line 494, :hash "-973921734"} {:id "defn/sinks", :kind "defn", :line 496, :end-line 498, :hash "1773380131"} {:id "defn-/cloud-at-canvas-point-in", :kind "defn-", :line 500, :end-line 506, :hash "-558702525"} {:id "defn/source-at-canvas-point", :kind "defn", :line 508, :end-line 511, :hash "-720631851"} {:id "defn/sink-at-canvas-point", :kind "defn", :line 513, :end-line 516, :hash "-1057973209"} {:id "defn/cloud-at-canvas-point", :kind "defn", :line 518, :end-line 524, :hash "1471420906"} {:id "defn/fixture-source", :kind "defn", :line 526, :end-line 528, :hash "-1647492037"} {:id "defn/fixture-sink", :kind "defn", :line 530, :end-line 532, :hash "453893490"} {:id "defn/arm-source-placement", :kind "defn", :line 534, :end-line 536, :hash "1286577556"} {:id "defn/arm-sink-placement", :kind "defn", :line 538, :end-line 540, :hash "1149357822"} {:id "defn-/place-cloud", :kind "defn-", :line 542, :end-line 552, :hash "-820935337"} {:id "defn/place-source", :kind "defn", :line 554, :end-line 556, :hash "394591180"} {:id "defn/place-sink", :kind "defn", :line 558, :end-line 560, :hash "785372302"} {:id "defn/source-placement-disarmed?", :kind "defn", :line 562, :end-line 564, :hash "1135284315"} {:id "defn/sink-placement-disarmed?", :kind "defn", :line 566, :end-line 568, :hash "-1769136352"} {:id "defn-/flow-entry-by-name", :kind "defn-", :line 570, :end-line 572, :hash "-1730283296"} {:id "defn/flow-exists?", :kind "defn", :line 574, :end-line 576, :hash "905629296"} {:id "defn-/flow-attribute", :kind "defn-", :line 578, :end-line 581, :hash "1273430238"} {:id "defn/flow-from", :kind "defn", :line 583, :end-line 585, :hash "-751374872"} {:id "defn/flow-to", :kind "defn", :line 587, :end-line 589, :hash "1500420793"} {:id "defn/flow-endpoints", :kind "defn", :line 591, :end-line 596, :hash "1470373818"} {:id "defn/flow-rate", :kind "defn", :line 598, :end-line 600, :hash "-2102829222"} {:id "defn-/rename-endpoints-in-connectors", :kind "defn-", :line 602, :end-line 607, :hash "-1425675650"} {:id "defn/set-flow-name", :kind "defn", :line 609, :end-line 616, :hash "-79078843"} {:id "defn/set-flow-rate", :kind "defn", :line 618, :end-line 624, :hash "1693738946"} {:id "defn/apply-flow-edit", :kind "defn", :line 626, :end-line 636, :hash "-1702185434"} {:id "defn/flow-count", :kind "defn", :line 638, :end-line 640, :hash "-2009265396"} {:id "defn/flows", :kind "defn", :line 642, :end-line 644, :hash "608017970"} {:id "defn-/fixture-named-link", :kind "defn-", :line 646, :end-line 652, :hash "999899682"} {:id "defn-/fixture-endpoint-link", :kind "defn-", :line 654, :end-line 660, :hash "-1371478247"} {:id "defn/fixture-flow", :kind "defn", :line 662, :end-line 665, :hash "614964979"} {:id "defn/fixture-flow-from-source", :kind "defn", :line 667, :end-line 670, :hash "-126009109"} {:id "defn/fixture-flow-to-sink", :kind "defn", :line 672, :end-line 675, :hash "1289106061"} {:id "defn-/valid-flow-pair?", :kind "defn-", :line 677, :end-line 683, :hash "-269751023"} {:id "defn-/create-collection-link!", :kind "defn-", :line 685, :end-line 693, :hash "310313692"} {:id "defn-/create-flow!", :kind "defn-", :line 695, :end-line 704, :hash "-1588176197"} {:id "defn/arm-flow-placement", :kind "defn", :line 706, :end-line 708, :hash "508715994"} {:id "defn/flow-placement-armed?", :kind "defn", :line 710, :end-line 712, :hash "-943631268"} {:id "defn-/draft-from-endpoint", :kind "defn-", :line 714, :end-line 723, :hash "1978981143"} {:id "defn/select-flow-source", :kind "defn", :line 725, :end-line 731, :hash "-323164585"} {:id "defn-/clear-flow-draft", :kind "defn-", :line 733, :end-line 735, :hash "-371891314"} {:id "form/125/declare", :kind "declare", :line 737, :end-line 737, :hash "988362584"} {:id "defn-/endpoint-existence-checks", :kind "defn-", :line 739, :end-line 746, :hash "1146175053"} {:id "defn-/endpoint-exists?", :kind "defn-", :line 748, :end-line 751, :hash "-1897963117"} {:id "defn-/try-connect-flow", :kind "defn-", :line 753, :end-line 761, :hash "822432826"} {:id "defn/connect-flow", :kind "defn", :line 763, :end-line 767, :hash "-817203079"} {:id "defn/flow-placement-disarmed?", :kind "defn", :line 769, :end-line 771, :hash "31272842"} {:id "defn-/converter-entry-by-name", :kind "defn-", :line 773, :end-line 775, :hash "-1383344081"} {:id "defn/converter-exists?", :kind "defn", :line 777, :end-line 779, :hash "300239202"} {:id "defn/converter-position", :kind "defn", :line 781, :end-line 784, :hash "843288393"} {:id "def/converter-icon-size", :kind "def", :line 786, :end-line 786, :hash "1952596549"} {:id "defn/converter-at-canvas-point", :kind "defn", :line 788, :end-line 795, :hash "-59067732"} {:id "defn/move-converter", :kind "defn", :line 797, :end-line 801, :hash "-1179681609"} {:id "defn/converter-value", :kind "defn", :line 803, :end-line 806, :hash "13277447"} {:id "defn/converter-count", :kind "defn", :line 808, :end-line 810, :hash "267660665"} {:id "defn/converters", :kind "defn", :line 812, :end-line 814, :hash "-1686748773"} {:id "defn/fixture-converter", :kind "defn", :line 816, :end-line 819, :hash "-475935667"} {:id "defn/arm-converter-placement", :kind "defn", :line 821, :end-line 823, :hash "-743641004"} {:id "defn/place-converter", :kind "defn", :line 825, :end-line 834, :hash "-89935502"} {:id "defn/converter-placement-disarmed?", :kind "defn", :line 836, :end-line 838, :hash "887795504"} {:id "defn/flow-midpoint", :kind "defn", :line 840, :end-line 847, :hash "1396828306"} {:id "def/clickable-kinds-by-mode", :kind "def", :line 849, :end-line 851, :hash "733363462"} {:id "defn/endpoint-clickable?", :kind "defn", :line 853, :end-line 855, :hash "1518566357"} {:id "defn-/connector-entry-by-name", :kind "defn-", :line 857, :end-line 859, :hash "971711688"} {:id "defn/connector-exists?", :kind "defn", :line 861, :end-line 863, :hash "314481541"} {:id "defn-/connector-attribute", :kind "defn-", :line 865, :end-line 868, :hash "709631432"} {:id "defn/connector-from", :kind "defn", :line 870, :end-line 872, :hash "524525571"} {:id "defn/connector-to", :kind "defn", :line 874, :end-line 876, :hash "799906750"} {:id "defn/connector-formula", :kind "defn", :line 878, :end-line 880, :hash "-1029787925"} {:id "defn/set-converter-name", :kind "defn", :line 882, :end-line 889, :hash "-847157151"} {:id "defn-/converter-to-flow-connector-id", :kind "defn-", :line 891, :end-line 898, :hash "-195957966"} {:id "defn-/converter-bound-stocks", :kind "defn-", :line 900, :end-line 906, :hash "1635543389"} {:id "defn-/formula-stock-value", :kind "defn-", :line 908, :end-line 911, :hash "1016779065"} {:id "defn-/refresh-converter", :kind "defn-", :line 913, :end-line 927, :hash "-246955903"} {:id "defn/refresh-converter-rates", :kind "defn", :line 929, :end-line 931, :hash "-347115180"} {:id "defn/set-converter-formula", :kind "defn", :line 933, :end-line 949, :hash "-2075209334"} {:id "defn/converter-connector-formula", :kind "defn", :line 951, :end-line 954, :hash "1867041656"} {:id "defn/apply-converter-edit", :kind "defn", :line 956, :end-line 967, :hash "-1686629901"} {:id "defn-/fixture-connector-endpoints", :kind "defn-", :line 969, :end-line 979, :hash "-179157893"} {:id "defn/fixture-connector", :kind "defn", :line 981, :end-line 984, :hash "-1936572057"} {:id "defn/fixture-stock-connector", :kind "defn", :line 986, :end-line 989, :hash "1964218954"} {:id "defn/connector-count", :kind "defn", :line 991, :end-line 993, :hash "-1826970045"} {:id "defn/connectors", :kind "defn", :line 995, :end-line 997, :hash "-1048990130"} {:id "defn-/valid-connector-pair?", :kind "defn-", :line 999, :end-line 1004, :hash "1385416327"} {:id "defn-/create-connector!", :kind "defn-", :line 1006, :end-line 1015, :hash "364741780"} {:id "defn/arm-connector-placement", :kind "defn", :line 1017, :end-line 1019, :hash "-1175395888"} {:id "defn/connector-placement-armed?", :kind "defn", :line 1021, :end-line 1023, :hash "-675985363"} {:id "defn/select-connector-origin", :kind "defn", :line 1025, :end-line 1033, :hash "1914302439"} {:id "defn-/try-connect-connector", :kind "defn-", :line 1035, :end-line 1047, :hash "1616656565"} {:id "defn/connect-connector", :kind "defn", :line 1049, :end-line 1054, :hash "1621379443"} {:id "defn/connector-placement-disarmed?", :kind "defn", :line 1056, :end-line 1058, :hash "-1401416734"} {:id "defn-/object-ref", :kind "defn-", :line 1060, :end-line 1062, :hash "256234122"} {:id "defn/selected?", :kind "defn", :line 1064, :end-line 1066, :hash "780210026"} {:id "defn/selection-count", :kind "defn", :line 1068, :end-line 1070, :hash "102084099"} {:id "defn/nothing-selected?", :kind "defn", :line 1072, :end-line 1074, :hash "1570407953"} {:id "defn-/normalize-rect", :kind "defn-", :line 1076, :end-line 1078, :hash "-886761888"} {:id "defn-/rects-intersect?", :kind "defn-", :line 1080, :end-line 1082, :hash "465071508"} {:id "defn-/point-in-rect?", :kind "defn-", :line 1084, :end-line 1087, :hash "658438042"} {:id "defn-/circle-intersects-rect?", :kind "defn-", :line 1089, :end-line 1095, :hash "384019027"} {:id "defn-/stock-bounds", :kind "defn-", :line 1097, :end-line 1099, :hash "1578532971"} {:id "defn-/converter-bounds", :kind "defn-", :line 1101, :end-line 1103, :hash "1578934291"} {:id "defn-/cloud-bounds", :kind "defn-", :line 1105, :end-line 1107, :hash "1755981880"} {:id "defn-/link-bounds", :kind "defn-", :line 1109, :end-line 1116, :hash "545244540"} {:id "defn-/point-segment-distance", :kind "defn-", :line 1118, :end-line 1134, :hash "-1837605759"} {:id "def/canvas-center", :kind "def", :line 1136, :end-line 1136, :hash "-823088025"} {:id "defn-/connector-default-control-offset", :kind "defn-", :line 1138, :end-line 1152, :hash "107222231"} {:id "defn-/connector-control-point", :kind "defn-", :line 1154, :end-line 1159, :hash "258390474"} {:id "defn-/quadratic-point", :kind "defn-", :line 1161, :end-line 1172, :hash "-1382276052"} {:id "defn-/connector-curve-midpoint", :kind "defn-", :line 1174, :end-line 1176, :hash "-668851218"} {:id "defn-/endpoint-center", :kind "defn-", :line 1178, :end-line 1184, :hash "-652861683"} {:id "defn-/ellipse-boundary-point", :kind "defn-", :line 1186, :end-line 1194, :hash "-1806779819"} {:id "defn-/rectangle-boundary-point", :kind "defn-", :line 1196, :end-line 1204, :hash "878119706"} {:id "defn-/endpoint-boundary-point", :kind "defn-", :line 1206, :end-line 1214, :hash "55404980"} {:id "defn-/visible-link-endpoints", :kind "defn-", :line 1216, :end-line 1221, :hash "-1884022774"} {:id "defn-/point-near-link?", :kind "defn-", :line 1223, :end-line 1229, :hash "-407336029"} {:id "defn-/connector-curve-points", :kind "defn-", :line 1231, :end-line 1241, :hash "592816946"} {:id "defn-/point-near-connector-handle?", :kind "defn-", :line 1243, :end-line 1248, :hash "-185775857"} {:id "defn/object-at-canvas-point", :kind "defn", :line 1250, :end-line 1272, :hash "-1991665536"} {:id "defn/connector-handle-position", :kind "defn", :line 1274, :end-line 1277, :hash "-1533459181"} {:id "defn/move-connector-handle", :kind "defn", :line 1279, :end-line 1290, :hash "-1062521520"} {:id "defn-/selectable-objects-with-bounds", :kind "defn-", :line 1292, :end-line 1309, :hash "5577208"} {:id "defn-/selection-update-allowed?", :kind "defn-", :line 1311, :end-line 1314, :hash "-1752512220"} {:id "defn-/update-selection-when-allowed", :kind "defn-", :line 1316, :end-line 1320, :hash "491192258"} {:id "defn/click-select", :kind "defn", :line 1322, :end-line 1334, :hash "583368951"} {:id "defn/click-select-at", :kind "defn", :line 1336, :end-line 1340, :hash "1167079823"} {:id "defn/shift-click-select", :kind "defn", :line 1342, :end-line 1355, :hash "-2113900096"} {:id "defn/shift-click-select-at", :kind "defn", :line 1357, :end-line 1361, :hash "959922948"} {:id "defn/marquee-select", :kind "defn", :line 1363, :end-line 1375, :hash "-629623796"} {:id "defn/clear-selection", :kind "defn", :line 1377, :end-line 1379, :hash "1454538573"} {:id "defn-/links-referencing-ref", :kind "defn-", :line 1381, :end-line 1387, :hash "864942845"} {:id "defn-/cascade-additions-for-ref", :kind "defn-", :line 1389, :end-line 1398, :hash "1156344263"} {:id "defn-/expand-delete-set", :kind "defn-", :line 1400, :end-line 1407, :hash "-1194035989"} {:id "defn-/remove-object-ref", :kind "defn-", :line 1409, :end-line 1430, :hash "1823015851"} {:id "defn/delete-selection", :kind "defn", :line 1432, :end-line 1440, :hash "1915710064"}]}
;; clj-mutate-manifest-end
