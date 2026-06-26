(ns stella.flow-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(defn- diagram-with-stocks []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)))

(deftest connect-flow-test
  (let [diagram (-> (diagram-with-stocks)
                    (model/arm-flow-placement)
                    (model/select-flow-source "Stock1")
                    (model/connect-flow "Stock2"))]
    (is (model/flow-exists? diagram "Flow1"))
    (is (= ["Stock1" "Stock2"] (model/flow-endpoints diagram "Flow1")))
    (is (= "0" (model/flow-rate diagram "Flow1")))
    (is (model/placement-disarmed? diagram))))

(deftest same-stock-rejected-test
  (let [diagram (-> (diagram-with-stocks)
                    (model/arm-flow-placement)
                    (model/select-flow-source "Stock1")
                    (model/connect-flow "Stock1"))]
    (is (zero? (model/flow-count diagram)))))

(deftest arm-without-select-test
  (is (zero? (model/flow-count (model/arm-flow-placement (diagram-with-stocks))))))