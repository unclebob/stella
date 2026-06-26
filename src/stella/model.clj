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
  (some #(when (and (= (:label %) item-label) (not (:separator %)))
           (:disabled %))
        (menu-items shell)))

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

(defn stock-initial-value
  [diagram name]
  (when-let [[_ stock] (stock-entry-by-name diagram name)]
    (:initial-value stock)))

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
          stock {:name name :initial-value "0" :x x :y y}]
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

(defn fixture-stock
  [diagram name x y]
  (let [num (num-from-name "Stock" name)
        id (keyword (str "stock-" num))]
    (-> diagram
        (assoc-in [:stocks id] {:name name :initial-value "0" :x x :y y})
        (update :next-stock-num #(max % (inc num))))))

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
  (let [num (num-from-name "Source" name)
        id (keyword (str "source-" num))]
    (-> diagram
        (assoc-in [:sources id] {:name name :x x :y y})
        (update :next-source-num #(max % (inc num))))))

(defn fixture-sink
  [diagram name x y]
  (let [num (num-from-name "Sink" name)
        id (keyword (str "sink-" num))]
    (-> diagram
        (assoc-in [:sinks id] {:name name :x x :y y})
        (update :next-sink-num #(max % (inc num))))))

(defn arm-source-placement
  [diagram]
  (assoc diagram :placement-mode :source))

(defn arm-sink-placement
  [diagram]
  (assoc diagram :placement-mode :sink))

(defn place-source
  [diagram x y]
  (if (= :source (:placement-mode diagram))
    (let [num (:next-source-num diagram)
          name (str "Source" num)
          id (keyword (str "source-" num))]
      (-> diagram
          (assoc-in [:sources id] {:name name :x x :y y})
          (assoc :placement-mode :idle)
          (update :next-source-num inc)))
    diagram))

(defn place-sink
  [diagram x y]
  (if (= :sink (:placement-mode diagram))
    (let [num (:next-sink-num diagram)
          name (str "Sink" num)
          id (keyword (str "sink-" num))]
      (-> diagram
          (assoc-in [:sinks id] {:name name :x x :y y})
          (assoc :placement-mode :idle)
          (update :next-sink-num inc)))
    diagram))

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

(defn flow-from
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (:from flow)))

(defn flow-to
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (:to flow)))

(defn flow-endpoints
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    [(:id (:from flow)) (:id (:to flow))]))

(defn flow-rate
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (:rate flow)))

(defn flow-count
  [diagram]
  (count (:flows diagram)))

(defn flows
  [diagram]
  (vals (:flows diagram)))

(defn fixture-flow
  [diagram flow-name from-stock to-stock]
  (let [num (num-from-name "Flow" flow-name)
        id (keyword (str "flow-" num))]
    (-> diagram
        (assoc-in [:flows id] {:name flow-name
                               :from (endpoint-ref :stock from-stock)
                               :to (endpoint-ref :stock to-stock)
                               :rate "0"})
        (update :next-flow-num #(max % (inc num))))))

(defn- valid-flow-pair?
  [from to]
  (case [(:kind from) (:kind to)]
    [:source :stock] true
    [:stock :stock] (not= (:id from) (:id to))
    [:stock :sink] true
    false))

(defn- create-flow!
  [diagram from to]
  (let [num (:next-flow-num diagram)
        name (str "Flow" num)
        id (keyword (str "flow-" num))]
    (-> diagram
        (assoc-in [:flows id] {:name name :from from :to to :rate "0"})
        (assoc :placement-mode :idle :flow-draft nil)
        (update :next-flow-num inc))))

(defn arm-flow-placement
  [diagram]
  (-> diagram
      (assoc :placement-mode :flow)
      (assoc :flow-draft nil)))

(defn flow-placement-armed?
  [diagram]
  (= :flow (:placement-mode diagram)))

(defn select-flow-source
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram))
           (nil? (:flow-draft diagram))
           (not= kind :sink))
    (cond
      (and (= kind :source) (source-exists? diagram name))
      (assoc diagram :flow-draft {:from (endpoint-ref :source name)})

      (and (= kind :stock) (stock-exists? diagram name))
      (assoc diagram :flow-draft {:from (endpoint-ref :stock name)})

      :else diagram)
    diagram))

(defn connect-flow
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram))
           (:flow-draft diagram))
    (let [from (:from (:flow-draft diagram))
          to (endpoint-ref kind name)]
      (cond
        (= kind :source)
        (assoc diagram :flow-draft nil)

        (and (= kind :stock) (stock-exists? diagram name)
             (valid-flow-pair? from to))
        (create-flow! diagram from to)

        (and (= kind :sink) (sink-exists? diagram name)
             (valid-flow-pair? from to))
        (create-flow! diagram from to)

        :else
        (assoc diagram :flow-draft nil)))
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
  (let [num (num-from-name "Converter" name)
        id (keyword (str "converter-" num))]
    (-> diagram
        (assoc-in [:converters id] {:name name :value "0" :x x :y y})
        (update :next-converter-num #(max % (inc num))))))

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

(declare flow-midpoint)

(defn- endpoint-anchor
  [pos kind side]
  (let [[x y] pos]
    (case [kind side]
      [:stock :right] [(+ x 80.0) (+ y 25.0)]
      [:stock :left] [x (+ y 25.0)]
      [:source :right] [(+ x 80.0) (+ y 25.0)]
      [:sink :left] [x (+ y 25.0)]
      [:converter :right] [(+ x 50.0) (+ y 25.0)]
      [:converter :left] [x (+ y 25.0)]
      [:flow :right] [(+ x 10.0) y]
      [:flow :left] [(- x 10.0) y]
      [x y])))

(defn- endpoint-position
  [diagram {:keys [kind id]}]
  (case kind
    :stock (stock-position diagram id)
    :source (source-position diagram id)
    :sink (sink-position diagram id)
    :converter (converter-position diagram id)
    :flow (flow-midpoint diagram id)
    nil))

(defn flow-midpoint
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (when-let [from-pos (endpoint-position diagram (:from flow))]
      (when-let [to-pos (endpoint-position diagram (:to flow))]
        (let [[fx fy] (endpoint-anchor from-pos (:kind (:from flow)) :right)
              [tx ty] (endpoint-anchor to-pos (:kind (:to flow)) :left)]
          [(/ (+ fx tx) 2.0) (/ (+ fy ty) 2.0)])))))

(defn- connector-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:connectors diagram))))

(defn connector-exists?
  [diagram name]
  (some? (connector-entry-by-name diagram name)))

(defn connector-from
  [diagram name]
  (when-let [[_ connector] (connector-entry-by-name diagram name)]
    (:from connector)))

(defn connector-to
  [diagram name]
  (when-let [[_ connector] (connector-entry-by-name diagram name)]
    (:to connector)))

(defn connector-count
  [diagram]
  (count (:connectors diagram)))

(defn connectors
  [diagram]
  (vals (:connectors diagram)))

(defn- endpoint-exists?
  [diagram kind name]
  (case kind
    :stock (stock-exists? diagram name)
    :converter (converter-exists? diagram name)
    :flow (flow-exists? diagram name)
    :source (source-exists? diagram name)
    :sink (sink-exists? diagram name)
    false))

(defn- valid-connector-pair?
  [from to]
  (case [(:kind from) (:kind to)]
    [:converter :flow] true
    [:stock :converter] true
    false))

(defn- create-connector!
  [diagram from to]
  (let [num (:next-connector-num diagram)
        name (str "Connector" num)
        id (keyword (str "connector-" num))]
    (-> diagram
        (assoc-in [:connectors id] {:name name :from from :to to})
        (assoc :placement-mode :idle :connector-draft nil)
        (update :next-connector-num inc))))

(defn arm-connector-placement
  [diagram]
  (-> diagram
      (assoc :placement-mode :connector)
      (assoc :connector-draft nil)))

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

(defn connect-connector
  [diagram kind name]
  (if (and (= :connector (:placement-mode diagram))
           (:connector-draft diagram))
    (let [from (:from (:connector-draft diagram))
          to (endpoint-ref kind name)]
      (cond
        (contains? #{:source :sink :stock} kind)
        (assoc diagram :connector-draft nil)

        (and (endpoint-exists? diagram kind name)
             (valid-connector-pair? from to))
        (create-connector! diagram from to)

        :else
        (assoc diagram :connector-draft nil)))
    diagram))

(defn connector-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))