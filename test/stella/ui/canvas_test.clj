(ns stella.ui.canvas-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

(deftest canvas-description-test
  (let [shell (model/default-shell)
        desc (canvas/canvas-desc shell)]
    (is (= :stack-pane (:fx/type desc)))
    (is (= "canvas" (:id desc)))
    (is (= {:event events/canvas-click} (:on-mouse-clicked desc)))))

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

(deftest canvas-renders-flows-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        flow (first (filter #(= "flow-Flow1" (:id %)) (:children desc)))
        pipe-line (first (filter #(= :line (:fx/type %)) (:children flow)))
        arrowhead (first (filter #(= :polygon (:fx/type %)) (:children flow)))]
    (is (some? flow))
    (is (>= (:stroke-width pipe-line) 6))
    (is (some? arrowhead))
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
    (is (= "Stock1 0 || Flow1 0 || Converter1 0 || Connector1"
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
                            first)
        connector-line (some->> (:children desc)
                                 (filter #(re-matches #"connector-.*" (:id %)))
                                 first
                                 :children
                                 first)]
    (is (some? connector-line))
    (is (> (:stroke-width flow-line) (:stroke-width connector-line)))))