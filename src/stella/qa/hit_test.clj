(ns stella.qa.hit-test
  (:require [stella.model :as model])
  (:import [javafx.geometry Bounds]
           [javafx.scene Node Parent]
           [javafx.scene.control Label]
           [javafx.scene.layout BorderPane Pane]
           [javafx.stage Stage]))

(defn- find-by-id [^Node node id]
  (cond
    (= id (.getId node)) node
    (instance? Parent node)
    (some #(find-by-id % id) (.getChildrenUnmodifiable ^Parent node))
    :else nil))

(defn- canvas-node [^Stage stage]
  (or (some-> stage .getScene .getRoot (find-by-id "canvas"))
      (when-let [root (some-> stage .getScene .getRoot)]
        (when (instance? BorderPane root)
          (let [center (.getCenter ^BorderPane root)]
            (when (instance? Pane center) center))))))

(defn region-node
  [^Stage stage region]
  (case region
    :canvas (canvas-node stage)
    nil))

(defn region-bounds
  [^Stage stage region]
  (when-let [^Node node (region-node stage region)]
    (let [^Bounds local (.getBoundsInParent node)
          ^Bounds screen (.localToScreen node (.getBoundsInLocal node))]
      {:x (.getMinX screen)
       :y (.getMinY screen)
       :width (.getWidth local)
       :height (.getHeight local)})))

(defn wait-for-region-bounds
  [^Stage stage region attempts]
  (loop [n attempts]
    (if (region-node stage region)
      (or (region-bounds stage region) {:width 1 :height 1})
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn region-center
  [^Stage stage region]
  (when-let [{:keys [x y width height]} (region-bounds stage region)]
    {:x (+ x (/ width 2.0))
     :y (+ y (/ height 2.0))}))

(defn label-texts
  [^Node node]
  (cond
    (instance? Label node)
    [(.getText ^Label node)]

    (instance? Parent node)
    (mapcat label-texts (.getChildrenUnmodifiable ^Parent node))

    :else []))

(defn visible-text
  [^Stage stage]
  (set (label-texts (.getRoot (.getScene stage)))))

(defn- link-target
  [from-pos to-pos width height]
  (let [[fx fy] from-pos
        [tx ty] to-pos
        mid-x (/ (+ fx tx) 2.0)
        mid-y (/ (+ fy ty) 2.0)]
    {:x mid-x :y mid-y :w width :h height}))

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