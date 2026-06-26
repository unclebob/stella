(ns stella.diagram-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(deftest default-diagram-test
  (let [diagram (model/default-diagram)]
    (is (empty? (:stocks diagram)))
    (is (= :idle (:placement-mode diagram)))
    (is (= 1 (:next-stock-num diagram)))))

(deftest diagram-queries-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-stock-placement)
                    (model/place-stock 200 150))]
    (is (model/stock-exists? diagram "Stock1"))
    (is (= [200 150] (model/stock-position diagram "Stock1")))
    (is (= "0" (model/stock-initial-value diagram "Stock1")))
    (is (= 1 (model/stock-count diagram)))
    (is (model/placement-disarmed? diagram))))