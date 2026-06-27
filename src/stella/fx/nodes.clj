(ns stella.fx.nodes
  (:import [javafx.scene Node Parent]
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