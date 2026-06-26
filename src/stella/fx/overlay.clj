(ns stella.fx.overlay
  (:require [stella.fx.nodes :as fx-nodes]
            [stella.ui.canvas :as canvas])
  (:import [javafx.scene.control Label]
           [javafx.stage Stage Window]))

(defn sync-diagram-overlay!
  [diagram]
  (when-let [^Stage stage (first (filter #(instance? Stage %) (Window/getWindows)))]
    (when-let [^Label overlay (some-> stage .getScene .getRoot (fx-nodes/find-by-id "diagram-overlay"))]
      (.setText overlay (canvas/diagram-overlay-text diagram)))))