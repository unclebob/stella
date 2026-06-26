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

(defn converter-targets
  "Returns semantic hit-test targets for converters on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/converters diagram)]
          [[:converter name] {:x x :y :y :w 50 :h 50}])))

(defn- link-target
  [from-pos to-pos width height]
  (let [[fx fy] from-pos
        [tx ty] to-pos
        mid-x (/ (+ fx tx) 2.0)
        mid-y (/ (+ fy ty) 2.0)]
    {:x mid-x :y mid-y :w width :h height}))

(defn flow-targets
  "Returns semantic hit-test targets for flows on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name from to]} (model/flows diagram)
              :let [from-pos (model/endpoint-position diagram from)
                    to-pos (model/endpoint-position diagram to)]
              :when (and from-pos to-pos)]
          [[:flow name] (link-target from-pos to-pos 60 30)])))

(defn connector-targets
  "Returns semantic hit-test targets for connectors on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name from to]} (model/connectors diagram)
              :let [from-pos (model/endpoint-position diagram from)
                    to-pos (model/endpoint-position diagram to)]
              :when (and from-pos to-pos)]
          [[:connector name] (link-target from-pos to-pos 60 20)])))