(ns stella.edit-stock-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-stock []
  (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 200 150))

(deftest new-stock-has-default-bounds-test
  (let [diagram (diagram-with-stock)]
    (is (= "0" (model/stock-min-value diagram "Stock1")))
    (is (nil? (model/stock-max-value diagram "Stock1")))))

(deftest rename-stock-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-name! "Stock1" "Cats"))]
    (is (not (model/stock-exists? diagram "Stock1")))
    (is (model/stock-exists? diagram "Cats"))
    (is (= "0" (model/stock-initial-value diagram "Cats")))))

(deftest reject-duplicate-stock-name-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/set-stock-name! "Stock1" "Stock2"))]
    (is (model/stock-exists? diagram "Stock1"))
    (is (model/stock-exists? diagram "Stock2"))))

(deftest set-stock-initial-value-test
  (let [diagram (cmd/set-stock-initial-value! (diagram-with-stock) "Stock1" "25")]
    (is (= "25" (model/stock-initial-value diagram "Stock1")))))

(deftest reject-initial-below-minimum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-min! "Stock1" "10")
                    (cmd/set-stock-initial-value! "Stock1" "5"))]
    (is (= "0" (model/stock-initial-value diagram "Stock1")))))

(deftest reject-initial-above-maximum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-max! "Stock1" "40")
                    (cmd/set-stock-initial-value! "Stock1" "50"))]
    (is (= "0" (model/stock-initial-value diagram "Stock1")))))

(deftest set-stock-bounds-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-min! "Stock1" "0")
                    (cmd/set-stock-max! "Stock1" "100"))]
    (is (= "0" (model/stock-min-value diagram "Stock1")))
    (is (= "100" (model/stock-max-value diagram "Stock1")))))

(deftest clear-stock-maximum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-max! "Stock1" "50")
                    (cmd/clear-stock-max! "Stock1"))]
    (is (nil? (model/stock-max-value diagram "Stock1")))))