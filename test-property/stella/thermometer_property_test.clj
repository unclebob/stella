(ns stella.thermometer-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.commands :as cmd]
            [stella.thermometer :as thermometer]))

(defn- bounded-diagram [value]
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 200 150)
      (cmd/set-stock-min! "Stock1" "0")
      (cmd/set-stock-max! "Stock1" "100")
      (cmd/set-stock-initial-value! "Stock1" (str value))))

(def ^:private bounded-value-gen
  (gen/large-integer* {:min 0 :max 100}))

(defspec bounded-fill-width-stays-within-track
  25
  (for-all [value bounded-value-gen]
    (let [therm (thermometer/stock-thermometer (bounded-diagram value) "Stock1")]
      (and (<= (:fill-width therm) thermometer/track-width)
           (>= (:fill-width therm) 0)))))

(defspec bounded-fill-width-increases-with-value
  25
  (for-all [low bounded-value-gen
            high bounded-value-gen]
    (let [low-val (min low high)
          high-val (max low high)
          low-fill (:fill-width (thermometer/stock-thermometer (bounded-diagram low-val) "Stock1"))
          high-fill (:fill-width (thermometer/stock-thermometer (bounded-diagram high-val) "Stock1"))]
      (<= low-fill high-fill))))

(defspec stock-thermometer-metadata-is-stable
  25
  (for-all [value bounded-value-gen]
    (let [therm (thermometer/stock-thermometer (bounded-diagram value) "Stock1")]
      (and (= thermometer/track-width (:track-width therm))
           (= thermometer/track-height (:track-height therm))
           (= thermometer/fill-color (:fill-color therm))
           (:name-at-top therm)
           (:thermometer-below-name therm)))))

(defspec missing-stock-has-no-thermometer
  25
  (prop/for-all [_ gen/int]
    (nil? (thermometer/stock-thermometer (cmd/default-diagram! nil) "Stock1"))))