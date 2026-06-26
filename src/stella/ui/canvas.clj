(ns stella.ui.canvas
  (:require [stella.events :as events]
            [stella.model :as model]))

(defn- canvas-pane [{:keys [style id on-mouse-clicked children]}]
  (cond-> {:fx/type :pane
           :id (or id "canvas")
           :style style}
    on-mouse-clicked (assoc :on-mouse-clicked on-mouse-clicked)
    children (assoc :children children)))

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(defn- flow-desc
  [diagram {:keys [name rate from to]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (model/endpoint-anchor from-pos (:kind from) :right)
            [end-x end-y] (model/endpoint-anchor to-pos (:kind to) :left)
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)]
        (cond-> {:fx/type :group
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
                                        {:fx/type :label :text rate}]}]}
          (model/endpoint-clickable? diagram :flow)
          (assoc :on-mouse-clicked (endpoint-click :flow name)))))))

(defn- connector-desc
  [diagram {:keys [name from to]}]
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
                     :stroke "#666"
                     :stroke-width 1}
                    {:fx/type :label
                     :layout-x (- mid-x 30)
                     :layout-y (- mid-y 10)
                     :text name
                     :style "-fx-font-size: 10px;"}]}))))

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
    (model/endpoint-clickable? diagram :stock)
    (assoc :on-mouse-clicked (endpoint-click :stock name))))

(defn- converter-desc
  [diagram {:keys [name value x y]}]
  (cond-> {:fx/type :group
           :layout-x x
           :layout-y y
           :children [{:fx/type :circle
                       :center-x 25
                       :center-y 25
                       :radius 25
                       :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                      {:fx/type :vbox
                       :layout-x 5
                       :layout-y 12
                       :spacing 2
                       :children [{:fx/type :label :text name}
                                  {:fx/type :label :text value}]}]}
    (model/endpoint-clickable? diagram :converter)
    (assoc :on-mouse-clicked (endpoint-click :converter name))))

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
    (model/endpoint-clickable? diagram kind)
    (assoc :on-mouse-clicked (endpoint-click kind name))))

(defn canvas-desc
  [shell]
  (let [diagram (:diagram shell)
        connector-nodes (keep #(connector-desc diagram %) (model/connectors diagram))
        flow-nodes (keep #(flow-desc diagram %) (model/flows diagram))
        source-nodes (mapv #(cloud-desc diagram :source %) (model/sources diagram))
        sink-nodes (mapv #(cloud-desc diagram :sink %) (model/sinks diagram))
        converter-nodes (mapv #(converter-desc diagram %) (model/converters diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))
        children (into (vec connector-nodes)
                       (concat flow-nodes source-nodes sink-nodes converter-nodes stock-nodes))]
    (cond-> (canvas-pane {:id "canvas"
                          :style "-fx-background-color: #f5f5f5;"
                          :children children})
      (#{:stock :source :sink :converter} (:placement-mode diagram))
      (assoc :on-mouse-clicked {:event events/canvas-click})
      true (assoc :vgrow :always :hgrow :always))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T17:11:21.583692-05:00", :module-hash "816607009", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-465839271"} {:id "defn-/canvas-pane", :kind "defn-", :line 5, :end-line 10, :hash "-731199666"} {:id "defn-/endpoint-click", :kind "defn-", :line 12, :end-line 14, :hash "-1042549141"} {:id "defn-/flow-desc", :kind "defn-", :line 16, :end-line 39, :hash "-644359831"} {:id "defn-/connector-desc", :kind "defn-", :line 41, :end-line 61, :hash "366913679"} {:id "defn-/stock-desc", :kind "defn-", :line 63, :end-line 79, :hash "1971088292"} {:id "defn-/converter-desc", :kind "defn-", :line 81, :end-line 98, :hash "-23692744"} {:id "defn-/cloud-desc", :kind "defn-", :line 100, :end-line 116, :hash "735845704"} {:id "defn/canvas-desc", :kind "defn", :line 118, :end-line 134, :hash "-1822371234"}]}
;; clj-mutate-manifest-end
