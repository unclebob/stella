(ns stella.connector-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(defn- diagram-with-fixtures []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 200 150)
      (model/fixture-stock "Stock2" 350 150)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/fixture-converter "Converter1" 100 250)))

(deftest converter-to-flow-connector-test
  (let [diagram (-> (diagram-with-fixtures)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :converter "Converter1")
                    (model/connect-connector :flow "Flow1"))]
    (is (model/connector-exists? diagram "Connector1"))
    (is (= {:kind :converter :id "Converter1"} (model/connector-from diagram "Connector1")))
    (is (= {:kind :flow :id "Flow1"} (model/connector-to diagram "Connector1")))))

(deftest stock-to-converter-connector-test
  (let [diagram (-> (diagram-with-fixtures)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :stock "Stock1")
                    (model/connect-connector :converter "Converter1"))]
    (is (model/connector-exists? diagram "Connector1"))
    (is (= {:kind :stock :id "Stock1"} (model/connector-from diagram "Connector1")))
    (is (= {:kind :converter :id "Converter1"} (model/connector-to diagram "Connector1")))))

(deftest reject-flow-as-origin-test
  (let [diagram (-> (diagram-with-fixtures)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :flow "Flow1"))]
    (is (zero? (model/connector-count diagram)))
    (is (model/connector-placement-armed? diagram))))

(deftest reject-source-as-destination-test
  (let [diagram (-> (diagram-with-fixtures)
                    (model/fixture-source "Source1" 50 150)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :converter "Converter1")
                    (model/connect-connector :source "Source1"))]
    (is (zero? (model/connector-count diagram)))))

(deftest reject-invalid-connector-pair-test
  (let [diagram (-> (diagram-with-fixtures)
                    (model/arm-connector-placement)
                    (model/select-connector-origin :stock "Stock1")
                    (model/connect-connector :flow "Flow1"))]
    (is (zero? (model/connector-count diagram)))
    (is (nil? (:connector-draft diagram)))))