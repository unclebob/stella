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

(defn stock-canvas-labels
  [diagram stock-name]
  (some->> (model/stocks diagram)
           (filter #(= stock-name (:name %)))
           first
           ((fn [{:keys [name min-value max-value]}]
              {:name name
               :min (or min-value "0")
               :max max-value
               :value (simulation/stock-value diagram name)}))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T09:56:19.526966-05:00", :module-hash "-1572096981", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "351254847"} {:id "def/track-width", :kind "def", :line 5, :end-line 5, :hash "-1286841508"} {:id "def/track-height", :kind "def", :line 6, :end-line 6, :hash "1224337518"} {:id "def/track-x", :kind "def", :line 7, :end-line 7, :hash "-2123677070"} {:id "def/track-y", :kind "def", :line 8, :end-line 8, :hash "1785653131"} {:id "def/stock-name-y", :kind "def", :line 9, :end-line 9, :hash "-189981225"} {:id "def/fill-color", :kind "def", :line 10, :end-line 10, :hash "-189646120"} {:id "def/unbounded-scale", :kind "def", :line 11, :end-line 11, :hash "940162514"} {:id "defn-/fill-width", :kind "defn-", :line 13, :end-line 23, :hash "-323416382"} {:id "defn/stock-thermometer", :kind "defn", :line 25, :end-line 36, :hash "1140453436"}]}
;; clj-mutate-manifest-end
