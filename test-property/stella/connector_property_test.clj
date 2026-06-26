(ns stella.connector-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-fixtures []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 200 150)
      (model/fixture-stock "Stock2" 350 150)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/fixture-converter "Converter1" 100 250)))

(defspec converter-to-flow-connector-succeeds
  25
  (prop/for-all [_ gen/int]
    (let [diagram (-> (diagram-with-fixtures)
                      (model/arm-connector-placement)
                      (model/select-connector-origin :converter "Converter1")
                      (model/connect-connector :flow "Flow1"))]
      (and (model/connector-exists? diagram "Connector1")
           (= {:kind :converter :id "Converter1"} (model/connector-from diagram "Connector1"))
           (= {:kind :flow :id "Flow1"} (model/connector-to diagram "Connector1"))))))

(defspec flow-cannot-be-connector-origin
  25
  (prop/for-all [_ gen/int]
    (zero? (model/connector-count
            (-> (diagram-with-fixtures)
                (model/arm-connector-placement)
                (model/select-connector-origin :flow "Flow1"))))))

(defspec successful-connector-disarms-placement
  25
  (prop/for-all [_ gen/int]
    (let [diagram (-> (diagram-with-fixtures)
                      (model/arm-connector-placement)
                      (model/select-connector-origin :stock "Stock1")
                      (model/connect-connector :converter "Converter1"))]
      (model/placement-disarmed? diagram))))

(deftest invalid-connector-pair-clears-draft
  (let [diagram (-> (diagram-with-fixtures)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :stock "Stock1")
                    (model/connect-connector :flow "Flow1"))]
    (is (zero? (model/connector-count diagram)))
    (is (nil? (:connector-draft diagram)))))