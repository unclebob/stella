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
    (is (= :stock (:placement-mode diagram)))))

(deftest place-multiple-stocks-without-rearming-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-stock-placement)
                    (model/place-stock 200 150)
                    (model/place-stock 300 200))]
    (is (model/stock-exists? diagram "Stock1"))
    (is (model/stock-exists? diagram "Stock2"))
    (is (= 2 (model/stock-count diagram)))
    (is (= :stock (:placement-mode diagram)))))