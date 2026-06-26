(ns stella.ui.canvas)

(defn- canvas-pane [{:keys [style]}]
  {:fx/type :pane
   :style style})

(defn canvas-desc
  []
  {:fx/type canvas-pane
   :style "-fx-background-color: #f5f5f5;"
   :vgrow :always
   :hgrow :always})