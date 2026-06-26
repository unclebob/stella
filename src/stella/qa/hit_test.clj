(ns stella.qa.hit-test
  (:require [stella.fx.nodes :as fx-nodes]
            [stella.model :as model])
  (:import [javafx.geometry Bounds]
           [javafx.scene Node Parent]
           [javafx.scene.control Label]
            [javafx.scene Group]
            [javafx.scene.layout BorderPane Pane StackPane]
           [javafx.stage Stage]))

(defn- canvas-node [^Stage stage]
  (or (some-> stage .getScene .getRoot (fx-nodes/find-by-id "canvas"))
      (when-let [root (some-> stage .getScene .getRoot)]
        (when (instance? BorderPane root)
          (let [center (.getCenter ^BorderPane root)]
            (cond
              (instance? Group center) center
              (instance? Pane center) center
              (and (instance? StackPane center)
                   (seq (.getChildren ^Parent center)))
              (first (.getChildren ^Parent center))))))))

(defn region-node
  [^Stage stage region]
  (case region
    :canvas (canvas-node stage)
    nil))

(defn region-bounds
  [^Stage stage region]
  (when-let [^Node node (region-node stage region)]
    (let [^Bounds screen (.localToScreen node (.getBoundsInLocal node))]
      {:x (.getMinX screen)
       :y (.getMinY screen)
       :width (.getWidth screen)
       :height (.getHeight screen)})))

(defn wait-for-region-bounds
  [^Stage stage region attempts]
  (loop [n attempts]
    (if (region-node stage region)
      (or (region-bounds stage region) {:width 1 :height 1})
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn bounds-screen-center
  [^Bounds screen]
  {:x (+ (.getMinX screen) (/ (.getWidth screen) 2.0))
   :y (+ (.getMinY screen) (/ (.getHeight screen) 2.0))})

(defn node-screen-center
  [^Node node]
  (bounds-screen-center (.localToScreen node (.getBoundsInLocal node))))

(defn region-center
  [^Stage stage region]
  (when-let [^Node node (region-node stage region)]
    (node-screen-center node)))

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

(defn- find-label-node
  [^Node node text]
  (fx-nodes/find-child
   node
   #(and (instance? Label %) (= text (.getText ^Label %)))))

(defn element-node
  "Finds a diagram element node by matching its primary name label."
  [^Stage stage kind element-name]
  (when-let [root (some-> stage .getScene .getRoot)]
    (or (fx-nodes/find-by-id root (str (name kind) "-" element-name))
        (find-label-node root element-name))))

(defn stock-targets
  "Returns semantic hit-test targets for stocks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/stocks diagram)]
          [[:stock name] {:x x :y y :w 80 :h 50}])))

(defn source-targets
  "Returns semantic hit-test targets for sources on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/sources diagram)]
          [[:source name] {:x x :y y :w 80 :h 50}])))

(defn sink-targets
  "Returns semantic hit-test targets for sinks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/sinks diagram)]
          [[:sink name] {:x x :y y :w 80 :h 50}])))

(defn flow-targets
  "Returns semantic hit-test targets for flows on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name from to]} (model/flows diagram)
              :let [from-pos (model/endpoint-position diagram from)
                    to-pos (model/endpoint-position diagram to)]
              :when (and from-pos to-pos)
              :let [[fx fy] from-pos
                    [tx ty] to-pos
                    mid-x (/ (+ fx tx 80) 2.0)
                    mid-y (/ (+ fy ty 50) 2.0)]]
          [[:flow name] {:x mid-x :y mid-y :w 60 :h 30}])))

(defn- semantic-targets
  [diagram]
  (merge (stock-targets diagram)
         (source-targets diagram)
         (sink-targets diagram)
         (flow-targets diagram)))

(defn- semantic-bounds
  [^Stage stage diagram kind element-name]
  (when (and diagram (= "true" (System/getProperty "stella.qa.semantic-targets")))
    (when-let [{:keys [x y w h]} (get (semantic-targets diagram) [kind element-name])]
      (when-let [^Node canvas (region-node stage :canvas)]
        (let [^Bounds canvas-screen (.localToScreen canvas (.getBoundsInLocal canvas))
              screen-x (+ (.getMinX canvas-screen) x)
              screen-y (+ (.getMinY canvas-screen) y)]
          {:x screen-x
           :y screen-y
           :width (or w 80)
           :height (or h 50)})))))

(defn element-bounds
  ([^Stage stage kind element-name]
   (element-bounds stage nil kind element-name))
  ([^Stage stage diagram kind element-name]
   (or (semantic-bounds stage diagram kind element-name)
      (when-let [^Node label (element-node stage kind element-name)]
        (let [^Node target (or (when-let [p (.getParent label)] p) label)
              ^Bounds screen (.localToScreen target (.getBoundsInLocal target))]
          {:x (.getMinX screen)
           :y (.getMinY screen)
           :width (- (.getMaxX screen) (.getMinX screen))
           :height (- (.getMaxY screen) (.getMinY screen))})))))