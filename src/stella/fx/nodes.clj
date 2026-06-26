(ns stella.fx.nodes
  (:import [javafx.scene Node Parent]))

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