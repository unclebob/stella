(ns stella.delete-selection-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- two-stocks []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 100 100)
      (cmd/fixture-stock! "Stock2" 300 200)))

(deftest delete-selected-stock-test
  (let [diagram (-> (two-stocks)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/delete-selection!))]
    (is (not (model/stock-exists? diagram "Stock1")))
    (is (model/stock-exists? diagram "Stock2"))
    (is (= 1 (model/stock-count diagram)))))

(deftest delete-stock-removes-flows-test
  (let [diagram (-> (two-stocks)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/click-select! :stock "Stock1")
                    (cmd/delete-selection!))]
    (is (not (model/flow-exists? diagram "Flow1")))
    (is (model/stock-exists? diagram "Stock2"))))

(deftest delete-flow-removes-connectors-test
  (let [diagram (-> (two-stocks)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/fixture-connector! "Connector1" "Converter1" "Flow1")
                    (cmd/click-select! :flow "Flow1")
                    (cmd/delete-selection!))]
    (is (not (model/flow-exists? diagram "Flow1")))
    (is (not (model/connector-exists? diagram "Connector1")))
    (is (model/stock-exists? diagram "Stock1"))
    (is (model/converter-exists? diagram "Converter1"))))

(deftest delete-converter-removes-connectors-test
  (let [diagram (-> (two-stocks)
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/fixture-stock-connector! "Connector1" "Stock1" "Converter1")
                    (cmd/click-select! :converter "Converter1")
                    (cmd/delete-selection!))]
    (is (not (model/converter-exists? diagram "Converter1")))
    (is (not (model/connector-exists? diagram "Connector1")))
    (is (model/stock-exists? diagram "Stock1"))))

(deftest delete-multiple-selected-test
  (let [diagram (-> (two-stocks)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/click-select! :stock "Stock1")
                    (cmd/shift-click-select! :stock "Stock2")
                    (cmd/delete-selection!))]
    (is (zero? (model/stock-count diagram)))
    (is (not (model/flow-exists? diagram "Flow1")))))

(deftest delete-empty-selection-no-op-test
  (let [before (two-stocks)
        after (cmd/delete-selection! before)]
    (is (= 2 (model/stock-count after)))
    (is (model/stock-exists? after "Stock1"))
    (is (model/stock-exists? after "Stock2"))))