(ns stella.converter-flow-rate-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]
            [stella.simulation :as simulation]))

(defn- diagram-with-converter-flow []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/fixture-converter "Converter1" 100 250)
      (model/fixture-connector "Connector1" "Converter1" "Flow1")))

(deftest converter-without-formula-shows-zero-test
  (let [diagram (diagram-with-converter-flow)]
    (is (= "0" (model/converter-value diagram "Converter1")))
    (is (= "0" (model/flow-rate diagram "Flow1")))))

(deftest constant-formula-sets-value-and-flow-rate-test
  (let [diagram (model/set-converter-formula (diagram-with-converter-flow)
                                             "Converter1" "5")]
    (is (= "5" (model/converter-value diagram "Converter1")))
    (is (= "5" (model/flow-rate diagram "Flow1")))))

(deftest stock-reference-formula-test
  (let [diagram (-> (diagram-with-converter-flow)
                    (model/set-stock-initial-value "Stock1" "100")
                    (model/set-converter-formula "Converter1" "Stock1 * 0.1"))]
    (is (= "10" (model/converter-value diagram "Converter1")))
    (is (= "10" (model/flow-rate diagram "Flow1")))))

(deftest reject-invalid-formula-test
  (let [before (diagram-with-converter-flow)
        after (model/set-converter-formula before "Converter1" "Stock1 & 0.1")]
    (is (= before after))
    (is (= "" (model/connector-formula after "Connector1")))))

(deftest simulation-transfers-at-computed-rate-test
  (let [diagram (-> (diagram-with-converter-flow)
                    (model/set-stock-initial-value "Stock1" "100")
                    (model/set-stock-initial-value "Stock2" "0")
                    (model/set-converter-formula "Converter1" "10"))
        after-one (simulation/run-steps diagram 1)]
    (is (= "99" (simulation/stock-value after-one "Stock1")))
    (is (= "1" (simulation/stock-value after-one "Stock2")))))

(deftest computed-rate-tracks-changing-stock-test
  (let [diagram (-> (diagram-with-converter-flow)
                    (model/set-stock-initial-value "Stock1" "100")
                    (model/set-stock-initial-value "Stock2" "0")
                    (model/set-converter-formula "Converter1" "Stock1 * 0.1"))
        after-one (simulation/run-steps diagram 1)]
    (is (= "9.9" (model/converter-value after-one "Converter1")))
    (is (= "9.9" (model/flow-rate after-one "Flow1")))))