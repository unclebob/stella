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
  (when-let [{:keys [name value]} (first (filter #(= converter-name (:name %))
                                                 (model/converters diagram)))]
    {:name name
     :value (or value "0")}))

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
      [(thermometer-rectangle therm (:track-width therm) thermometer-track-style)
       (thermometer-rectangle therm (max 0 fill-width) thermometer-fill-style
                              :id fill-id
                              :mouse-transparent? true)])))

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
                :layout-x 0
                :layout-y 18
                :pref-width 50
                :pref-height 14
                :alignment :center
                :text-alignment :center
                :style "-fx-font-size: 10px;"
                :text (or (model/converter-value diagram name) "0")}
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
   :fx/key "canvas-bg"
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
       :fx/key "marquee-selection"
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
;; {:version 1, :tested-at "2026-06-30T11:23:18.454621-05:00", :module-hash "2037205985", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "-567149834"} {:id "defn-/endpoint-click", :kind "defn-", :line 8, :end-line 10, :hash "-1042549141"} {:id "defn-/selection-click", :kind "defn-", :line 12, :end-line 14, :hash "1908235825"} {:id "def/selection-outline-style", :kind "def", :line 16, :end-line 17, :hash "1190386272"} {:id "defn-/with-rect-selection-outline", :kind "defn-", :line 19, :end-line 30, :hash "-2036968219"} {:id "defn-/with-ellipse-selection-outline", :kind "defn-", :line 32, :end-line 43, :hash "-439484726"} {:id "def/flow-pipe-stroke-width", :kind "def", :line 45, :end-line 45, :hash "-936806796"} {:id "def/connector-stroke-width", :kind "def", :line 46, :end-line 46, :hash "-1722330092"} {:id "def/connector-arrow-size", :kind "def", :line 47, :end-line 47, :hash "-2106447463"} {:id "def/flow-arrow-size", :kind "def", :line 48, :end-line 48, :hash "1500874750"} {:id "def/flow-arrow-wing", :kind "def", :line 49, :end-line 49, :hash "363708473"} {:id "def/flow-pipe-fill", :kind "def", :line 50, :end-line 50, :hash "-31819995"} {:id "def/flow-boundary-radius", :kind "def", :line 51, :end-line 51, :hash "1198641873"} {:id "def/flow-label-width", :kind "def", :line 52, :end-line 52, :hash "-211844601"} {:id "def/flow-label-height", :kind "def", :line 53, :end-line 53, :hash "1932042292"} {:id "def/flow-label-gap", :kind "def", :line 54, :end-line 54, :hash "2040781150"} {:id "def/preview-opacity", :kind "def", :line 55, :end-line 55, :hash "-1736148803"} {:id "def/canvas-center", :kind "def", :line 56, :end-line 56, :hash "-823088025"} {:id "def/connector-control-radius", :kind "def", :line 57, :end-line 57, :hash "-1560957918"} {:id "def/connector-control-hit-radius", :kind "def", :line 58, :end-line 58, :hash "465261268"} {:id "defn-/endpoint-center", :kind "defn-", :line 60, :end-line 66, :hash "-652861683"} {:id "defn-/ellipse-boundary-point", :kind "defn-", :line 68, :end-line 76, :hash "-1806779819"} {:id "defn-/rectangle-boundary-point", :kind "defn-", :line 78, :end-line 86, :hash "878119706"} {:id "defn-/endpoint-boundary-point", :kind "defn-", :line 88, :end-line 96, :hash "-1898240722"} {:id "defn-/clipped-link-endpoints", :kind "defn-", :line 98, :end-line 103, :hash "939141369"} {:id "defn-/unit-vector", :kind "defn-", :line 105, :end-line 112, :hash "-24303620"} {:id "defn-/connector-default-control-offset", :kind "defn-", :line 114, :end-line 128, :hash "107222231"} {:id "defn-/connector-control-point", :kind "defn-", :line 130, :end-line 135, :hash "258390474"} {:id "defn-/quadratic-point", :kind "defn-", :line 137, :end-line 148, :hash "-1382276052"} {:id "defn-/connector-curve-midpoint", :kind "defn-", :line 150, :end-line 152, :hash "-668851218"} {:id "defn-/with-stroke-dash", :kind "defn-", :line 154, :end-line 157, :hash "1627518132"} {:id "defn-/connector-curve", :kind "defn-", :line 159, :end-line 174, :hash "1729291355"} {:id "defn-/connector-control-drag-events", :kind "defn-", :line 176, :end-line 183, :hash "1935972712"} {:id "defn-/connector-handle-desc", :kind "defn-", :line 185, :end-line 210, :hash "11149100"} {:id "defn-/connector-arrowhead", :kind "defn-", :line 212, :end-line 240, :hash "1987389552"} {:id "defn-/flow-arrowhead", :kind "defn-", :line 242, :end-line 254, :hash "92217714"} {:id "defn-/flow-pipe-body", :kind "defn-", :line 256, :end-line 288, :hash "1937527804"} {:id "defn-/flow-hit-line", :kind "defn-", :line 290, :end-line 299, :hash "-1913031846"} {:id "defn-/flow-midpoint-circle", :kind "defn-", :line 301, :end-line 310, :hash "-1317546727"} {:id "defn-/flow-label", :kind "defn-", :line 312, :end-line 321, :hash "364006743"} {:id "defn-/flow-labels", :kind "defn-", :line 323, :end-line 330, :hash "1324904471"} {:id "defn-/flow-desc", :kind "defn-", :line 332, :end-line 355, :hash "-303958501"} {:id "defn-/connector-stroke-dash-array", :kind "defn-", :line 357, :end-line 361, :hash "-984033385"} {:id "defn-/connector-body", :kind "defn-", :line 363, :end-line 383, :hash "-1486105772"} {:id "defn/connector-canvas-labels", :kind "defn", :line 385, :end-line 392, :hash "-1053159609"} {:id "defn-/connector-desc", :kind "defn-", :line 394, :end-line 411, :hash "739161325"} {:id "defn/converter-canvas-labels", :kind "defn", :line 413, :end-line 418, :hash "1889346497"} {:id "defn/converter-canvas-position", :kind "defn", :line 420, :end-line 422, :hash "1181723587"} {:id "def/converter-label-width", :kind "def", :line 424, :end-line 424, :hash "605605520"} {:id "def/converter-label-gap", :kind "def", :line 425, :end-line 425, :hash "-1040156602"} {:id "def/converter-label-height", :kind "def", :line 426, :end-line 426, :hash "-1595430248"} {:id "def/bound-label-style", :kind "def", :line 428, :end-line 428, :hash "-2104208887"} {:id "def/stock-bound-row-y", :kind "def", :line 429, :end-line 429, :hash "-851565702"} {:id "def/stock-bound-side-width", :kind "def", :line 430, :end-line 430, :hash "1971583436"} {:id "def/stock-bound-center-width", :kind "def", :line 431, :end-line 431, :hash "226744719"} {:id "def/stock-bound-side-x", :kind "def", :line 432, :end-line 432, :hash "2063657045"} {:id "def/stock-bound-center-x", :kind "def", :line 433, :end-line 433, :hash "-554892415"} {:id "def/stock-bound-max-x", :kind "def", :line 434, :end-line 434, :hash "-299205663"} {:id "def/thermometer-fill-style", :kind "def", :line 435, :end-line 435, :hash "1157283931"} {:id "def/thermometer-track-style", :kind "def", :line 436, :end-line 436, :hash "-584172645"} {:id "defn/stock-canvas-thermometer", :kind "defn", :line 438, :end-line 440, :hash "-1947127297"} {:id "defn-/thermometer-rectangle", :kind "defn-", :line 442, :end-line 451, :hash "432113984"} {:id "defn-/thermometer-nodes", :kind "defn-", :line 453, :end-line 461, :hash "657837003"} {:id "defn/stock-canvas-labels", :kind "defn", :line 463, :end-line 465, :hash "766167394"} {:id "defn/stock-canvas-position", :kind "defn", :line 467, :end-line 469, :hash "1913963469"} {:id "defn/flow-icon-labels", :kind "defn", :line 471, :end-line 474, :hash "860011591"} {:id "defn-/flow-on-canvas", :kind "defn-", :line 476, :end-line 483, :hash "899244249"} {:id "defn/flow-canvas-labels", :kind "defn", :line 485, :end-line 487, :hash "-1659873221"} {:id "defn-/stock-bound-row-labels", :kind "defn-", :line 489, :end-line 513, :hash "-20258019"} {:id "defn-/stock-desc", :kind "defn-", :line 515, :end-line 546, :hash "-1843372983"} {:id "defn-/converter-desc", :kind "defn-", :line 548, :end-line 589, :hash "1297928604"} {:id "def/cloud-shape-style", :kind "def", :line 591, :end-line 592, :hash "1080239382"} {:id "defn-/cloud-shape", :kind "defn-", :line 594, :end-line 615, :hash "-540921027"} {:id "defn-/cloud-arrowhead", :kind "defn-", :line 617, :end-line 632, :hash "460841082"} {:id "defn-/cloud-direction-arrow", :kind "defn-", :line 634, :end-line 652, :hash "-917119525"} {:id "defn-/directed-cloud-shape", :kind "defn-", :line 654, :end-line 656, :hash "-1443678781"} {:id "defn-/cloud-selection-highlight-circle", :kind "defn-", :line 658, :end-line 668, :hash "-1772442301"} {:id "defn-/cloud-selection-highlight-line", :kind "defn-", :line 670, :end-line 680, :hash "-927055013"} {:id "defn-/cloud-selection-highlight", :kind "defn-", :line 682, :end-line 687, :hash "-1068225471"} {:id "defn-/with-cloud-selection-highlight", :kind "defn-", :line 689, :end-line 693, :hash "25169482"} {:id "defn-/cloud-desc", :kind "defn-", :line 695, :end-line 718, :hash "-1894442526"} {:id "defn-/preview-stock-desc", :kind "defn-", :line 720, :end-line 736, :hash "507146799"} {:id "defn-/preview-cloud-desc", :kind "defn-", :line 738, :end-line 747, :hash "-1215548052"} {:id "defn-/preview-child", :kind "defn-", :line 749, :end-line 751, :hash "1824542590"} {:id "defn-/preview-converter-desc", :kind "defn-", :line 753, :end-line 776, :hash "-1337203564"} {:id "defn-/placement-preview-desc", :kind "defn-", :line 778, :end-line 785, :hash "-276426215"} {:id "defn-/draft-flow-desc", :kind "defn-", :line 787, :end-line 798, :hash "-459651754"} {:id "defn-/draft-connector-desc", :kind "defn-", :line 800, :end-line 812, :hash "664630687"} {:id "defn-/canvas-preview-nodes", :kind "defn-", :line 814, :end-line 820, :hash "1524252989"} {:id "defn-/overlay-segment", :kind "defn-", :line 822, :end-line 824, :hash "-1910896273"} {:id "defn/diagram-overlay-text", :kind "defn", :line 826, :end-line 840, :hash "-2087119286"} {:id "defn-/canvas-background", :kind "defn-", :line 842, :end-line 849, :hash "218207276"} {:id "defn-/marquee-preview-desc", :kind "defn-", :line 851, :end-line 866, :hash "-1454712816"} {:id "defn/canvas-stack", :kind "defn", :line 868, :end-line 894, :hash "-1027837223"} {:id "defn/canvas-desc", :kind "defn", :line 896, :end-line 898, :hash "-772167057"}]}
;; clj-mutate-manifest-end
