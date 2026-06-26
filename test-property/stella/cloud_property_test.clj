(ns stella.cloud-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(def ^:private coord-gen
  (gen/tuple (gen/large-integer* {:min 0 :max 500})
             (gen/large-integer* {:min 0 :max 500})))

(defspec source-placement-disarms
  25
  (for-all [[x y] coord-gen]
    (let [diagram (-> (model/default-diagram)
                      (model/arm-source-placement)
                      (model/place-source x y))]
      (and (model/source-exists? diagram "Source1")
           (model/placement-disarmed? diagram)))))

(defspec sink-placement-disarms
  25
  (for-all [[x y] coord-gen]
    (let [diagram (-> (model/default-diagram)
                      (model/arm-sink-placement)
                      (model/place-sink x y))]
      (and (model/sink-exists? diagram "Sink1")
           (model/placement-disarmed? diagram)))))

(defspec source-to-stock-flow-valid
  25
  (prop/for-all [_ gen/int]
    (let [diagram (-> (model/default-diagram)
                      (model/fixture-stock "Stock1" 200 150)
                      (model/fixture-source "Source1" 50 150)
                      (model/arm-flow-placement)
                      (model/select-flow-source :source "Source1")
                      (model/connect-flow :stock "Stock1"))]
      (and (model/flow-exists? diagram "Flow1")
           (= {:kind :source :id "Source1"} (model/flow-from diagram "Flow1"))
           (= {:kind :stock :id "Stock1"} (model/flow-to diagram "Flow1"))))))

(deftest sink-cannot-be-flow-source
  (is (zero? (model/flow-count
              (-> (model/default-diagram)
                  (model/fixture-sink "Sink1" 400 150)
                  (model/arm-flow-placement)
                  (model/select-flow-source :sink "Sink1"))))))