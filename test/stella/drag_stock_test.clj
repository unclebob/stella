(ns stella.drag-stock-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-stock []
  (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 100 100))

(deftest move-stock-test
  (let [diagram (cmd/move-stock! (diagram-with-stock) "Stock1" 250 180)]
    (is (= [250 180] (model/stock-position diagram "Stock1")))))

(deftest move-stock-preserves-count-test
  (let [diagram (cmd/move-stock! (diagram-with-stock) "Stock1" 200 150)]
    (is (= 1 (model/stock-count diagram)))
    (is (model/stock-exists? diagram "Stock1"))))

(deftest move-stock-preserves-flow-endpoints-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/move-stock! "Stock1" 50 80))]
    (is (= [50 80] (model/stock-position diagram "Stock1")))
    (is (= ["Stock1" "Stock2"] (model/flow-endpoints diagram "Flow1")))))

(deftest move-one-stock-leaves-other-untouched-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/move-stock! "Stock1" 120 90))]
    (is (= [120 90] (model/stock-position diagram "Stock1")))
    (is (= [300 200] (model/stock-position diagram "Stock2")))))

(deftest move-missing-stock-no-op-test
  (let [before (diagram-with-stock)
        after (cmd/move-stock! before "Missing" 1 2)]
    (is (= before after))))