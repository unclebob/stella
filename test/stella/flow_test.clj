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
                    (model/select-flow-source :stock "Stock1")
                    (model/connect-flow :stock "Stock2"))]
    (is (model/flow-exists? diagram "Flow1"))
    (is (= ["Stock1" "Stock2"] (model/flow-endpoints diagram "Flow1")))
    (is (= "0" (model/flow-rate diagram "Flow1")))
    (is (model/flow-placement-armed? diagram))
    (is (nil? (:flow-draft diagram)))))

(deftest connect-multiple-flows-without-rearming-test
  (let [diagram (-> (diagram-with-stocks)
                    (model/fixture-stock "Stock3" 500 200)
                    (model/arm-flow-placement)
                    (model/select-flow-source :stock "Stock1")
                    (model/connect-flow :stock "Stock2")
                    (model/select-flow-source :stock "Stock2")
                    (model/connect-flow :stock "Stock3"))]
    (is (= 2 (model/flow-count diagram)))
    (is (model/flow-placement-armed? diagram))))

(deftest same-stock-rejected-test
  (let [diagram (-> (diagram-with-stocks)
                    (model/arm-flow-placement)
                    (model/select-flow-source :stock "Stock1")
                    (model/connect-flow :stock "Stock1"))]
    (is (zero? (model/flow-count diagram)))))

(deftest arm-without-select-test
  (is (zero? (model/flow-count (model/arm-flow-placement (diagram-with-stocks))))))