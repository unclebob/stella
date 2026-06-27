(ns stella.drag-stock-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-stock []
  (model/fixture-stock (model/default-diagram) "Stock1" 100 100))

(defn- diagram-with-two-stocks-and-flow []
  (-> (diagram-with-stock)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")))

(def ^:private coord-gen
  (gen/tuple (gen/large-integer* {:min -500 :max 500})
             (gen/large-integer* {:min -500 :max 500})))

(defspec move-updates-coordinates
  50
  (for-all [[x y] coord-gen]
    (= [x y]
       (model/stock-position
        (model/move-stock (diagram-with-stock) "Stock1" x y)
        "Stock1"))))

(defspec move-preserves-stock-count
  50
  (for-all [[x y] coord-gen]
    (= 1
       (model/stock-count
        (model/move-stock (diagram-with-stock) "Stock1" x y)))))

(defspec move-missing-stock-is-noop
  50
  (for-all [[x y] coord-gen]
    (let [diagram (diagram-with-stock)]
      (= diagram (model/move-stock diagram "Missing" x y)))))

(defspec move-preserves-flow-endpoints
  25
  (for-all [[x y] coord-gen]
    (= ["Stock1" "Stock2"]
       (model/flow-endpoints
        (model/move-stock (diagram-with-two-stocks-and-flow) "Stock1" x y)
        "Flow1"))))

(defspec move-one-stock-leaves-other-untouched
  25
  (for-all [[x y] coord-gen]
    (let [diagram (diagram-with-two-stocks-and-flow)
          moved (model/move-stock diagram "Stock1" x y)]
      (= [300 200] (model/stock-position moved "Stock2")))))

(deftest move-preserves-stock-attributes
  (let [diagram (-> (diagram-with-stock)
                    (model/set-stock-min "Stock1" "5")
                    (model/set-stock-max "Stock1" "50")
                    (model/set-stock-initial-value "Stock1" "10")
                    (model/move-stock "Stock1" 200 150))]
    (is (= "10" (model/stock-initial-value diagram "Stock1")))
    (is (= "5" (model/stock-min-value diagram "Stock1")))
    (is (= "50" (model/stock-max-value diagram "Stock1")))))