(ns stella.edit-stock-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-stock []
  (model/fixture-stock (model/default-diagram) "Stock1" 200 150))

(defn- diagram-with-two-stocks []
  (-> (diagram-with-stock)
      (model/fixture-stock "Stock2" 300 200)))

(def ^:private suffix-gen
  (gen/large-integer* {:min 1 :max 9999}))

(def ^:private bounded-value-gen
  (gen/large-integer* {:min 0 :max 100}))

(defspec blocked-rename-leaves-diagram-unchanged
  25
  (for-all [_ gen/int]
    (let [diagram (diagram-with-two-stocks)
          blocked (model/set-stock-name diagram "Stock1" "Stock2")]
      (and (model/stock-exists? blocked "Stock1")
           (model/stock-exists? blocked "Stock2")
           (= (model/stock-initial-value diagram "Stock1")
              (model/stock-initial-value blocked "Stock1"))))))

(defspec successful-rename-preserves-initial-value
  25
  (for-all [suffix suffix-gen]
    (let [new-name (str "Renamed" suffix)
          diagram (diagram-with-stock)
          renamed (model/set-stock-name diagram "Stock1" new-name)]
      (and (not (model/stock-exists? renamed "Stock1"))
           (model/stock-exists? renamed new-name)
           (= (model/stock-initial-value diagram "Stock1")
              (model/stock-initial-value renamed new-name))))))

(defspec valid-initial-value-within-bounds
  25
  (for-all [value bounded-value-gen]
    (let [diagram (-> (diagram-with-stock)
                      (model/set-stock-min "Stock1" "0")
                      (model/set-stock-max "Stock1" "100")
                      (model/set-stock-initial-value "Stock1" (str value)))]
      (= (str value) (model/stock-initial-value diagram "Stock1")))))

(defspec invalid-initial-below-min-is-rejected
  25
  (for-all [_ gen/int]
    (let [diagram (-> (diagram-with-stock)
                      (model/set-stock-min "Stock1" "10")
                      (model/set-stock-initial-value "Stock1" "5"))]
      (= "0" (model/stock-initial-value diagram "Stock1")))))

(defspec rename-updates-flow-endpoints
  25
  (for-all [suffix suffix-gen]
    (let [new-name (str "FlowStock" suffix)
          diagram (-> (diagram-with-two-stocks)
                      (model/fixture-flow "Flow1" "Stock1" "Stock2")
                      (model/set-stock-name "Stock1" new-name))]
      (= [new-name "Stock2"] (model/flow-endpoints diagram "Flow1")))))

(deftest clear-max-removes-bound
  (let [diagram (-> (diagram-with-stock)
                    (model/set-stock-max "Stock1" "50")
                    (model/clear-stock-max "Stock1"))]
    (is (nil? (model/stock-max-value diagram "Stock1")))))