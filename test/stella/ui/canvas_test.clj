(ns stella.ui.canvas-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

(defn- descendant-types
  [desc]
  (tree-seq map? :children desc))

(defn- roughly=
  [expected actual]
  (< (Math/abs (- expected actual)) 0.001))

(defn- distance
  [[ax ay] [bx by]]
  (Math/sqrt (+ (Math/pow (- bx ax) 2)
                (Math/pow (- by ay) 2))))

(defn- quadratic-point
  [curve t]
  (let [one-minus-t (- 1.0 t)
        start-scale (* one-minus-t one-minus-t)
        control-scale (* 2.0 one-minus-t t)
        end-scale (* t t)]
    [(+ (* start-scale (:start-x curve))
        (* control-scale (:control-x curve))
        (* end-scale (:end-x curve)))
     (+ (* start-scale (:start-y curve))
        (* control-scale (:control-y curve))
        (* end-scale (:end-y curve)))]))

(defn- arrow-wing-base
  [arrow-lines]
  [(/ (+ (:end-x (first arrow-lines)) (:end-x (second arrow-lines))) 2.0)
   (/ (+ (:end-y (first arrow-lines)) (:end-y (second arrow-lines))) 2.0)])

(defn- arrow-tip
  [arrow-lines]
  [(:start-x (first arrow-lines)) (:start-y (first arrow-lines))])

(defn- node-count
  [desc fx-type]
  (count (filter #(= fx-type (:fx/type %)) (:children desc))))

(defn- node-by
  [desc pred]
  (first (filter pred (:children desc))))

(defn- dot
  [[ax ay] [bx by]]
  (+ (* ax bx) (* ay by)))

(deftest canvas-description-test
  (let [shell (model/default-shell)
        desc (canvas/canvas-desc shell)
        background (first (:children desc))]
    (is (= :pane (:fx/type desc)))
    (is (= "canvas" (:id desc)))
    (is (nil? (:on-mouse-clicked desc)))
    (is (= {:event events/canvas-click} (:on-mouse-clicked background)))
    (is (= {:event events/canvas-move} (:on-mouse-moved desc)))
    (is (= {:event events/marquee-drag :from-canvas true}
           (:on-mouse-dragged desc)))))

(deftest canvas-renders-stocks-test
  (let [shell (-> (cmd/default-shell! nil)
                  (cmd/arm-stock-placement-on-shell!)
                  (cmd/place-stock-on-shell! 200 150))
        desc (canvas/canvas-desc shell)
        stocks (filter #(= :group (:fx/type %)) (:children desc))
        stock (first stocks)]
    (is (= 1 (count stocks)))
    (is (= "stock-Stock1" (:id stock)))
    (is (= {:name "Stock1" :min "0" :max nil}
           (canvas/stock-canvas-labels (:diagram shell) "Stock1")))))

(deftest canvas-renders-armed-object-preview-test
  (let [shell (-> (cmd/default-shell! nil)
                  (cmd/arm-stock-placement-on-shell!)
                  (assoc :canvas-preview {:x 120 :y 140}))
        preview (first (filter #(= "preview-stock" (:id %))
                               (:children (canvas/canvas-desc shell))))]
    (is (some? preview))
    (is (= 120 (:layout-x preview)))
    (is (= 140 (:layout-y preview)))
    (is (= 0.55 (:opacity preview)))))

(deftest canvas-renders-armed-cloud-preview-test
  (let [shell (-> (cmd/default-shell! nil)
                  (cmd/arm-source-placement-on-shell!)
                  (assoc :canvas-preview {:x 120 :y 140}))
        preview (first (filter #(= "preview-source" (:id %))
                               (:children (canvas/canvas-desc shell))))
        arrow (node-by preview #(and (= :line (:fx/type %))
                                     (= 40 (:start-x %))
                                     (= 25 (:start-y %))))]
    (is (some? preview))
    (is (= 3 (node-count preview :circle)))
    (is (= 0 (node-count preview :ellipse)))
    (is (= [40 25 40 -1]
           (mapv arrow [:start-x :start-y :end-x :end-y])))
    (is (empty? (filter #(= :label (:fx/type %)) (:children preview))))))

(deftest canvas-renders-clouds-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-source! "Source1" 50 80)
                    (cmd/fixture-sink! "Sink1" 250 80))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        clouds (filter #(and (= :group (:fx/type %))
                            (re-matches #"(source|sink)-.*" (:id %)))
                     (:children desc))
        source (first (filter #(= "source-Source1" (:id %)) clouds))
        sink (first (filter #(= "sink-Sink1" (:id %)) clouds))
        source-arrow (node-by source #(and (= :line (:fx/type %))
                                           (= 40 (:start-x %))
                                           (= 25 (:start-y %))))
        sink-arrow (node-by sink #(and (= :line (:fx/type %))
                                       (= 40 (:start-x %))
                                       (= -4 (:start-y %))))]
    (is (= 2 (count clouds)))
    (is (= #{"source-Source1" "sink-Sink1"} (set (map :id clouds))))
    (is (every? #(= 3 (node-count % :circle)) clouds))
    (is (every? #(= 3 (node-count % :line)) clouds))
    (is (= [40 25 40 -1]
           (mapv source-arrow [:start-x :start-y :end-x :end-y])))
    (is (= [40 -4 40 22]
           (mapv sink-arrow [:start-x :start-y :end-x :end-y])))
    (is (every? #(= 0 (node-count % :ellipse)) clouds))
    (is (every? #(empty? (filter (fn [child] (= :label (:fx/type child))) (:children %))) clouds))))

(deftest canvas-clouds-select-and-drag-when-idle-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-source! "Source1" 50 80)
                    (cmd/fixture-sink! "Sink1" 250 80))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        source (first (filter #(= "source-Source1" (:id %)) (:children desc)))
        sink (first (filter #(= "sink-Sink1" (:id %)) (:children desc)))]
    (is (= {:event events/selection-click
            :object-kind :source
            :object-name "Source1"}
           (:on-mouse-clicked source)))
    (is (= {:event events/cloud-drag-start
            :cloud-kind :source
            :cloud-name "Source1"}
           (:on-mouse-pressed source)))
    (is (= {:event events/cloud-drag
            :cloud-kind :source
            :cloud-name "Source1"}
           (:on-mouse-dragged source)))
    (is (= {:event events/selection-click
            :object-kind :sink
            :object-name "Sink1"}
           (:on-mouse-clicked sink)))
    (is (= {:event events/cloud-drag
            :cloud-kind :sink
            :cloud-name "Sink1"}
           (:on-mouse-dragged sink)))
    (is (= {:event events/cloud-drag-end
            :cloud-kind :sink
            :cloud-name "Sink1"}
           (:on-mouse-released sink)))))

(deftest canvas-selected-clouds-render-ellipse-outline-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-source! "Source1" 50 80)
                    (cmd/click-select! :source "Source1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        source (first (filter #(= "source-Source1" (:id %))
                              (:children (canvas/canvas-desc shell))))
        outline (first (:children source))]
    (is (= :ellipse (:fx/type outline)))
    (is (= 42 (:radius-x outline)))
    (is (= 27 (:radius-y outline)))))

(deftest canvas-renders-live-marquee-test
  (let [shell (assoc (cmd/default-shell! nil)
                     :marquee-drag {:start-x 100
                                    :start-y 80
                                    :current-x 40
                                    :current-y 130})
        marquee (first (filter #(= "marquee-selection" (:id %))
                               (:children (canvas/canvas-desc shell))))]
    (is (some? marquee))
    (is (= 40 (:x marquee)))
    (is (= 80 (:y marquee)))
    (is (= 60 (:width marquee)))
    (is (= 50 (:height marquee)))))

(deftest canvas-renders-flows-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        flow (first (filter #(= "flow-Flow1" (:id %)) (:children desc)))
        pipe-lines (filter #(= :line (:fx/type %)) (:children flow))
        midpoint-circle (first (filter #(= :circle (:fx/type %)) (:children flow)))
        flow-labels (filter #(= :label (:fx/type %)) (:children flow))
        name-label (first (filter #(= "Flow1" (:text %)) flow-labels))
        rate-label (first (filter #(= "0" (:text %)) flow-labels))
        visible-pipe-lines (remove #(= "transparent" (:stroke %)) pipe-lines)
        pipe-line (last visible-pipe-lines)
        arrowhead (first (filter #(= :polygon (:fx/type %)) (:children flow)))
        arrow-points (:points arrowhead)]
    (is (some? flow))
    (is (= 2 (count visible-pipe-lines)))
    (is (= 1 (count (filter #(= "transparent" (:stroke %)) pipe-lines))))
    (is (every? #(>= (:stroke-width %) 8) visible-pipe-lines))
    (is (= [180.0 145.0]
           (mapv pipe-line [:start-x :start-y])))
    (is (roughly= 287.478 (:end-x pipe-line)))
    (is (roughly= 198.739 (:end-y pipe-line)))
    (is (= {:fx/type :circle
            :center-x 240.0
            :center-y 175.0
            :radius 12.0
            :fill "white"
            :stroke "#555"
            :stroke-width 1
            :mouse-transparent true}
           midpoint-circle))
    (is (> (.indexOf (:children flow) midpoint-circle)
           (.indexOf (:children flow) pipe-line)))
    (is (= :polygon (:fx/type arrowhead)))
    (is (= "#eef4f7" (:fill arrowhead)))
    (is (= "#555" (:stroke arrowhead)))
    (is (= [300.0 205.0] (take 2 arrow-points)))
    (is (roughly= (:end-x pipe-line)
                  (/ (+ (nth arrow-points 2) (nth arrow-points 4)) 2.0)))
    (is (roughly= (:end-y pipe-line)
                  (/ (+ (nth arrow-points 3) (nth arrow-points 5)) 2.0)))
    (is (roughly= 20.0 (distance [(nth arrow-points 2) (nth arrow-points 3)]
                                  [(nth arrow-points 4) (nth arrow-points 5)])))
    (is (= 180.0 (:layout-x name-label)))
    (is (= 180.0 (:layout-x rate-label)))
    (is (= 120 (:pref-width name-label)))
    (is (= 120 (:pref-width rate-label)))
    (is (= :center (:alignment name-label)))
    (is (= :center (:alignment rate-label)))
    (is (<= (+ (:layout-y name-label) 18 6)
            (- (:center-y midpoint-circle) (:radius midpoint-circle))))
    (is (>= (:layout-y rate-label)
            (+ (:center-y midpoint-circle) (:radius midpoint-circle) 6)))
    (is (= {:name "Flow1" :rate "0"}
           (canvas/flow-canvas-labels diagram "Flow1")))))

(deftest canvas-renders-flow-draft-preview-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/arm-flow-placement!)
                    (cmd/select-flow-source! :stock "Stock1"))
        shell (assoc (cmd/default-shell! nil)
                     :diagram diagram
                     :canvas-preview {:x 280 :y 160})
        preview (first (filter #(= "preview-flow" (:id %))
                               (:children (canvas/canvas-desc shell))))
        lines (filter #(= :line (:fx/type %)) (:children preview))]
    (is (some? preview))
    (is (= 2 (count lines)))))

(deftest canvas-flows-select-when-idle-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        flow (first (filter #(= "flow-Flow1" (:id %))
                            (:children (canvas/canvas-desc shell))))]
    (is (= {:event events/selection-click
            :object-kind :flow
            :object-name "Flow1"}
           (:on-mouse-clicked flow)))))

(deftest canvas-selected-flow-renders-highlight-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/click-select! :flow "Flow1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        flow (first (filter #(= "flow-Flow1" (:id %))
                            (:children (canvas/canvas-desc shell))))
        pipe-lines (filter #(= :line (:fx/type %)) (:children flow))
        visible-pipe-lines (remove #(= "transparent" (:stroke %)) pipe-lines)
        highlight (first (filter #(= "#2f80ed" (:stroke %)) pipe-lines))]
    (is (= 3 (count visible-pipe-lines)))
    (is (= 16 (:stroke-width highlight)))
    (is (= 0.35 (:opacity highlight)))))

(deftest canvas-uses-cljfx-layout-type-names-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/fixture-connector! "Connector1" "Converter1" "Flow1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        types (set (map :fx/type (descendant-types (canvas/canvas-desc shell))))]
    (is (not (contains? types :vbox)))))

(deftest flow-canvas-labels-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))]
    (is (= {:name "Flow1" :rate "0"}
           (canvas/flow-canvas-labels diagram "Flow1")))))

(deftest converter-canvas-labels-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/set-converter-name! "Converter1" "Growth"))]
    (is (= {:name "Growth"} (canvas/converter-canvas-labels diagram "Growth")))))

(deftest canvas-renders-converter-name-below-circle-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-converter! "Converter1" 100 250))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        converter (first (filter #(= "converter-Converter1" (:id %))
                                 (:children (canvas/canvas-desc shell))))
        label (first (filter #(= :label (:fx/type %)) (:children converter)))]
    (is (= "Converter1" (:text label)))
    (is (= -25.0 (:layout-x label)))
    (is (= 56 (:layout-y label)))
    (is (= 100 (:pref-width label)))
    (is (= :center (:alignment label)))))

(deftest diagram-overlay-text-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-converter! "Converter1" 25 75)
                    (cmd/arm-connector-placement!)
                    (cmd/select-connector-origin! :converter "Converter1")
                    (cmd/connect-connector! :flow "Flow1"))]
    (is (= "Stock1 0 || Flow1 0 || Converter1 || Connector1"
           (canvas/diagram-overlay-text diagram)))))

(deftest canvas-renders-connectors-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 200 150)
                    (cmd/fixture-stock! "Stock2" 350 150)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/arm-connector-placement!)
                    (cmd/select-connector-origin! :converter "Converter1")
                    (cmd/connect-connector! :flow "Flow1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        flow-line (some->> (:children desc)
                            (filter #(= "flow-Flow1" (:id %)))
                            first
                            :children
                            (filter #(= :line (:fx/type %)))
                            (remove #(= "transparent" (:stroke %)))
                            last)
        connector-curve (some->> (:children desc)
                                  (filter #(re-matches #"connector-.*" (:id %)))
                                  first
                                  :children
                                  (filter #(= :quad-curve (:fx/type %)))
                                  first)
        flow-midpoint (model/flow-midpoint diagram "Flow1")
        connector-lines (some->> (:children desc)
                                  (filter #(re-matches #"connector-.*" (:id %)))
                                  first
                                  :children
                                  (filter #(= :line (:fx/type %))))
        connector-labels (some->> (:children desc)
                                   (filter #(re-matches #"connector-.*" (:id %)))
                                   first
                                   :children
                                   (filter #(= :label (:fx/type %))))
        connector-label-containers (some->> (:children desc)
                                            (filter #(re-matches #"connector-.*" (:id %)))
                                            first
                                            :children
                                            (filter #(= :v-box (:fx/type %))))
        connector-curves (some->> (:children desc)
                                   (filter #(re-matches #"connector-.*" (:id %)))
                                   first
                                   :children
                                   (filter #(= :quad-curve (:fx/type %))))
        visible-connector-lines (remove #(= "transparent" (:stroke %)) connector-lines)
        hit-curves (filter #(= "transparent" (:stroke %)) connector-curves)
        control-points (some->> (:children desc)
                                (filter #(re-matches #"connector-.*" (:id %)))
                                first
                                :children
                                (filter #(= :circle (:fx/type %))))
        connector-polygons (some->> (:children desc)
                                     (filter #(re-matches #"connector-.*" (:id %)))
                                     first
                                     :children
                                     (filter #(= :polygon (:fx/type %))))
        arrow-lines visible-connector-lines
        arrow-tip (arrow-tip arrow-lines)
        arrow-base (arrow-wing-base arrow-lines)
        control-point (first (filter #(= "#000" (:fill %)) control-points))
        hit-control-point (first (filter #(= "transparent" (:fill %)) control-points))
        curve-midpoint (quadratic-point connector-curve 0.5)
        chord-midpoint [(/ (+ (:start-x connector-curve) (:end-x connector-curve)) 2.0)
                        (/ (+ (:start-y connector-curve) (:end-y connector-curve)) 2.0)]
        converter-center [125.0 275.0]]
    (is (some? connector-curve))
    (is (= 1 (count connector-curves)))
    (is (= 2 (count visible-connector-lines)))
    (is (= 0 (count hit-curves)))
    (is (= 2 (count control-points)))
    (is (empty? connector-labels))
    (is (empty? connector-label-containers))
    (is (= "#000" (:fill control-point)))
    (is (= 3 (:radius control-point)))
    (is (= 10 (:radius hit-control-point)))
    (is (roughly= (first curve-midpoint) (:center-x control-point)))
    (is (roughly= (second curve-midpoint) (:center-y control-point)))
    (is (not (roughly= (:control-x connector-curve) (:center-x control-point))))
    (is (= {:event events/connector-control-drag-start
            :connector-name "Connector1"}
           (:on-mouse-pressed control-point)))
    (is (= {:event events/connector-control-drag
            :connector-name "Connector1"}
           (:on-mouse-dragged control-point)))
    (is (= {:event events/connector-control-drag-end
            :connector-name "Connector1"}
           (:on-mouse-released control-point)))
    (is (= (:on-mouse-pressed control-point)
           (:on-mouse-pressed hit-control-point)))
    (is (= (:on-mouse-dragged control-point)
           (:on-mouse-dragged hit-control-point)))
    (is (= (:on-mouse-released control-point)
           (:on-mouse-released hit-control-point)))
    (is (> (distance [(:control-x connector-curve) (:control-y connector-curve)] [2000.0 2000.0])
           (distance chord-midpoint [2000.0 2000.0])))
    (is (empty? connector-polygons))
    (is (every? #(and (roughly= (first arrow-tip) (:start-x %))
                      (roughly= (second arrow-tip) (:start-y %)))
                arrow-lines))
    (is (not= [(:end-x connector-curve) (:end-y connector-curve)] arrow-tip))
    (is (roughly= 12.0 (distance flow-midpoint arrow-tip)))
    (is (pos? (dot [(- (:end-x connector-curve) (:control-x connector-curve))
                    (- (:end-y connector-curve) (:control-y connector-curve))]
                   [(- (first arrow-tip) (first arrow-base))
                    (- (second arrow-tip) (second arrow-base))])))
    (is (nil? (:stroke-dash-array connector-curve)))
    (is (= converter-center
           [(:start-x connector-curve) (:start-y connector-curve)]))
    (is (= flow-midpoint
           [(:end-x connector-curve) (:end-y connector-curve)]))
    (is (nil? (:on-mouse-clicked (some->> (:children desc)
                                          (filter #(re-matches #"connector-.*" (:id %)))
                                          first))))
    (is (= {:event events/selection-click
            :object-kind :connector
            :object-name "Connector1"}
           (:on-mouse-clicked control-point)))
    (is (> (:stroke-width flow-line) (:stroke-width connector-curve)))))

(deftest canvas-renders-stock-connectors-dashed-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 200 150)
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/fixture-stock-connector! "Connector1" "Stock1" "Converter1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        connector (first (filter #(= "connector-Connector1" (:id %))
                                 (:children (canvas/canvas-desc shell))))
        connector-curve (first (filter #(and (= :quad-curve (:fx/type %))
                                             (= "#666" (:stroke %)))
                                       (:children connector)))
        connector-lines (filter #(= :line (:fx/type %)) (:children connector))
        arrow-lines (remove #(= "transparent" (:stroke %)) connector-lines)
        arrow-tip (arrow-tip arrow-lines)
        arrow-base (arrow-wing-base arrow-lines)
        stock-center [240.0 175.0]
        converter-center [125.0 275.0]]
    (is (= [6 4] (:stroke-dash-array connector-curve)))
    (is (every? #(= [6 4] (:stroke-dash-array %)) arrow-lines))
    (is (= stock-center
           [(:start-x connector-curve) (:start-y connector-curve)]))
    (is (= converter-center
           [(:end-x connector-curve) (:end-y connector-curve)]))
    (is (not= [(:end-x connector-curve) (:end-y connector-curve)] arrow-tip))
    (is (roughly= 25.0 (distance converter-center arrow-tip)))
    (is (pos? (dot [(- (:end-x connector-curve) (:control-x connector-curve))
                    (- (:end-y connector-curve) (:control-y connector-curve))]
                   [(- (first arrow-tip) (first arrow-base))
                    (- (second arrow-tip) (second arrow-base))])))))

(deftest canvas-selected-connector-renders-highlight-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 200 150)
                    (cmd/fixture-stock! "Stock2" 350 150)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/fixture-connector! "Connector1" "Converter1" "Flow1")
                    (cmd/click-select! :connector "Connector1"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        connector (first (filter #(= "connector-Connector1" (:id %))
                                 (:children (canvas/canvas-desc shell))))
        lines (filter #(= :line (:fx/type %)) (:children connector))
        curves (filter #(= :quad-curve (:fx/type %)) (:children connector))
        visible-lines (remove #(= "transparent" (:stroke %)) lines)
        highlights (concat (filter #(= "#2f80ed" (:stroke %)) lines)
                           (filter #(= "#2f80ed" (:stroke %)) curves))]
    (is (= 4 (count visible-lines)))
    (is (= 0 (count (filter #(= "transparent" (:stroke %)) curves))))
    (is (= 3 (count highlights)))
    (is (every? #(= 0.35 (:opacity %)) highlights))))

(deftest canvas-renders-connector-draft-preview-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/arm-connector-placement!)
                    (cmd/select-connector-origin! :converter "Converter1"))
        shell (assoc (cmd/default-shell! nil)
                     :diagram diagram
                     :canvas-preview {:x 250 :y 200})
        preview (first (filter #(= "preview-connector" (:id %))
                               (:children (canvas/canvas-desc shell))))
        lines (filter #(= :line (:fx/type %)) (:children preview))
        curves (filter #(= :quad-curve (:fx/type %)) (:children preview))
        control-points (filter #(= :circle (:fx/type %)) (:children preview))]
    (is (some? preview))
    (is (= 2 (count lines)))
    (is (= 1 (count curves)))
    (is (= 1 (count control-points)))))
