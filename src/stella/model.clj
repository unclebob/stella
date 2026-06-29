(ns stella.model
  (:require [stella.model.selection :as selection]
            [stella.model.shell :as shell]))

(defn default-diagram []
  (shell/default-diagram))

(defn default-shell []
  (shell/default-shell))

(defn top-level-menus
  [shell]
  (shell/top-level-menus shell))

(defn menu-includes?
  [shell menu-label]
  (shell/menu-includes? shell menu-label))

(defn menu-item-disabled?
  [shell item-label]
  (shell/menu-item-disabled? shell item-label))

(defn window-title
  [shell]
  (shell/window-title shell))

(defn showing?
  [shell]
  (shell/showing? shell))

(defn about-visible?
  [shell]
  (shell/about-visible? shell))

(defn about-text
  [shell]
  (shell/about-text shell))

(defn diagram-empty?
  [shell]
  (shell/diagram-empty? shell))

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

(defn- object-at-canvas-point
  [objects width height cx cy]
  (some (fn [{:keys [name x y]}]
          (when (and (<= x cx (+ x width))
                     (<= y cy (+ y height)))
            name))
        objects))

(defn stock-at-canvas-point
  "Returns the stock name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (let [stocks (vals (:stocks diagram))]
    (object-at-canvas-point stocks stock-icon-width stock-icon-height cx cy)))

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

(defn- changed-and-rejected?
  [requested prior current]
  (and (not= (str requested) (str prior))
       (= current prior)))

(defn- apply-stock-min-edit
  [diagram stock-name min-value]
  (let [prior (stock-min-value diagram stock-name)
        edited (set-stock-min diagram stock-name min-value)]
    (if (changed-and-rejected? min-value prior (stock-min-value edited stock-name))
      nil
      edited)))

(defn- apply-stock-max-edit
  [diagram stock-name max-value]
  (let [prior (stock-max-value diagram stock-name)
        edited (if (seq (str max-value))
                 (set-stock-max diagram stock-name max-value)
                 (clear-stock-max diagram stock-name))]
    (if (and (seq (str max-value))
             (changed-and-rejected? max-value prior (stock-max-value edited stock-name)))
      nil
      edited)))

(defn- apply-stock-initial-edit
  [diagram stock-name initial-value]
  (let [prior (stock-initial-value diagram stock-name)
        edited (set-stock-initial-value diagram stock-name initial-value)]
    (if (changed-and-rejected? initial-value prior (stock-initial-value edited stock-name))
      nil
      edited)))

(defn- apply-stock-name-edit
  [diagram stock-name name]
  (let [edited (if (= name stock-name) diagram (set-stock-name diagram stock-name name))]
    (when-not (and (not= name stock-name) (= edited diagram))
      [edited (if (= edited diagram) stock-name name)])))

(defn apply-stock-edit
  [diagram stock-name {:keys [name initial-value min-value max-value]}]
  (if-let [[after-name target] (apply-stock-name-edit diagram stock-name name)]
    (if-let [after-min (apply-stock-min-edit after-name target min-value)]
      (if-let [after-max (apply-stock-max-edit after-min target max-value)]
        (or (apply-stock-initial-edit after-max target initial-value) after-max)
        after-min)
      after-name)
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

(def converter-icon-size 50)

(defn converter-at-canvas-point
  "Returns the converter name whose icon contains canvas-local point [x y], or nil."
  [diagram cx cy]
  (object-at-canvas-point (vals (:converters diagram))
                          converter-icon-size
                          converter-icon-size
                          cx
                          cy))

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

(defn- apply-converter-formula-edit
  [diagram converter-name formula]
  (let [prior (or (converter-connector-formula diagram converter-name) "")]
    (if (= (str formula) (str prior))
      diagram
      (set-converter-formula diagram converter-name formula))))

(defn apply-converter-edit
  [diagram converter-name {:keys [name formula]}]
  (let [after-name (if (= name converter-name)
                     diagram
                     (set-converter-name diagram converter-name name))]
    (if (and (not= name converter-name) (= after-name diagram))
      diagram
      (let [target (if (= after-name diagram) converter-name name)]
        (apply-converter-formula-edit after-name target formula)))))

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

(defn selected?
  [diagram kind name]
  (selection/selected? diagram kind name))

(defn selection-count
  [diagram]
  (selection/selection-count diagram))

(defn nothing-selected?
  [diagram]
  (selection/nothing-selected? diagram))

(defn click-select
  [diagram kind name]
  (selection/click-select diagram kind name))

(defn shift-click-select
  [diagram kind name]
  (selection/shift-click-select diagram kind name))

(defn marquee-select
  [diagram x1 y1 x2 y2]
  (selection/marquee-select diagram x1 y1 x2 y2))

(defn clear-selection
  [diagram]
  (selection/clear-selection diagram))

(defn delete-selection
  [diagram]
  (selection/delete-selection diagram))
