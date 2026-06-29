(ns stella.selection-hit-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- connector-diagram []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 200 150)
      (cmd/fixture-stock! "Stock2" 350 150)
      (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
      (cmd/fixture-converter! "Converter1" 100 250)
      (cmd/fixture-connector! "Connector1" "Converter1" "Flow1")))

(deftest object-at-canvas-point-selects-stock-test
  (let [diagram (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 100 100)]
    (is (= {:kind :stock :id "Stock1"}
           (model/object-at-canvas-point diagram 140 125)))))

(deftest object-at-canvas-point-selects-connector-at-handle-only-test
  (let [diagram (connector-diagram)
        [hx hy] (model/connector-handle-position diagram "Connector1")]
    (is (some? [hx hy]))
    (is (= {:kind :connector :id "Connector1"}
           (model/object-at-canvas-point diagram hx hy)))
    (is (not= {:kind :connector :id "Connector1"}
              (model/object-at-canvas-point diagram 175 230)))))

(deftest click-select-at-connector-on-curve-misses-test
  (let [diagram (connector-diagram)]
    (is (not (model/selected? (model/click-select-at diagram 175 230) :connector "Connector1")))))

(deftest click-select-at-connector-on-handle-selects-test
  (let [diagram (connector-diagram)
        [hx hy] (model/connector-handle-position diagram "Connector1")]
    (is (model/selected? (model/click-select-at diagram hx hy) :connector "Connector1"))))

(deftest click-select-at-connector-on-handle-while-stock-armed-test
  (let [diagram (-> (connector-diagram)
                    (model/arm-stock-placement))
        [hx hy] (model/connector-handle-position diagram "Connector1")]
    (is (model/selected? (model/click-select-at diagram hx hy) :connector "Connector1"))
    (is (= :stock (:placement-mode diagram)))))