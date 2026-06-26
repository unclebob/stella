(ns stella.ui.canvas
  (:require [stella.events :as events]
            [stella.model :as model]))

(defn- canvas-pane [{:keys [style on-mouse-clicked children]}]
  (cond-> {:fx/type :pane
           :style style}
    on-mouse-clicked (assoc :on-mouse-clicked on-mouse-clicked)
    children (assoc :children children)))

(defn- stock-desc
  [{:keys [name initial-value x y]}]
  {:fx/type :group
   :layout-x x
   :layout-y y
   :children [{:fx/type :rectangle
               :width 80
               :height 50
               :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
              {:fx/type :vbox
               :layout-x 8
               :layout-y 8
               :spacing 2
               :children [{:fx/type :label :text name}
                          {:fx/type :label :text initial-value}]}]})

(defn canvas-desc
  [shell]
  (let [diagram (:diagram shell)]
    {:fx/type canvas-pane
     :style "-fx-background-color: #f5f5f5;"
     :vgrow :always
     :hgrow :always
     :on-mouse-clicked {:event events/canvas-click}
     :children (mapv stock-desc (model/stocks diagram))}))