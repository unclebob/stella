(ns stella.ui.canvas
  (:require [clojure.string :as str]
            [stella.events :as events]
            [stella.model :as model]))

(defn- flow-desc
  [diagram {:keys [name rate from to]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (model/endpoint-anchor from-pos (:kind from) :right)
            [end-x end-y] (model/endpoint-anchor to-pos (:kind to) :left)
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)]
        {:fx/type :group
         :id (str "flow-" name)
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
                                {:fx/type :label :text (str rate)}]}]}))))

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(defn- stock-desc
  [diagram {:keys [name initial-value x y]}]
  (cond-> {:fx/type :group
           :id (str "stock-" name)
           :children [{:fx/type :label :layout-x x :layout-y y :text name}
                      {:fx/type :label
                       :layout-x x
                       :layout-y (+ y 16)
                       :text (str initial-value)}]}
    (= :flow (:placement-mode diagram))
    (assoc :on-mouse-clicked (endpoint-click :stock name))))

(defn- cloud-desc
  [diagram kind {:keys [name x y]}]
  (cond-> {:fx/type :group
           :id (str (name kind) "-" name)
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

(defn diagram-overlay-text
  [diagram]
  (let [stocks (for [{:keys [name initial-value]} (model/stocks diagram)]
                (str name " " initial-value))
        flows (for [{:keys [name rate]} (model/flows diagram)]
                (str name " " rate))
        sources (for [{:keys [name]} (model/sources diagram)] name)
        sinks (for [{:keys [name]} (model/sinks diagram)] name)]
    (str (str/join " | " stocks)
         (when (seq flows) (str " || " (str/join " | " flows)))
         (when (seq sources) (str " || " (str/join " | " sources)))
         (when (seq sinks) (str " || " (str/join " | " sinks))))))

(defn- canvas-background []
  {:fx/type :rectangle
   :id "canvas-bg"
   :width 4000
   :height 4000
   :style "-fx-fill: #f5f5f5;"})

(defn canvas-stack
  [shell]
  (let [diagram (:diagram shell)
        flow-nodes (vec (keep #(flow-desc diagram %) (model/flows diagram)))
        source-nodes (mapv #(cloud-desc diagram :source %) (model/sources diagram))
        sink-nodes (mapv #(cloud-desc diagram :sink %) (model/sinks diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))
        diagram-nodes (into flow-nodes (concat source-nodes sink-nodes stock-nodes))
        children (into [(canvas-background)] diagram-nodes)]
    {:fx/type :stack-pane
     :id "canvas"
     :on-mouse-clicked {:event events/canvas-click}
     :children children}))

(defn canvas-desc
  [shell]
  (canvas-stack shell))