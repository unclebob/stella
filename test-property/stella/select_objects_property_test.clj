(ns stella.select-objects-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(defn- base-diagram []
  (-> (model/default-diagram)
      (model/fixture-stock "Stock1" 100 100)
      (model/fixture-stock "Stock2" 300 200)
      (model/fixture-converter "Converter1" 100 250)))

(defspec click-select-toggles
  25
  (for-all [_ gen/int]
    (let [selected (model/click-select (base-diagram) :stock "Stock1")
          toggled (model/click-select selected :stock "Stock1")]
      (and (model/selected? selected :stock "Stock1")
           (model/nothing-selected? toggled)))))

(defspec click-select-replaces-selection
  25
  (for-all [_ gen/int]
    (let [diagram (-> (base-diagram)
                      (model/click-select :stock "Stock1")
                      (model/click-select :stock "Stock2"))]
      (and (not (model/selected? diagram :stock "Stock1"))
           (model/selected? diagram :stock "Stock2")
           (= 1 (model/selection-count diagram))))))

(defspec shift-click-adds-without-removing
  25
  (for-all [_ gen/int]
    (let [diagram (-> (base-diagram)
                      (model/click-select :stock "Stock1")
                      (model/shift-click-select :stock "Stock2"))]
      (and (model/selected? diagram :stock "Stock1")
           (model/selected? diagram :stock "Stock2")
           (= 2 (model/selection-count diagram))))))

(defspec shift-click-removes-from-selection
  25
  (for-all [_ gen/int]
    (let [diagram (-> (base-diagram)
                      (model/click-select :stock "Stock1")
                      (model/shift-click-select :stock "Stock2")
                      (model/shift-click-select :stock "Stock1"))]
      (and (not (model/selected? diagram :stock "Stock1"))
           (model/selected? diagram :stock "Stock2")))))

(defspec clear-selection-empties-set
  25
  (for-all [_ gen/int]
    (model/nothing-selected?
     (model/clear-selection
      (model/click-select (base-diagram) :stock "Stock1")))))

(defspec selection-disabled-when-placement-armed
  25
  (for-all [mode (gen/elements [:stock :flow :converter :connector])]
    (model/nothing-selected?
     (model/click-select
      (case mode
        :stock (model/arm-stock-placement (base-diagram))
        :flow (model/arm-flow-placement (base-diagram))
        :converter (model/arm-converter-placement (base-diagram))
        :connector (model/arm-connector-placement (base-diagram)))
      :stock "Stock1"))))

(defspec click-missing-object-is-noop
  25
  (for-all [_ gen/int]
    (let [diagram (base-diagram)]
      (= diagram (model/click-select diagram :stock "Missing")))))

(deftest marquee-select-includes-intersecting-stock
  (let [diagram (model/marquee-select (base-diagram) 50 50 200 200)]
    (is (model/selected? diagram :stock "Stock1"))
    (is (not (model/selected? diagram :stock "Stock2")))))