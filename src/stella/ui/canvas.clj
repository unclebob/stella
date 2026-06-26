(ns stella.ui.canvas
  (:require [stella.events :as events]
            [stella.model :as model]))

(defn- canvas-pane [{:keys [style on-mouse-clicked children]}]
  (cond-> {:fx/type :pane
           :style style}
    on-mouse-clicked (assoc :on-mouse-clicked on-mouse-clicked)
    children (assoc :children children)))

(defn- stock-anchor
  [[x y] side]
  (case side
    :right [(+ x 80.0) (+ y 25.0)]
    :left [x (+ y 25.0)]))

(defn- flow-desc
  [diagram {:keys [name rate from-stock to-stock]}]
  (when-let [from-pos (model/stock-position diagram from-stock)]
    (when-let [to-pos (model/stock-position diagram to-stock)]
      (let [[start-x start-y] (stock-anchor from-pos :right)
            [end-x end-y] (stock-anchor to-pos :left)
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)]
        {:fx/type :group
         :children [{:fx/type :line
                     :start-x start-x
                     :start-y start-y
                     :end-x end-x
                     :end-y end-y
                     :stroke "#333"
                     :stroke-width 2}
                    {:fx/type :vbox
                     :layout-x (- mid-x 30)
                     :layout-y (- mid-y 20)
                     :spacing 2
                     :children [{:fx/type :label :text name}
                                {:fx/type :label :text rate}]}]}))))

(defn- stock-desc
  [diagram {:keys [name initial-value x y] :as stock}]
  (cond-> {:fx/type :group
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
                                  {:fx/type :label :text initial-value}]}]}
    (= :flow (:placement-mode diagram))
    (assoc :on-mouse-clicked {:event events/stock-click :stock-name name})))

(defn canvas-desc
  [shell]
  (let [diagram (:diagram shell)
        flow-nodes (keep #(flow-desc diagram %) (model/flows diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))]
    (cond-> {:fx/type canvas-pane
             :style "-fx-background-color: #f5f5f5;"
             :vgrow :always
             :hgrow :always
             :children (into (vec flow-nodes) stock-nodes)}
      (= :stock (:placement-mode diagram))
      (assoc :on-mouse-clicked {:event events/canvas-click}))))