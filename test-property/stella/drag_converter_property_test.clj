(ns stella.drag-converter-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-converter []
  (model/fixture-converter (model/default-diagram) "Converter1" 100 250))

(defn- diagram-with-connector []
  (-> (diagram-with-converter)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/fixture-connector "Connector1" "Converter1" "Flow1")))

(defn- diagram-with-two-converters []
  (-> (diagram-with-converter)
      (model/fixture-converter "Converter2" 300 250)))

(def ^:private coord-gen
  (gen/tuple (gen/large-integer* {:min -500 :max 500})
             (gen/large-integer* {:min -500 :max 500})))

(defspec move-updates-coordinates
  50
  (for-all [[x y] coord-gen]
    (= [x y]
       (model/converter-position
        (model/move-converter (diagram-with-converter) "Converter1" x y)
        "Converter1"))))

(defspec move-preserves-converter-count
  50
  (for-all [[x y] coord-gen]
    (= 1
       (model/converter-count
        (model/move-converter (diagram-with-converter) "Converter1" x y)))))

(defspec move-missing-converter-is-noop
  50
  (for-all [[x y] coord-gen]
    (let [diagram (diagram-with-converter)]
      (= diagram (model/move-converter diagram "Missing" x y)))))

(defspec move-preserves-connector-endpoints
  25
  (for-all [[x y] coord-gen]
    (let [moved (model/move-converter (diagram-with-connector) "Converter1" x y)]
      (and (= {:kind :converter :id "Converter1"}
              (model/connector-from moved "Connector1"))
           (= {:kind :flow :id "Flow1"}
              (model/connector-to moved "Connector1"))))))

(defspec move-one-converter-leaves-other-untouched
  25
  (for-all [[x y] coord-gen]
    (let [diagram (diagram-with-two-converters)
          moved (model/move-converter diagram "Converter1" x y)]
      (= [300 250] (model/converter-position moved "Converter2")))))

(deftest move-preserves-converter-value
  (let [diagram (model/move-converter (diagram-with-converter) "Converter1" 200 150)]
    (is (= "0" (model/converter-value diagram "Converter1")))))