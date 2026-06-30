(ns stella.simulation-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.simulation :as simulation]))

(defn- diagram-with-stock-flow []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/set-stock-initial-value "Stock1" "100")
      (model/set-stock-initial-value "Stock2" "0")
      (model/set-flow-rate "Flow1" "10")))

(deftest stock-value-equals-initial-before-simulation-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 100 100)
                    (model/set-stock-initial-value "Stock1" "42"))]
    (is (= "42" (simulation/stock-value diagram "Stock1")))
    (is (zero? (simulation/simulation-time diagram)))))

(deftest stock-to-stock-simulation-test
  (let [after-one (simulation/run-steps (diagram-with-stock-flow) 1)
        after-five (simulation/run-steps (diagram-with-stock-flow) 5)]
    (is (= "99" (simulation/stock-value after-one "Stock1")))
    (is (= "1" (simulation/stock-value after-one "Stock2")))
    (is (= 0.1 (simulation/simulation-time after-one)))
    (is (= "95" (simulation/stock-value after-five "Stock1")))
    (is (= "5" (simulation/stock-value after-five "Stock2")))
    (is (= 0.5 (simulation/simulation-time after-five)))))

(deftest source-to-stock-simulation-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-source "Source1" 50 150)
                    (model/fixture-flow-from-source "Flow1" "Source1" "Stock1")
                    (model/set-stock-initial-value "Stock1" "0")
                    (model/set-flow-rate "Flow1" "10"))
        after-one (simulation/run-steps diagram 1)]
    (is (= "1" (simulation/stock-value after-one "Stock1")))
    (is (= 0.1 (simulation/simulation-time after-one)))))

(deftest stock-to-sink-simulation-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/fixture-flow-to-sink "Flow1" "Stock1" "Sink1")
                    (model/set-stock-initial-value "Stock1" "100")
                    (model/set-flow-rate "Flow1" "10"))
        after-one (simulation/run-steps diagram 1)]
    (is (= "99" (simulation/stock-value after-one "Stock1")))
    (is (= 0.1 (simulation/simulation-time after-one)))))

(deftest source-and-sink-net-simulation-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-source "Source1" 50 150)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/fixture-flow-from-source "Flow1" "Source1" "Stock1")
                    (model/fixture-flow-to-sink "Flow2" "Stock1" "Sink1")
                    (model/set-stock-initial-value "Stock1" "0")
                    (model/set-flow-rate "Flow1" "10")
                    (model/set-flow-rate "Flow2" "5"))
        after-one (simulation/run-steps diagram 1)
        after-two (simulation/run-steps diagram 2)]
    (is (= "0.5" (simulation/stock-value after-one "Stock1")))
    (is (= "1" (simulation/stock-value after-two "Stock1")))
    (is (= 0.2 (simulation/simulation-time after-two)))))

(deftest zero-rate-advances-time-only-test
  (let [diagram (-> (diagram-with-stock-flow)
                    (model/set-stock-initial-value "Stock1" "50")
                    (model/set-stock-initial-value "Stock2" "20")
                    (model/set-flow-rate "Flow1" "0"))
        after-three (simulation/run-steps diagram 3)]
    (is (= "50" (simulation/stock-value after-three "Stock1")))
    (is (= "20" (simulation/stock-value after-three "Stock2")))
    (is (= 0.3 (simulation/simulation-time after-three)))))

(deftest run-simulation-command-test
  (let [diagram (cmd/run-simulation-steps! (diagram-with-stock-flow) 1)]
    (is (= "99" (simulation/stock-value diagram "Stock1")))
    (is (= 0.1 (simulation/simulation-time diagram)))))