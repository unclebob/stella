(ns stella.delete-selection-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- two-stocks []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)))

(defn- stocks-with-flow []
  (-> (two-stocks)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")))

(defn- flow-with-connector []
  (-> (stocks-with-flow)
      (model/fixture-converter "Converter1" 100 250)
      (model/fixture-connector "Connector1" "Converter1" "Flow1")))

(defspec delete-empty-selection-is-noop
  25
  (for-all [_ gen/int]
    (let [diagram (two-stocks)]
      (= diagram (model/delete-selection diagram)))))

(defspec delete-selected-stock-removes-it
  25
  (for-all [_ gen/int]
    (let [diagram (-> (two-stocks)
                      (model/click-select :stock "Stock1")
                      (model/delete-selection))]
      (and (not (model/stock-exists? diagram "Stock1"))
           (model/stock-exists? diagram "Stock2")
           (model/nothing-selected? diagram)))))

(defspec delete-stock-cascades-to-flows
  25
  (for-all [_ gen/int]
    (let [diagram (-> (stocks-with-flow)
                      (model/click-select :stock "Stock1")
                      (model/delete-selection))]
      (and (not (model/flow-exists? diagram "Flow1"))
           (model/stock-exists? diagram "Stock2")))))

(defspec delete-flow-cascades-to-connectors
  25
  (for-all [_ gen/int]
    (let [diagram (-> (flow-with-connector)
                      (model/click-select :flow "Flow1")
                      (model/delete-selection))]
      (and (not (model/flow-exists? diagram "Flow1"))
           (not (model/connector-exists? diagram "Connector1"))
           (model/converter-exists? diagram "Converter1")))))

(defspec delete-multiple-selection-removes-all
  25
  (for-all [_ gen/int]
    (let [diagram (-> (stocks-with-flow)
                      (model/click-select :stock "Stock1")
                      (model/shift-click-select :stock "Stock2")
                      (model/delete-selection))]
      (and (zero? (model/stock-count diagram))
           (not (model/flow-exists? diagram "Flow1"))
           (model/nothing-selected? diagram)))))

(defspec delete-disabled-when-placement-armed
  25
  (for-all [mode (gen/elements [:stock :flow :converter])]
    (let [diagram (-> (two-stocks)
                      (model/click-select :stock "Stock1"))]
      (= 2
         (model/stock-count
          (model/delete-selection
           (case mode
             :stock (model/arm-stock-placement diagram)
             :flow (model/arm-flow-placement diagram)
             :converter (model/arm-converter-placement diagram))))))))

(deftest delete-converter-removes-stock-connector
  (let [diagram (-> (two-stocks)
                    (model/fixture-converter "Converter1" 100 250)
                    (model/fixture-stock-connector "Connector1" "Stock1" "Converter1")
                    (model/click-select :converter "Converter1")
                    (model/delete-selection))]
    (is (not (model/converter-exists? diagram "Converter1")))
    (is (not (model/connector-exists? diagram "Connector1")))
    (is (model/stock-exists? diagram "Stock1"))))