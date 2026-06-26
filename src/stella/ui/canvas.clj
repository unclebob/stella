(ns stella.ui.canvas
  (:require [stella.events :as events]
            [stella.model :as model]))

(defn- canvas-pane [{:keys [style on-mouse-clicked children]}]
  (cond-> {:fx/type :pane
           :style style}
    on-mouse-clicked (assoc :on-mouse-clicked on-mouse-clicked)
    children (assoc :children children)))

(defn- flow-desc
  [diagram {:keys [name rate from to]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (model/endpoint-anchor from-pos (:kind from) :right)
            [end-x end-y] (model/endpoint-anchor to-pos (:kind to) :left)
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

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(defn- stock-desc
  [diagram {:keys [name initial-value x y]}]
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
    (assoc :on-mouse-clicked (endpoint-click :stock name))))

(defn- cloud-desc
  [diagram kind {:keys [name x y]}]
  (cond-> {:fx/type :group
           :layout-x x
           :layout-y y
           :children [{:fx/type :ellipse
                       :center-x 40
                       :center-y 25
                       :radius-x 40
                       :radius-y 25
                       :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                      {:fx/type :label
                       :layout-x 20
                       :layout-y 18
                       :text name}]}
    (= :flow (:placement-mode diagram))
    (assoc :on-mouse-clicked (endpoint-click kind name))))

(defn canvas-desc
  [shell]
  (let [diagram (:diagram shell)
        flow-nodes (keep #(flow-desc diagram %) (model/flows diagram))
        source-nodes (mapv #(cloud-desc diagram :source %) (model/sources diagram))
        sink-nodes (mapv #(cloud-desc diagram :sink %) (model/sinks diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))
        children (into (vec flow-nodes) (concat source-nodes sink-nodes stock-nodes))]
    (cond-> {:fx/type canvas-pane
             :style "-fx-background-color: #f5f5f5;"
             :vgrow :always
             :hgrow :always
             :children children}
      (#{:stock :source :sink} (:placement-mode diagram))
      (assoc :on-mouse-clicked {:event events/canvas-click}))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:44:35.210024-05:00", :module-hash "-433728703", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-465839271"} {:id "defn-/canvas-pane", :kind "defn-", :line 5, :end-line 9, :hash "-939857556"} {:id "defn-/flow-desc", :kind "defn-", :line 11, :end-line 32, :hash "-1927454499"} {:id "defn-/endpoint-click", :kind "defn-", :line 34, :end-line 36, :hash "-1042549141"} {:id "defn-/stock-desc", :kind "defn-", :line 38, :end-line 54, :hash "128931971"} {:id "defn-/cloud-desc", :kind "defn-", :line 56, :end-line 72, :hash "-666234367"} {:id "defn/canvas-desc", :kind "defn", :line 74, :end-line 88, :hash "-393729737"}]}
;; clj-mutate-manifest-end
