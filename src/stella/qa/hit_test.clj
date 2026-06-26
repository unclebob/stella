(ns stella.qa.hit-test
  (:require [stella.model :as model]))

(defn stock-targets
  "Returns semantic hit-test targets for stocks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/stocks diagram)]
          [[:stock name] {:x x :y :y :w 80 :h 50}])))

(defn flow-targets
  "Returns semantic hit-test targets for flows on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name from-stock to-stock]} (model/flows diagram)
              :let [from (model/stock-position diagram from-stock)
                    to (model/stock-position diagram to-stock)]
              :when (and from to)
              :let [[fx fy] from
                    [tx ty] to
                    mid-x (/ (+ fx tx 80) 2.0)
                    mid-y (/ (+ fy ty 50) 2.0)]]
          [[:flow name] {:x mid-x :y mid-y :w 60 :h 30}])))