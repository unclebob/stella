(ns stella.flow-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-stocks []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)))

(defspec connect-flow-increments-count
  25
  (prop/for-all [_ gen/int]
    (let [diagram (-> (diagram-with-stocks)
                      (model/arm-flow-placement)
                      (model/select-flow-source :stock "Stock1")
                      (model/connect-flow :stock "Stock2"))]
      (= 1 (model/flow-count diagram)))))

(defspec same-stock-connection-is-rejected
  25
  (prop/for-all [_ gen/int]
    (zero? (model/flow-count
            (-> (diagram-with-stocks)
                (model/arm-flow-placement)
                (model/select-flow-source :stock "Stock1")
                (model/connect-flow :stock "Stock1"))))))

(defspec successful-connection-disarms-placement
  25
  (prop/for-all [_ gen/int]
    (let [diagram (-> (diagram-with-stocks)
                      (model/arm-flow-placement)
                      (model/select-flow-source :stock "Stock1")
                      (model/connect-flow :stock "Stock2"))]
      (model/flow-placement-disarmed? diagram))))

(defspec flow-endpoints-match-selection
  25
  (prop/for-all [_ gen/int]
    (= ["Stock1" "Stock2"]
       (model/flow-endpoints
        (-> (diagram-with-stocks)
            (model/arm-flow-placement)
            (model/select-flow-source :stock "Stock1")
            (model/connect-flow :stock "Stock2"))
        "Flow1"))))

(deftest arm-flow-clears-draft
  (is (nil? (:flow-draft (model/arm-flow-placement (diagram-with-stocks))))))