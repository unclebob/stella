(ns stella.thermometer
  (:require [stella.model :as model]
            [stella.numbers :as numbers]
            [stella.simulation :as simulation]))

(def track-width 72)
(def track-height 8)
(def track-x 4)
(def track-y 22)
(def stock-name-y 4)
(def fill-color "light blue")
(def ^:private unbounded-scale 100.0)

(defn- fill-width
  [diagram stock-name]
  (let [value (numbers/parse-number (simulation/stock-value diagram stock-name))
        min-value (numbers/parse-number (or (model/stock-min-value diagram stock-name) "0"))
        max-value (if-let [max-v (model/stock-max-value diagram stock-name)]
                    (numbers/parse-number max-v)
                    unbounded-scale)
        scale-range (- max-value min-value)]
    (if (or (<= scale-range 0.0) (<= value min-value))
      0
      (int (Math/round (* (/ (- value min-value) scale-range) track-width))))))

(defn stock-thermometer
  [diagram stock-name]
  (when (model/stock-exists? diagram stock-name)
    {:fill-width (fill-width diagram stock-name)
     :fill-color fill-color
     :track-width track-width
     :track-height track-height
     :name-at-top true
     :thermometer-below-name true
     :track-x track-x
     :name-y stock-name-y
     :thermometer-y track-y}))