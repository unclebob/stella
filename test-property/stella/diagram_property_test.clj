(ns stella.diagram-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(def ^:private coord-gen
  (gen/tuple (gen/large-integer* {:min -500 :max 500})
             (gen/large-integer* {:min -500 :max 500})))

(defspec place-stock-increments-count-when-armed
  50
  (for-all [[x y] coord-gen]
    (let [diagram (-> (model/default-diagram)
                      (model/arm-stock-placement)
                      (model/place-stock x y))]
      (= 1 (model/stock-count diagram)))))

(defspec place-stock-is-noop-when-idle
  50
  (for-all [[x y] coord-gen]
    (= (model/default-diagram)
       (model/place-stock (model/default-diagram) x y))))

(defspec arm-then-place-keeps-placement-armed
  50
  (for-all [[x y] coord-gen]
    (let [diagram (-> (model/default-diagram)
                      (model/arm-stock-placement)
                      (model/place-stock x y))]
      (= :stock (:placement-mode diagram)))))

(defspec placed-stock-retains-coordinates
  50
  (for-all [[x y] coord-gen]
    (let [diagram (-> (model/default-diagram)
                      (model/arm-stock-placement)
                      (model/place-stock x y))]
      (= [x y] (model/stock-position diagram "Stock1")))))

(deftest default-diagram-is-empty
  (is (zero? (model/stock-count (model/default-diagram)))))