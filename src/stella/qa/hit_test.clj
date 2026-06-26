(ns stella.qa.hit-test
  (:require [stella.model :as model]))

(defn stock-targets
  "Returns semantic hit-test targets for stocks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/stocks diagram)]
          [[:stock name] {:x x :y :y :w 80 :h 50}])))

(defn source-targets
  "Returns semantic hit-test targets for sources on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/sources diagram)]
          [[:source name] {:x x :y :y :w 80 :h 50}])))

(defn sink-targets
  "Returns semantic hit-test targets for sinks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/sinks diagram)]
          [[:sink name] {:x x :y :y :w 80 :h 50}])))

(defn flow-targets
  "Returns semantic hit-test targets for flows on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name from to]} (model/flows diagram)
              :let [from-pos (case (:kind from)
                               :stock (model/stock-position diagram (:id from))
                               :source (model/source-position diagram (:id from)))
                    to-pos (case (:kind to)
                             :stock (model/stock-position diagram (:id to))
                             :sink (model/sink-position diagram (:id to)))]
              :when (and from-pos to-pos)]
          (let [[fx fy] from-pos
                [tx ty] to-pos
                mid-x (/ (+ fx tx 80) 2.0)
                mid-y (/ (+ fy ty 50) 2.0)]
            [[:flow name] {:x mid-x :y mid-y :w 60 :h 30}])))