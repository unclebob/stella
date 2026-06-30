(ns stella.thermometer-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.simulation :as simulation]
            [stella.thermometer :as thermometer]))

(defn- diagram-with-stock []
  (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 200 150))

(defn- bounded-diagram-with-stock []
  (-> (diagram-with-stock)
      (cmd/set-stock-min! "Stock1" "0")
      (cmd/set-stock-max! "Stock1" "100")))

(deftest bounded-thermometer-fill-width-test
  (let [diagram (bounded-diagram-with-stock)]
    (is (= 0 (get (thermometer/stock-thermometer diagram "Stock1") :fill-width)))
    (is (= 36 (get (thermometer/stock-thermometer
                    (cmd/set-stock-initial-value! diagram "Stock1" "50")
                    "Stock1")
                  :fill-width)))
    (is (= 72 (get (thermometer/stock-thermometer
                    (cmd/set-stock-initial-value! diagram "Stock1" "100")
                    "Stock1")
                  :fill-width)))))

(deftest unbounded-thermometer-fill-width-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/clear-stock-max! "Stock1")
                    (cmd/set-stock-initial-value! "Stock1" "25"))]
    (is (= 18 (get (thermometer/stock-thermometer diagram "Stock1") :fill-width)))))

(deftest thermometer-track-dimensions-test
  (let [therm (thermometer/stock-thermometer (bounded-diagram-with-stock) "Stock1")]
    (is (= 72 (:track-width therm)))
    (is (= 8 (:track-height therm)))
    (is (= "light blue" (:fill-color therm)))))

(deftest thermometer-layout-test
  (let [therm (thermometer/stock-thermometer (bounded-diagram-with-stock) "Stock1")]
    (is (:name-at-top therm))
    (is (:thermometer-below-name therm))))

(deftest thermometer-fill-after-simulation-test
  (let [diagram (-> (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 100 100)
                    (cmd/fixture-source! "Source1" 50 100)
                    (cmd/fixture-flow-from-source! "Flow1" "Source1" "Stock1")
                    (cmd/set-stock-min! "Stock1" "0")
                    (cmd/set-stock-max! "Stock1" "100")
                    (cmd/set-stock-initial-value! "Stock1" "0")
                    (cmd/set-flow-rate! "Flow1" "10"))
        after-one (simulation/run-steps diagram 1)
        after-five (simulation/run-steps diagram 5)]
    (is (= 1 (get (thermometer/stock-thermometer after-one "Stock1") :fill-width)))
    (is (= 4 (get (thermometer/stock-thermometer after-five "Stock1") :fill-width)))))