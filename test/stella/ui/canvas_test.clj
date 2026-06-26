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
    (is (= :v-box (:fx/type (second (:children stock)))))))

(deftest canvas-renders-flows-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))
        shell (assoc (cmd/default-shell! nil) :diagram diagram)
        desc (canvas/canvas-desc shell)
        lines (filter (fn [node]
                        (and (= :group (:fx/type node))
                             (= :line (:fx/type (first (:children node))))))
                      (:children desc))]
    (is (= 1 (count lines)))))

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
        connectors (filter (fn [node]
                             (and (= :group (:fx/type node))
                                  (= "#666" (:stroke (first (:children node))))))
                           (:children desc))]
    (is (= 1 (count connectors)))))