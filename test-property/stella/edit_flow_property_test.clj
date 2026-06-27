(ns stella.edit-flow-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-flow []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")))

(defn- diagram-with-two-flows []
  (-> (diagram-with-flow)
      (model/fixture-flow "Flow2" "Stock2" "Stock1")))

(def ^:private suffix-gen
  (gen/large-integer* {:min 1 :max 9999}))

(def ^:private rate-gen
  (gen/large-integer* {:min 0 :max 1000}))

(defspec blocked-rename-leaves-diagram-unchanged
  25
  (for-all [_ gen/int]
    (let [diagram (diagram-with-two-flows)
          blocked (model/set-flow-name diagram "Flow1" "Flow2")]
      (and (model/flow-exists? blocked "Flow1")
           (model/flow-exists? blocked "Flow2")
           (= (model/flow-rate diagram "Flow1")
              (model/flow-rate blocked "Flow1"))))))

(defspec successful-rename-preserves-rate
  25
  (for-all [suffix suffix-gen]
    (let [new-name (str "RenamedFlow" suffix)
          diagram (diagram-with-flow)
          renamed (model/set-flow-name diagram "Flow1" new-name)]
      (and (not (model/flow-exists? renamed "Flow1"))
           (model/flow-exists? renamed new-name)
           (= (model/flow-rate diagram "Flow1")
              (model/flow-rate renamed new-name))))))

(defspec valid-numeric-rate-is-set
  25
  (for-all [rate rate-gen]
    (let [diagram (model/set-flow-rate (diagram-with-flow) "Flow1" (str rate))]
      (= (str rate) (model/flow-rate diagram "Flow1")))))

(defspec invalid-rate-is-rejected
  25
  (for-all [_ gen/int]
    (= "0"
       (model/flow-rate
        (model/set-flow-rate (diagram-with-flow) "Flow1" "not-a-number")
        "Flow1"))))

(defspec rename-updates-connector-refs
  25
  (for-all [suffix suffix-gen]
    (let [new-name (str "Drain" suffix)
          diagram (-> (diagram-with-flow)
                      (model/fixture-converter "Converter1" 100 250)
                      (model/arm-connector-placement)
                      (model/select-connector-origin :converter "Converter1")
                      (model/connect-connector :flow "Flow1")
                      (model/set-flow-name "Flow1" new-name))]
      (and (model/flow-exists? diagram new-name)
           (= {:kind :flow :id new-name}
              (model/connector-to diagram "Connector1"))))))

(deftest rename-preserves-endpoints
  (let [diagram (model/set-flow-name (diagram-with-flow) "Flow1" "Drain")]
    (is (= ["Stock1" "Stock2"] (model/flow-endpoints diagram "Drain")))))