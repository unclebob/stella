(ns stella.edit-converter-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- diagram-with-connector []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-flow "Flow1" "Stock1" "Stock2")
      (model/fixture-converter "Converter1" 100 250)
      (model/fixture-connector "Connector1" "Converter1" "Flow1")))

(defn- diagram-with-two-converters []
  (-> (diagram-with-connector)
      (model/fixture-converter "Converter2" 300 250)))

(def ^:private suffix-gen
  (gen/large-integer* {:min 1 :max 9999}))

(def ^:private formula-gen
  (gen/elements ["Stock1 * 0.1" "Stock1 + 5" "Stock2 - 1"]))

(defspec blocked-rename-leaves-diagram-unchanged
  25
  (for-all [_ gen/int]
    (let [diagram (diagram-with-two-converters)
          blocked (model/set-converter-name diagram "Converter1" "Converter2")]
      (and (model/converter-exists? blocked "Converter1")
           (model/converter-exists? blocked "Converter2")
           (= {:kind :converter :id "Converter1"}
              (model/connector-from blocked "Connector1"))))))

(defspec successful-rename-updates-connector-from
  25
  (for-all [suffix suffix-gen]
    (let [new-name (str "Growth" suffix)
          diagram (diagram-with-connector)
          renamed (model/set-converter-name diagram "Converter1" new-name)]
      (and (not (model/converter-exists? renamed "Converter1"))
           (model/converter-exists? renamed new-name)
           (= {:kind :converter :id new-name}
              (model/connector-from renamed "Connector1"))))))

(defspec formula-set-on-connected-connector
  25
  (for-all [formula formula-gen]
    (= formula
       (model/connector-formula
        (model/set-converter-formula (diagram-with-connector) "Converter1" formula)
        "Connector1"))))

(defspec formula-without-connector-is-noop
  25
  (for-all [formula formula-gen]
    (let [diagram (-> (diagram-with-connector)
                      (model/fixture-converter "Converter2" 300 250))]
      (= ""
         (model/connector-formula
          (model/set-converter-formula diagram "Converter2" formula)
          "Connector1")))))

(defspec empty-formula-is-noop
  25
  (for-all [_ gen/int]
    (= ""
       (model/connector-formula
        (model/set-converter-formula (diagram-with-connector) "Converter1" "")
        "Connector1"))))

(deftest new-connector-starts-with-empty-formula
  (is (= "" (model/connector-formula (diagram-with-connector) "Connector1"))))