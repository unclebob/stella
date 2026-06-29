(ns stella.model.selection)

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

(defn- entry-by-name
  [diagram collection name]
  (first (filter #(= name (:name (val %))) (get diagram collection))))

(defn- endpoint-exists?
  [diagram kind name]
  (case kind
    :stock (some? (entry-by-name diagram :stocks name))
    :source (some? (entry-by-name diagram :sources name))
    :sink (some? (entry-by-name diagram :sinks name))
    :converter (some? (entry-by-name diagram :converters name))
    :flow (some? (entry-by-name diagram :flows name))
    :connector (some? (entry-by-name diagram :connectors name))
    false))

(defn- entry-position
  [diagram collection name]
  (when-let [[_ entry] (entry-by-name diagram collection name)]
    [(:x entry) (:y entry)]))

(defn- endpoint-anchor
  [[x y] kind side]
  (let [[dx dy] (get {[:stock :right] [80.0 25.0]
                      [:stock :left] [0.0 25.0]
                      [:source :right] [80.0 25.0]
                      [:sink :left] [0.0 25.0]
                      [:converter :right] [50.0 25.0]
                      [:converter :left] [0.0 25.0]
                      [:flow :right] [10.0 0.0]
                      [:flow :left] [-10.0 0.0]}
                     [kind side]
                     [0.0 25.0])]
    [(+ x dx) (+ y dy)]))

(declare endpoint-position)

(defn- flow-midpoint
  [diagram name]
  (when-let [[_ flow] (entry-by-name diagram :flows name)]
    (when-let [from-pos (endpoint-position diagram (:from flow))]
      (when-let [to-pos (endpoint-position diagram (:to flow))]
        (let [[fx fy] (endpoint-anchor from-pos (:kind (:from flow)) :right)
              [tx ty] (endpoint-anchor to-pos (:kind (:to flow)) :left)]
          [(/ (+ fx tx) 2.0) (/ (+ fy ty) 2.0)])))))

(defn- endpoint-position
  [diagram {:keys [kind id]}]
  (case kind
    :stock (entry-position diagram :stocks id)
    :source (entry-position diagram :sources id)
    :sink (entry-position diagram :sinks id)
    :converter (entry-position diagram :converters id)
    :flow (flow-midpoint diagram id)
    nil))

(defn- placement-disarmed?
  [diagram]
  (= :idle (:placement-mode diagram)))

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

(defn- cascade-refs
  [diagram ref collections]
  (into #{} (mapcat #(links-referencing-ref diagram % ref)) collections))

(def ^:private cascade-collections-by-kind
  {:stock [:flows :connectors]
   :source [:flows]
   :sink [:flows]
   :flow [:connectors]
   :converter [:connectors]
   :connector []})

(defn- cascade-additions-for-ref
  [diagram ref]
  (cascade-refs diagram ref (get cascade-collections-by-kind (:kind ref) [])))

(defn- expand-delete-set
  [diagram refs]
  (loop [current refs]
    (let [additions (into #{} (mapcat #(cascade-additions-for-ref diagram %) current))
          expanded (into current additions)]
      (if (= expanded current)
        expanded
        (recur expanded)))))

(def ^:private removal-collections-by-kind
  {:stock :stocks
   :flow :flows
   :converter :converters
   :connector :connectors
   :source :sources
   :sink :sinks})

(defn- remove-object-ref
  [diagram {:keys [kind id]}]
  (if-let [collection (get removal-collections-by-kind kind)]
    (if-let [[object-id _] (entry-by-name diagram collection id)]
      (update diagram collection dissoc object-id)
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
