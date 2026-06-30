(ns stella.acceptance.step-handlers-thermometer
  (:require [stella.acceptance.step-support :as support]
            [stella.ui.canvas :as canvas]))

(defn- assert-thermometer-field
  [world stock-name field expected]
  (let [therm (canvas/stock-canvas-thermometer (support/diagram-from world) stock-name)]
    (when-not therm
      (support/fail! (str "stock " stock-name " not on canvas")))
    (let [actual (get therm field)]
      (when-not (= expected actual)
        (support/fail! (str "stock " stock-name " canvas thermometer "
                            (name field) " " actual " expected " expected))))
    world))

(def thermometer-handlers
  [{:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer fill width should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name fill-param] example]
          (assert-thermometer-field world name :fill-width
                                    (support/parse-int (support/require-value example fill-param)
                                                       fill-param)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer fill width should be (\d+)$"
    :fn (fn [world [_ name fill-width] _]
          (assert-thermometer-field world name :fill-width (Integer/parseInt fill-width)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer fill color should be light blue$"
    :fn (fn [world [_ name] _]
          (assert-thermometer-field world name :fill-color "light blue"))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas name should be at top$"
    :fn (fn [world [_ name] _]
          (assert-thermometer-field world name :name-at-top true))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer should be below name$"
    :fn (fn [world [_ name] _]
          (assert-thermometer-field world name :thermometer-below-name true))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer track width should be (\d+)$"
    :fn (fn [world [_ name width] _]
          (assert-thermometer-field world name :track-width (Integer/parseInt width)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas thermometer track height should be (\d+)$"
    :fn (fn [world [_ name height] _]
          (assert-thermometer-field world name :track-height (Integer/parseInt height)))}])