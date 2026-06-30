(ns stella.fx.nodes
  (:require [clojure.string :as str]
            [stella.thermometer :as thermometer])
  (:import [javafx.scene Group Node Parent]
           [javafx.scene.control Label]
           [javafx.scene.shape Rectangle]
           [javafx.stage Stage Window]))

(defn find-by-id
  [^Node node id]
  (cond
    (= id (.getId node)) node
    (instance? Parent node)
    (some #(find-by-id % id) (.getChildrenUnmodifiable ^Parent node))
    :else nil))

(defn find-child
  [^Node node pred]
  (cond
    (pred node) node
    (instance? Parent node)
    (some #(find-child % pred) (.getChildrenUnmodifiable ^Parent node))
    :else nil))

(defn find-by-id-in-windows
  [id]
  (some (fn [^Window w]
          (when (instance? Stage w)
            (some-> w .getScene .getRoot (find-by-id id))))
        (Window/getWindows)))

(defn- layout-coords-match?
  [^Node node x y]
  (and (= (double x) (.getLayoutX node))
       (= (double y) (.getLayoutY node))))

(defn- light-blue-fill-rectangle?
  [^Node node]
  (and (instance? Rectangle node)
       (let [style (str/lower-case (or (.getStyle ^Rectangle node) ""))]
         (or (str/includes? style "#add8e6")
             (str/includes? style "lightblue")
             (str/includes? style "light blue")))))

(defn- stock-name-label?
  [^Node node stock-name]
  (and (instance? Label node) (= stock-name (.getText ^Label node))))

(defn- converter-name-label?
  [^Node node converter-name]
  (and (instance? Label node) (= converter-name (.getText ^Label node))))

(defn find-stock-group-on-canvas
  ([^Parent canvas stock-x stock-y]
   (find-child canvas #(and (instance? Group %) (layout-coords-match? % stock-x stock-y))))
  ([^Parent canvas stock-name stock-x stock-y]
   (or (find-stock-group-on-canvas canvas stock-x stock-y)
       (find-child canvas
                   #(and (instance? Group %)
                         (boolean (find-child % (fn [n] (stock-name-label? n stock-name)))))))))

(defn find-converter-group-on-canvas
  ([^Parent canvas converter-x converter-y]
   (find-child canvas #(and (instance? Group %) (layout-coords-match? % converter-x converter-y))))
  ([^Parent canvas converter-name converter-x converter-y]
   (or (find-by-id canvas (str "converter-" converter-name))
       (find-child canvas
                   #(and (instance? Group %)
                         (boolean (find-child % (fn [n] (converter-name-label? n converter-name))))))
       (find-converter-group-on-canvas canvas converter-x converter-y))))

(defn find-stock-thermometer-fill-by-name
  [stock-name]
  (find-by-id-in-windows (str "stock-thermometer-fill-" stock-name)))

(defn find-stock-thermometer-fill
  ([^Parent canvas stock-x stock-y]
   (some-> canvas
           (find-stock-group-on-canvas stock-x stock-y)
           (find-child light-blue-fill-rectangle?)))
  ([^Parent canvas stock-name stock-x stock-y]
   (or (find-stock-thermometer-fill-by-name stock-name)
       (some-> canvas
               (find-stock-group-on-canvas stock-name stock-x stock-y)
               (find-child light-blue-fill-rectangle?))
       (find-child canvas
                   #(and (light-blue-fill-rectangle? %)
                         (= (+ stock-x thermometer/track-x) (.getLayoutX %))
                         (= (+ stock-y thermometer/track-y) (.getLayoutY %)))))))