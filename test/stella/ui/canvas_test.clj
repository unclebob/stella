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

(deftest canvas-description-test
  (let [shell (model/default-shell)
        desc (canvas/canvas-desc shell)]
    (is (= :pane (:fx/type desc)))
    (is (= "canvas" (:id desc)))
    (is (= {:event events/canvas-click} (:on-mouse-clicked desc)))
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

(deftest canvas-renders-clouds-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-source! "Source1" 50 80)
                    (cmd/fixture-sink! "Sink1" 250 80))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        clouds (filter #(and (= :group (:fx/type %))
                            (re-matches #"(source|sink)-.*" (:id %)))
                     (:children desc))]
    (is (= 2 (count clouds)))
    (is (= #{"source-Source1" "sink-Sink1"} (set (map :id clouds))))))

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
        visible-pipe-lines (remove #(= "transparent" (:stroke %)) pipe-lines)
        pipe-line (last visible-pipe-lines)
        arrowhead (first (filter #(= :polygon (:fx/type %)) (:children flow)))]
    (is (some? flow))
    (is (= 2 (count visible-pipe-lines)))
    (is (= 1 (count (filter #(= "transparent" (:stroke %)) pipe-lines))))
    (is (every? #(>= (:stroke-width %) 8) visible-pipe-lines))
    (is (= [180.0 145.0 300.0 205.0]
           (mapv pipe-line [:start-x :start-y :end-x :end-y])))
    (is (nil? arrowhead))
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
    (is (contains? types :v-box))
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
        connector-line (some->> (:children desc)
                                 (filter #(re-matches #"connector-.*" (:id %)))
                                 first
                                 :children
                                 (filter #(= :line (:fx/type %)))
                                 first)
        flow-midpoint (model/flow-midpoint diagram "Flow1")
        connector-lines (some->> (:children desc)
                                  (filter #(re-matches #"connector-.*" (:id %)))
                                  first
                                  :children
                                  (filter #(= :line (:fx/type %))))
        visible-connector-lines (remove #(= "transparent" (:stroke %)) connector-lines)
        hit-lines (filter #(= "transparent" (:stroke %)) connector-lines)
        connector-polygons (some->> (:children desc)
                                     (filter #(re-matches #"connector-.*" (:id %)))
                                     first
                                     :children
                                     (filter #(= :polygon (:fx/type %))))
        arrow-lines (rest visible-connector-lines)]
    (is (some? connector-line))
    (is (= 3 (count visible-connector-lines)))
    (is (= 1 (count hit-lines)))
    (is (empty? connector-polygons))
    (is (every? #(and (roughly= (:end-x connector-line) (:start-x %))
                      (roughly= (:end-y connector-line) (:start-y %)))
                arrow-lines))
    (is (not= [150.0 275.0 305.0 175.0]
              (mapv connector-line [:start-x :start-y :end-x :end-y])))
    (is (not= flow-midpoint
              [(:end-x connector-line) (:end-y connector-line)]))
    (is (= {:event events/selection-click
            :object-kind :connector
            :object-name "Connector1"}
           (:on-mouse-clicked (some->> (:children desc)
                                       (filter #(re-matches #"connector-.*" (:id %)))
                                       first))))
    (is (> (:stroke-width flow-line) (:stroke-width connector-line)))))

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
        visible-lines (remove #(= "transparent" (:stroke %)) lines)
        highlights (filter #(= "#2f80ed" (:stroke %)) lines)]
    (is (= 6 (count visible-lines)))
    (is (= 1 (count (filter #(= "transparent" (:stroke %)) lines))))
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
        lines (filter #(= :line (:fx/type %)) (:children preview))]
    (is (some? preview))
    (is (= 4 (count lines)))))
