(ns stella.ui.canvas
  (:require [clojure.string :as str]
            [stella.events :as events]
            [stella.model :as model]
            [stella.simulation :as simulation]
            [stella.thermometer :as thermometer]))

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(defn- selection-click
  [kind name]
  {:event events/selection-click :object-kind kind :object-name name})

(def ^:private selection-outline-style
  "-fx-fill: transparent; -fx-stroke: #888; -fx-stroke-width: 2;")

(defn- with-rect-selection-outline
  [diagram kind name width height children]
  (if (model/selected? diagram kind name)
    (into [{:fx/type :rectangle
            :width (+ width 4)
            :height (+ height 4)
            :layout-x -2
            :layout-y -2
            :mouse-transparent true
            :style selection-outline-style}]
          children)
    children))

(defn- with-ellipse-selection-outline
  [diagram kind name center-x center-y radius-x radius-y children]
  (if (model/selected? diagram kind name)
    (into [{:fx/type :ellipse
            :center-x center-x
            :center-y center-y
            :radius-x (+ radius-x 2)
            :radius-y (+ radius-y 2)
            :mouse-transparent true
            :style selection-outline-style}]
          children)
    children))

(def flow-pipe-stroke-width 8)
(def connector-stroke-width 1)
(def ^:private connector-arrow-size 8)
(def ^:private flow-arrow-size 14)
(def ^:private flow-arrow-wing 10)
(def ^:private flow-pipe-fill "#eef4f7")
(def ^:private flow-boundary-radius (* flow-pipe-stroke-width 1.5))
(def ^:private flow-label-width 120)
(def ^:private flow-label-height 18)
(def ^:private flow-label-gap 6)
(def ^:private preview-opacity 0.55)
(def ^:private canvas-center [2000.0 2000.0])
(def ^:private connector-control-radius 3)
(def ^:private connector-control-hit-radius 10)

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
      :flow (ellipse-boundary-point center target flow-boundary-radius flow-boundary-radius)
      center)))

(defn- clipped-link-endpoints
  [from-pos from-kind to-pos to-kind]
  (let [from-center (endpoint-center from-pos from-kind)
        to-center (endpoint-center to-pos to-kind)]
    [(endpoint-boundary-point from-pos from-kind to-center)
     (endpoint-boundary-point to-pos to-kind from-center)]))

(defn- unit-vector
  [from-x from-y to-x to-y]
  (let [dx (- to-x from-x)
        dy (- to-y from-y)
        length (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (zero? length)
      [0.0 0.0]
      [(/ dx length) (/ dy length)])))

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

(defn- with-stroke-dash
  [shape stroke-dash-array]
  (cond-> shape
    stroke-dash-array (assoc :stroke-dash-array stroke-dash-array)))

(defn- connector-curve
  [start-x start-y control-x control-y end-x end-y stroke stroke-width opacity stroke-dash-array]
  (with-stroke-dash
    {:fx/type :quad-curve
     :start-x start-x
     :start-y start-y
     :control-x control-x
     :control-y control-y
     :end-x end-x
     :end-y end-y
     :fill "transparent"
     :stroke stroke
     :stroke-width stroke-width
     :stroke-line-cap :round
     :opacity opacity}
    stroke-dash-array))

(defn- connector-control-drag-events
  [connector-name]
  {:on-mouse-pressed {:event events/connector-control-drag-start
                      :connector-name connector-name}
   :on-mouse-dragged {:event events/connector-control-drag
                      :connector-name connector-name}
   :on-mouse-released {:event events/connector-control-drag-end
                       :connector-name connector-name}})

(defn- connector-handle-desc
  [connector-name marker-x marker-y]
  (if connector-name
    (merge {:fx/type :group
            :fx/key (str "connector-handle-" connector-name)
            :layout-x marker-x
            :layout-y marker-y
            :children [{:fx/type :circle
                        :center-x 0
                        :center-y 0
                        :radius connector-control-hit-radius
                        :fill "transparent"
                        :stroke "transparent"}
                       {:fx/type :circle
                        :center-x 0
                        :center-y 0
                        :radius connector-control-radius
                        :fill "#000"
                        :mouse-transparent true}]
            :on-mouse-clicked (selection-click :connector connector-name)}
           (connector-control-drag-events connector-name))
    {:fx/type :circle
     :center-x marker-x
     :center-y marker-y
     :radius connector-control-radius
     :fill "#000"}))

(defn- connector-arrowhead
  ([end-x end-y ux uy]
   (connector-arrowhead end-x end-y ux uy "#666" connector-stroke-width 1.0 nil))
  ([end-x end-y ux uy stroke stroke-width opacity stroke-dash-array]
  (let [px (- uy)
        py ux
        base-x (- end-x (* ux connector-arrow-size))
        base-y (- end-y (* uy connector-arrow-size))
        wing (/ connector-arrow-size 2.0)]
    [(with-stroke-dash
       {:fx/type :line
        :start-x end-x
        :start-y end-y
        :end-x (+ base-x (* px wing))
        :end-y (+ base-y (* py wing))
        :stroke stroke
        :stroke-width stroke-width
        :opacity opacity}
       stroke-dash-array)
     (with-stroke-dash
       {:fx/type :line
        :start-x end-x
        :start-y end-y
        :end-x (- base-x (* px wing))
        :end-y (- base-y (* py wing))
        :stroke stroke
        :stroke-width stroke-width
        :opacity opacity}
       stroke-dash-array)])))

(defn- flow-arrowhead
  [tip-x tip-y base-x base-y]
  (let [[ux uy] (unit-vector base-x base-y tip-x tip-y)
        px (- uy)
        py ux
        wing flow-arrow-wing]
    {:fx/type :polygon
     :points [tip-x tip-y
              (+ base-x (* px wing)) (+ base-y (* py wing))
              (- base-x (* px wing)) (- base-y (* py wing))]
     :fill flow-pipe-fill
     :stroke "#555"
     :stroke-width 1}))

(defn- flow-pipe-body
  [selected? start-x start-y end-x end-y]
  (let [[ux uy] (unit-vector start-x start-y end-x end-y)
        base-x (- end-x (* ux flow-arrow-size))
        base-y (- end-y (* uy flow-arrow-size))]
    (filterv map?
             [(when selected?
                {:fx/type :line
                 :start-x start-x
                 :start-y start-y
                 :end-x base-x
                 :end-y base-y
                 :stroke "#2f80ed"
                 :stroke-width 16
                 :stroke-line-cap :round
                 :opacity 0.35})
              {:fx/type :line
               :start-x start-x
               :start-y start-y
               :end-x base-x
               :end-y base-y
               :stroke "#555"
               :stroke-width 10
               :stroke-line-cap :round}
              {:fx/type :line
               :start-x start-x
               :start-y start-y
               :end-x base-x
               :end-y base-y
               :stroke flow-pipe-fill
               :stroke-width flow-pipe-stroke-width
               :stroke-line-cap :round}
              (flow-arrowhead end-x end-y base-x base-y)])))

(defn- flow-hit-line
  [start-x start-y end-x end-y]
  {:fx/type :line
   :start-x start-x
   :start-y start-y
   :end-x end-x
   :end-y end-y
   :stroke "transparent"
   :stroke-width 10
   :stroke-line-cap :round})

(defn- flow-midpoint-circle
  [mid-x mid-y]
  {:fx/type :circle
   :center-x mid-x
   :center-y mid-y
   :radius flow-boundary-radius
   :fill "white"
   :stroke "#555"
   :stroke-width 1
   :mouse-transparent true})

(defn- flow-label
  [text mid-x layout-y]
  {:fx/type :label
   :layout-x (- mid-x (/ flow-label-width 2.0))
   :layout-y layout-y
   :pref-width flow-label-width
   :alignment :center
   :text-alignment :center
   :mouse-transparent true
   :text text})

(defn- flow-labels
  [name rate mid-x mid-y]
  [(flow-label name
               mid-x
               (- mid-y flow-boundary-radius flow-label-gap flow-label-height))
   (flow-label (str rate)
               mid-x
               (+ mid-y flow-boundary-radius flow-label-gap))])

(defn- flow-desc
  [diagram {:keys [name rate from to]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[[start-x start-y] [end-x end-y]]
            (clipped-link-endpoints from-pos (:kind from) to-pos (:kind to))
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)
            pipe (flow-pipe-body (model/selected? diagram :flow name)
                                 start-x start-y end-x end-y)]
        (cond-> {:fx/type :group
                 :fx/key (str "flow-" name)
                 :id (str "flow-" name)
                 :children (into (conj pipe
                                       (flow-hit-line start-x start-y end-x end-y)
                                       (flow-midpoint-circle mid-x mid-y))
                                 (flow-labels name rate mid-x mid-y))}
          (model/endpoint-clickable? diagram :flow)
          (assoc :on-mouse-clicked (endpoint-click :flow name))
          (= :idle (:placement-mode diagram))
          (assoc :on-mouse-clicked (selection-click :flow name))
          :always
          (assoc :on-context-menu-requested
                 {:event events/edit-flow-open :flow-name name}))))))

(defn- connector-stroke-dash-array
  [from to]
  (when (or (= :stock (:kind from))
            (= :stock (:kind to)))
    [6 4]))

(defn- connector-body
  [selected? connector-name control-offset from to to-pos start-x start-y end-x end-y]
  (let [[control-x control-y] (connector-control-point start-x start-y end-x end-y control-offset)
        [marker-x marker-y] (connector-curve-midpoint start-x start-y control-x control-y end-x end-y)
        [ux uy] (unit-vector control-x control-y end-x end-y)
        [arrow-tip-x arrow-tip-y] (if to-pos
                                    (endpoint-boundary-point to-pos (:kind to) [control-x control-y])
                                    [end-x end-y])
        stroke-dash-array (connector-stroke-dash-array from to)]
    (into
     (filterv map?
              [(when selected?
                 (connector-curve start-x start-y control-x control-y end-x end-y
                                  "#2f80ed" 8 0.35 stroke-dash-array))
               (connector-curve start-x start-y control-x control-y end-x end-y
                                "#666" connector-stroke-width 1.0 stroke-dash-array)])
     (concat
      [(connector-handle-desc connector-name marker-x marker-y)]
      (when selected?
        (connector-arrowhead arrow-tip-x arrow-tip-y ux uy "#2f80ed" 5 0.35 stroke-dash-array))
      (connector-arrowhead arrow-tip-x arrow-tip-y ux uy "#666" connector-stroke-width 1.0 stroke-dash-array)))))

(defn connector-canvas-labels
  [diagram connector-name]
  (when-let [connector (first (filter #(= connector-name (:name %))
                                      (model/connectors diagram)))]
    (when (and (model/endpoint-position diagram (:from connector))
               (model/endpoint-position diagram (:to connector)))
      (cond-> {:name (:name connector)}
        (seq (:formula connector)) (assoc :formula (:formula connector))))))

(defn- connector-desc
  [diagram {:keys [name from to control-offset]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (endpoint-center from-pos (:kind from))
            [end-x end-y] (endpoint-center to-pos (:kind to))
            body (connector-body (model/selected? diagram :connector name)
                                 name
                                 control-offset
                                 from
                                 to
                                 to-pos
                                 start-x start-y end-x end-y)
            children body]
        {:fx/type :group
         :fx/key (str "connector-" name)
         :id (str "connector-" name)
         :children children}))))

(defn converter-canvas-labels
  [diagram converter-name]
  (when-let [{:keys [name]} (first (filter #(= converter-name (:name %))
                                            (model/converters diagram)))]
    {:name name}))

(defn converter-canvas-position
  [diagram converter-name]
  (model/converter-position diagram converter-name))

(def ^:private converter-label-width 100)
(def ^:private converter-label-gap 6)
(def ^:private converter-label-height 18)

(def ^:private bound-label-style "-fx-font-size: 9px;")
(def ^:private stock-name-y 4)
(def ^:private stock-bound-row-y 36)
(def ^:private stock-bound-side-width 24)
(def ^:private stock-bound-center-width 32)
(def ^:private stock-bound-side-x 4)
(def ^:private stock-bound-center-x 24)
(def ^:private stock-bound-max-x 52)
(def ^:private thermometer-fill-style "-fx-fill: #add8e6;")
(def ^:private thermometer-track-style "-fx-fill: white; -fx-stroke: #ccc; -fx-stroke-width: 1;")

(defn stock-canvas-thermometer
  [diagram stock-name]
  (thermometer/stock-thermometer diagram stock-name))

(defn- thermometer-rectangle
  [therm width style & {:keys [id mouse-transparent?]}]
  (cond-> {:fx/type :rectangle
           :width width
           :height (:track-height therm)
           :layout-x (:track-x therm)
           :layout-y (:thermometer-y therm)
           :style style}
    id (assoc :fx/key id :id id)
    mouse-transparent? (assoc :mouse-transparent true)))

(defn- thermometer-nodes
  [diagram stock-name]
  (when-let [therm (thermometer/stock-thermometer diagram stock-name)]
    (let [fill-width (:fill-width therm)
          fill-id (str "stock-thermometer-fill-" stock-name)]
      (into [(thermometer-rectangle therm (:track-width therm) thermometer-track-style)]
            (when (pos? fill-width)
              [(thermometer-rectangle therm fill-width thermometer-fill-style
                                     :id fill-id
                                     :mouse-transparent? true)])))))

(defn stock-icon-labels
  [diagram {:keys [name min-value max-value]}]
  {:name name
   :min (or min-value "0")
   :max max-value
   :value (simulation/stock-value diagram name)})

(defn stock-canvas-labels
  [diagram stock-name]
  (some->> (model/stocks diagram)
           (filter #(= stock-name (:name %)))
           first
           (stock-icon-labels diagram)))

(defn stock-canvas-position
  [diagram stock-name]
  (model/stock-position diagram stock-name))

(defn flow-icon-labels
  [{:keys [name rate]}]
  {:name name
   :rate (str rate)})

(defn- flow-on-canvas
  [diagram flow-name]
  (some->> (model/flows diagram)
          (filter #(= flow-name (:name %)))
          first
          (#(when (and (model/endpoint-position diagram (:from %))
                        (model/endpoint-position diagram (:to %)))
              %))))

(defn flow-canvas-labels
  [diagram flow-name]
  (some-> (flow-on-canvas diagram flow-name) flow-icon-labels))

(defn- stock-bound-row-labels
  [{:keys [min max value]}]
  (into [{:fx/type :label
          :layout-x stock-bound-side-x
          :layout-y stock-bound-row-y
          :pref-width stock-bound-side-width
          :alignment :center-left
          :text min
          :style bound-label-style}
         {:fx/type :label
          :layout-x stock-bound-center-x
          :layout-y stock-bound-row-y
          :pref-width stock-bound-center-width
          :alignment :center
          :text-alignment :center
          :text value
          :style bound-label-style}]
        (when max
          [{:fx/type :label
            :layout-x stock-bound-max-x
            :layout-y stock-bound-row-y
            :pref-width stock-bound-side-width
            :alignment :center-right
            :text max
            :style bound-label-style}])))

(defn- stock-desc
  [diagram {:keys [name x y] :as stock}]
  (let [labels (stock-icon-labels diagram stock)
        {:keys [name min max value]} labels
        body (into [{:fx/type :rectangle
                     :width 80
                     :height 50
                     :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                    {:fx/type :label
                     :layout-x 20
                     :layout-y thermometer/stock-name-y
                     :text name}]
                   (concat (thermometer-nodes diagram name)
                           (stock-bound-row-labels labels)))
        children (with-rect-selection-outline diagram :stock name 80 50 body)]
    (cond-> {:fx/type :group
             :fx/key (str "stock-" name)
             :id (str "stock-" name)
             :layout-x x
             :layout-y y
             :children (filterv map? children)}
      (model/endpoint-clickable? diagram :stock)
      (assoc :on-mouse-clicked (endpoint-click :stock name))
      (= :idle (:placement-mode diagram))
      (assoc :on-mouse-clicked (selection-click :stock name)
             :on-mouse-pressed {:event events/stock-drag-start :stock-name name}
             :on-mouse-dragged {:event events/stock-drag :stock-name name}
             :on-mouse-released {:event events/stock-drag-end :stock-name name})
      :always
      (assoc :on-context-menu-requested
             {:event events/edit-stock-open :stock-name name}))))

(defn- converter-desc
  [diagram {:keys [name x y]}]
  (let [body (with-ellipse-selection-outline
              diagram :converter name 25 25 25 25
              [{:fx/type :circle
                :center-x 25
                :center-y 25
                :radius 25
                :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
               {:fx/type :label
                :layout-x (- 25 (/ converter-label-width 2.0))
                :layout-y (+ 50 converter-label-gap)
                :pref-width converter-label-width
                :pref-height converter-label-height
                :alignment :center
                :text-alignment :center
                :text name}])]
    (cond-> {:fx/type :group
             :fx/key (str "converter-" name)
             :id (str "converter-" name)
             :layout-x x
             :layout-y y
             :children (filterv map? body)}
      (model/endpoint-clickable? diagram :converter)
      (assoc :on-mouse-clicked (endpoint-click :converter name))
      (= :idle (:placement-mode diagram))
      (assoc :on-mouse-clicked (selection-click :converter name)
             :on-mouse-pressed {:event events/converter-drag-start :converter-name name}
             :on-mouse-dragged {:event events/converter-drag :converter-name name}
             :on-mouse-released {:event events/converter-drag-end :converter-name name})
      :always
      (assoc :on-context-menu-requested
             {:event events/edit-converter-open :converter-name name}))))

(def ^:private cloud-shape-style
  "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;")

(defn- cloud-shape []
  [{:fx/type :circle
    :center-x 27
    :center-y 29
    :radius 18
    :style cloud-shape-style}
   {:fx/type :circle
    :center-x 43
    :center-y 20
    :radius 21
    :style cloud-shape-style}
   {:fx/type :circle
    :center-x 59
    :center-y 30
    :radius 16
    :style cloud-shape-style}
   {:fx/type :rectangle
    :x 25
    :y 28
    :width 38
    :height 18
    :style "-fx-fill: white; -fx-stroke: transparent;"}])

(defn- cloud-arrowhead
  [tip-x tip-y base-y]
  [{:fx/type :line
    :start-x tip-x
    :start-y tip-y
    :end-x (- tip-x 6)
    :end-y base-y
    :stroke "#333"
    :stroke-width 1}
   {:fx/type :line
    :start-x tip-x
    :start-y tip-y
    :end-x (+ tip-x 6)
    :end-y base-y
    :stroke "#333"
    :stroke-width 1}])

(defn- cloud-direction-arrow
  [kind]
  (case kind
    :source (into [{:fx/type :line
                    :start-x 40
                    :start-y 25
                    :end-x 40
                    :end-y -1
                    :stroke "#333"
                    :stroke-width 1}]
                  (cloud-arrowhead 40 -4 5))
    :sink (into [{:fx/type :line
                  :start-x 40
                  :start-y -4
                  :end-x 40
                  :end-y 22
                  :stroke "#333"
                  :stroke-width 1}]
                (cloud-arrowhead 40 25 16))))

(defn- directed-cloud-shape
  [kind]
  (into (cloud-shape) (cloud-direction-arrow kind)))

(defn- cloud-selection-highlight-circle
  [center-x center-y radius]
  {:fx/type :circle
   :center-x center-x
   :center-y center-y
   :radius (+ radius 2)
   :fill "transparent"
   :stroke "#2f80ed"
   :stroke-width 4
   :opacity 0.35
   :mouse-transparent true})

(defn- cloud-selection-highlight-line
  [{:keys [start-x start-y end-x end-y]}]
  {:fx/type :line
   :start-x start-x
   :start-y start-y
   :end-x end-x
   :end-y end-y
   :stroke "#2f80ed"
   :stroke-width 4
   :opacity 0.35
   :mouse-transparent true})

(defn- cloud-selection-highlight
  [kind]
  (into [(cloud-selection-highlight-circle 27 29 18)
         (cloud-selection-highlight-circle 43 20 21)
         (cloud-selection-highlight-circle 59 30 16)]
        (map cloud-selection-highlight-line (cloud-direction-arrow kind))))

(defn- with-cloud-selection-highlight
  [diagram kind name children]
  (if (model/selected? diagram kind name)
    (into (cloud-selection-highlight kind) children)
    children))

(defn- cloud-desc
  [diagram kind {:keys [name x y]}]
  (let [body (with-cloud-selection-highlight
              diagram kind name
              (directed-cloud-shape kind))]
    (cond-> {:fx/type :group
             :fx/key (str (clojure.core/name kind) "-" name)
             :id (str (clojure.core/name kind) "-" name)
             :layout-x x
             :layout-y y
             :children (filterv map? body)}
      (model/endpoint-clickable? diagram kind)
      (assoc :on-mouse-clicked (endpoint-click kind name))
      (= :idle (:placement-mode diagram))
      (assoc :on-mouse-clicked (selection-click kind name)
             :on-mouse-pressed {:event events/cloud-drag-start
                                :cloud-kind kind
                                :cloud-name name}
             :on-mouse-dragged {:event events/cloud-drag
                                :cloud-kind kind
                                :cloud-name name}
             :on-mouse-released {:event events/cloud-drag-end
                                 :cloud-kind kind
                                 :cloud-name name}))))

(defn- preview-stock-desc
  [x y]
  {:fx/type :group
   :fx/key "preview-stock"
   :id "preview-stock"
   :layout-x x
   :layout-y y
   :mouse-transparent true
   :opacity preview-opacity
   :children [{:fx/type :rectangle
               :width 80
               :height 50
               :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1; -fx-stroke-dash-array: 6 4;"}
              {:fx/type :label
               :layout-x 20
               :layout-y 14
               :text "Stock"}]})

(defn- preview-cloud-desc
  [kind x y]
  {:fx/type :group
   :fx/key (str "preview-" (clojure.core/name kind))
   :id (str "preview-" (clojure.core/name kind))
   :layout-x x
   :layout-y y
   :mouse-transparent true
   :opacity preview-opacity
   :children (directed-cloud-shape kind)})

(defn- preview-child
  [desc]
  (assoc desc :mouse-transparent true))

(defn- preview-converter-desc
  [x y]
  {:fx/type :group
   :fx/key "preview-converter"
   :id "preview-converter"
   :layout-x x
   :layout-y y
   :mouse-transparent true
   :opacity preview-opacity
   :children [(preview-child
               {:fx/type :circle
                :center-x 25
                :center-y 25
                :radius 25
                :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1; -fx-stroke-dash-array: 6 4;"})
              (preview-child
               {:fx/type :label
                :layout-x (- 25 (/ converter-label-width 2.0))
                :layout-y (+ 50 converter-label-gap)
                :pref-width converter-label-width
                :pref-height converter-label-height
                :alignment :center
                :text-alignment :center
                :text "Converter"})]})

(defn- placement-preview-desc
  [diagram {:keys [x y]}]
  (case (:placement-mode diagram)
    :stock (preview-stock-desc x y)
    :source (preview-cloud-desc :source x y)
    :sink (preview-cloud-desc :sink x y)
    :converter (preview-converter-desc x y)
    nil))

(defn- draft-flow-desc
  [diagram {:keys [x y]}]
  (when-let [from (get-in diagram [:flow-draft :from])]
    (when-let [from-pos (model/endpoint-position diagram from)]
      (let [[[start-x start-y] [end-x end-y]]
            (clipped-link-endpoints from-pos (:kind from) [x y] :point)]
        {:fx/type :group
         :fx/key "preview-flow"
         :id "preview-flow"
         :mouse-transparent true
         :opacity preview-opacity
         :children (flow-pipe-body false start-x start-y end-x end-y)}))))

(defn- draft-connector-desc
  [diagram {:keys [x y]}]
  (when-let [from (get-in diagram [:connector-draft :from])]
    (when-let [from-pos (model/endpoint-position diagram from)]
      (let [[[start-x start-y] [end-x end-y]]
            (clipped-link-endpoints from-pos (:kind from) [x y] :point)]
        {:fx/type :group
         :fx/key "preview-connector"
         :id "preview-connector"
         :mouse-transparent true
         :opacity preview-opacity
         :children (connector-body false nil nil nil {:kind :flow} nil
                                   start-x start-y end-x end-y)}))))

(defn- canvas-preview-nodes
  [diagram preview]
  (when preview
    (filterv map?
             [(placement-preview-desc diagram preview)
              (draft-flow-desc diagram preview)
              (draft-connector-desc diagram preview)])))

(defn- overlay-segment
  [items]
  (when (seq items) (str/join " | " items)))

(defn diagram-overlay-text
  [diagram]
  (->> [(overlay-segment (map (fn [{:keys [name min-value max-value]}]
                                 (str name " "
                                      (or min-value "0")
                                      (when max-value (str " " max-value))))
                               (model/stocks diagram)))
        (overlay-segment (map (fn [{:keys [name rate]}] (str name " " rate))
                              (model/flows diagram)))
        (overlay-segment (map :name (model/sources diagram)))
        (overlay-segment (map :name (model/sinks diagram)))
        (overlay-segment (map :name (model/converters diagram)))
        (overlay-segment (map :name (model/connectors diagram)))]
       (remove nil?)
       (str/join " || ")))

(defn- canvas-background []
  {:fx/type :rectangle
   :id "canvas-bg"
   :width 4000
   :height 4000
   :style "-fx-fill: #f5f5f5;"
   :on-mouse-clicked {:event events/canvas-click}})

(defn- marquee-preview-desc
  [{:keys [start-x start-y current-x current-y]}]
  (when (and start-x start-y current-x current-y)
    (let [x (min start-x current-x)
          y (min start-y current-y)
          width (Math/abs (- current-x start-x))
          height (Math/abs (- current-y start-y))]
      {:fx/type :rectangle
       :id "marquee-selection"
       :x x
       :y y
       :width width
       :height height
       :mouse-transparent true
       :style "-fx-fill: rgba(47, 128, 237, 0.12); -fx-stroke: #2f80ed; -fx-stroke-width: 1; -fx-stroke-dash-array: 5 4;"})))

(defn canvas-stack
  [shell]
  (let [diagram (:diagram shell)
        connector-nodes (vec (keep #(connector-desc diagram %) (model/connectors diagram)))
        flow-nodes (vec (keep #(flow-desc diagram %) (model/flows diagram)))
        source-nodes (mapv #(cloud-desc diagram :source %) (model/sources diagram))
        sink-nodes (mapv #(cloud-desc diagram :sink %) (model/sinks diagram))
        converter-nodes (mapv #(converter-desc diagram %) (model/converters diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))
        preview-nodes (canvas-preview-nodes diagram (:canvas-preview shell))
        marquee-node (marquee-preview-desc (:marquee-drag shell))
        diagram-nodes (into connector-nodes
                            (concat flow-nodes source-nodes sink-nodes
                                    converter-nodes stock-nodes preview-nodes
                                    [marquee-node]))
        children (into [(canvas-background)] diagram-nodes)]
    (cond-> {:fx/type :pane
             :id "canvas"
             :clip {:fx/type :rectangle
                    :width 10000
                    :height 10000}
             :on-mouse-moved {:event events/canvas-move}
             :children (filterv map? children)}
      (= :idle (:placement-mode diagram))
      (assoc :on-mouse-pressed {:event events/marquee-drag-start :from-canvas true}
             :on-mouse-dragged {:event events/marquee-drag :from-canvas true}
             :on-mouse-released {:event events/marquee-drag-end :from-canvas true}))))

(defn canvas-desc
  [shell]
  (canvas-stack shell))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:22:50.607718-05:00", :module-hash "241096460", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "477415667"} {:id "defn-/endpoint-click", :kind "defn-", :line 6, :end-line 8, :hash "-1042549141"} {:id "def/flow-pipe-stroke-width", :kind "def", :line 10, :end-line 10, :hash "-936806796"} {:id "def/connector-stroke-width", :kind "def", :line 11, :end-line 11, :hash "-1722330092"} {:id "defn-/unit-vector", :kind "defn-", :line 13, :end-line 18, :hash "-518215995"} {:id "defn-/flow-arrowhead", :kind "defn-", :line 20, :end-line 32, :hash "-1459538016"} {:id "defn-/flow-pipe-body", :kind "defn-", :line 34, :end-line 48, :hash "312484768"} {:id "defn-/flow-desc", :kind "defn-", :line 50, :end-line 70, :hash "-1028093553"} {:id "def/connector-label-style", :kind "def", :line 72, :end-line 72, :hash "-514178943"} {:id "def/connector-formula-style", :kind "def", :line 73, :end-line 73, :hash "959198280"} {:id "defn/connector-canvas-labels", :kind "defn", :line 75, :end-line 82, :hash "-1053159609"} {:id "defn-/connector-desc", :kind "defn-", :line 84, :end-line 110, :hash "-2114511346"} {:id "defn/converter-canvas-labels", :kind "defn", :line 112, :end-line 116, :hash "-887333482"} {:id "defn/converter-canvas-position", :kind "defn", :line 118, :end-line 120, :hash "1181723587"} {:id "def/bound-label-style", :kind "def", :line 122, :end-line 122, :hash "-2104208887"} {:id "defn/stock-icon-labels", :kind "defn", :line 124, :end-line 128, :hash "-2139438638"} {:id "defn/stock-canvas-labels", :kind "defn", :line 130, :end-line 135, :hash "1666309085"} {:id "defn/stock-canvas-position", :kind "defn", :line 137, :end-line 139, :hash "1913963469"} {:id "defn/flow-icon-labels", :kind "defn", :line 141, :end-line 144, :hash "860011591"} {:id "defn-/flow-on-canvas", :kind "defn-", :line 146, :end-line 153, :hash "-1857371978"} {:id "defn/flow-canvas-labels", :kind "defn", :line 155, :end-line 157, :hash "-1659873221"} {:id "defn-/stock-desc", :kind "defn-", :line 159, :end-line 187, :hash "-1787538790"} {:id "defn-/converter-desc", :kind "defn-", :line 189, :end-line 205, :hash "399943550"} {:id "defn-/cloud-desc", :kind "defn-", :line 207, :end-line 224, :hash "273299004"} {:id "defn-/overlay-segment", :kind "defn-", :line 226, :end-line 228, :hash "-1910896273"} {:id "defn/diagram-overlay-text", :kind "defn", :line 230, :end-line 243, :hash "1103884749"} {:id "defn-/canvas-background", :kind "defn-", :line 245, :end-line 250, :hash "538381286"} {:id "defn/canvas-stack", :kind "defn", :line 252, :end-line 268, :hash "937153481"} {:id "defn/canvas-desc", :kind "defn", :line 270, :end-line 272, :hash "-772167057"}]}
;; clj-mutate-manifest-end
