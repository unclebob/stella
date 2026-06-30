(ns stella.simulation-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]
            [stella.simulation :as simulation]))

(defn- stock-flow-diagram []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/set-stock-initial-value "Stock1" "100")
      (model/set-stock-initial-value "Stock2" "0")
      (model/set-flow-rate "Flow1" "10")))

(def ^:private step-count-gen
  (gen/large-integer* {:min 1 :max 20}))

(defspec run-steps-matches-repeated-step
  25
  (for-all [steps step-count-gen]
    (let [diagram (stock-flow-diagram)
          via-run (simulation/run-steps diagram steps)
          via-step (reduce (fn [d _] (simulation/step d)) diagram (range steps))]
      (= (get-in via-run [:simulation :stock-values])
         (get-in via-step [:simulation :stock-values])))))

(defspec step-advances-time-by-dt
  25
  (for-all [steps step-count-gen]
    (let [diagram (stock-flow-diagram)
          after (simulation/run-steps diagram steps)
          expected (/ (Math/round (* steps 0.1 10)) 10.0)]
      (= expected (simulation/simulation-time after)))))

(defspec stock-to-stock-conserves-total
  25
  (for-all [steps step-count-gen]
    (let [diagram (stock-flow-diagram)
          after (simulation/run-steps diagram steps)
          total (+ (Double/parseDouble (simulation/stock-value after "Stock1"))
                   (Double/parseDouble (simulation/stock-value after "Stock2")))]
      (= 100.0 total))))

(defspec zero-rate-preserves-stock-values
  25
  (for-all [steps step-count-gen]
    (let [diagram (-> (stock-flow-diagram)
                      (model/set-flow-rate "Flow1" "0"))
          after (simulation/run-steps diagram steps)]
      (and (= "100" (simulation/stock-value after "Stock1"))
           (= "0" (simulation/stock-value after "Stock2"))))))

(defspec default-shell-shows-zero-time
  25
  (prop/for-all [_ gen/int]
    (= "0" (model/simulation-time-display (model/default-shell)))))

(deftest stock-value-falls-back-to-initial
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 100 100)
                    (model/set-stock-initial-value "Stock1" "17"))]
    (is (= "17" (simulation/stock-value diagram "Stock1")))))