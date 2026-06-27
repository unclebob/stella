(ns stella.ui.canvas
  (:require [clojure.string :as str]
            [stella.events :as events]
            [stella.model :as model]))

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
                 :id (str "flow-" name)
                 :children [{:fx/type :line
                             :start-x start-x
                             :start-y start-y
                             :end-x end-x
                             :end-y end-y
                             :stroke "#333"
                             :stroke-width 2}
                            {:fx/type :v-box
                             :layout-x (- mid-x 30)
                             :layout-y (- mid-y 20)
                             :spacing 2
                             :children [{:fx/type :label :text name}
                                        {:fx/type :label :text (str rate)}]}]}
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
         :id (str "connector-" name)
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

(def ^:private bound-label-style "-fx-font-size: 9px;")

(defn stock-icon-labels
  [{:keys [name min-value max-value]}]
  {:name name
   :min (or min-value "0")
   :max max-value})

(defn stock-canvas-labels
  [diagram stock-name]
  (some->> (model/stocks diagram)
           (filter #(= stock-name (:name %)))
           first
           stock-icon-labels))

(defn flow-icon-labels
  [{:keys [name rate]}]
  {:name name
   :rate (str rate)})

(defn- flow-on-canvas
  [diagram flow-name]
  (some->> (model/flows diagram)
          (filter #(= flow-name (:name %)))
          first
          (#(when (and (model/endpoint-position diagram (:from %))
                        (model/endpoint-position diagram (:to %)))
              %))))

(defn flow-canvas-labels
  [diagram flow-name]
  (some-> (flow-on-canvas diagram flow-name) flow-icon-labels))

(defn- stock-desc
  [diagram {:keys [name x y] :as stock}]
  (let [{:keys [name min max]} (stock-icon-labels stock)
        children (into [{:fx/type :rectangle
                         :width 80
                         :height 50
                         :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                        {:fx/type :label
                         :layout-x 20
                         :layout-y 14
                         :text name}
                        {:fx/type :label
                         :layout-x 4
                         :layout-y 36
                         :text min
                         :style bound-label-style}]
                       (when max
                         [{:fx/type :label
                           :layout-x 52
                           :layout-y 36
                           :text max
                           :style bound-label-style}]))]
    (cond-> {:fx/type :group
             :id (str "stock-" name)
             :layout-x x
             :layout-y y
             :children children}
      (model/endpoint-clickable? diagram :stock)
      (assoc :on-mouse-clicked (endpoint-click :stock name)))))

(defn- converter-desc
  [diagram {:keys [name value x y]}]
  (cond-> {:fx/type :group
           :id (str "converter-" name)
           :layout-x x
           :layout-y y
           :children [{:fx/type :circle
                       :center-x 25
                       :center-y 25
                       :radius 25
                       :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                      {:fx/type :v-box
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
           :id (str (clojure.core/name kind) "-" name)
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

(defn- overlay-segment
  [items]
  (when (seq items) (str/join " | " items)))

(defn diagram-overlay-text
  [diagram]
  (->> [(overlay-segment (map (fn [{:keys [name initial-value]}]
                                 (str name " " initial-value))
                               (model/stocks diagram)))
        (overlay-segment (map (fn [{:keys [name rate]}] (str name " " rate))
                              (model/flows diagram)))
        (overlay-segment (map :name (model/sources diagram)))
        (overlay-segment (map :name (model/sinks diagram)))
        (overlay-segment (map (fn [{:keys [name value]}] (str name " " value))
                              (model/converters diagram)))
        (overlay-segment (map :name (model/connectors diagram)))]
       (remove nil?)
       (str/join " || ")))

(defn- canvas-background []
  {:fx/type :rectangle
   :id "canvas-bg"
   :width 4000
   :height 4000
   :style "-fx-fill: #f5f5f5;"})

(defn canvas-stack
  [shell]
  (let [diagram (:diagram shell)
        connector-nodes (vec (keep #(connector-desc diagram %) (model/connectors diagram)))
        flow-nodes (vec (keep #(flow-desc diagram %) (model/flows diagram)))
        source-nodes (mapv #(cloud-desc diagram :source %) (model/sources diagram))
        sink-nodes (mapv #(cloud-desc diagram :sink %) (model/sinks diagram))
        converter-nodes (mapv #(converter-desc diagram %) (model/converters diagram))
        stock-nodes (mapv #(stock-desc diagram %) (model/stocks diagram))
        diagram-nodes (into connector-nodes
                            (concat flow-nodes source-nodes sink-nodes
                                    converter-nodes stock-nodes))
        children (into [(canvas-background)] diagram-nodes)]
    {:fx/type :stack-pane
     :id "canvas"
     :on-mouse-clicked {:event events/canvas-click}
     :children children}))

(defn canvas-desc
  [shell]
  (canvas-stack shell))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T18:02:22.398105-05:00", :module-hash "236193478", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "477415667"} {:id "defn-/endpoint-click", :kind "defn-", :line 6, :end-line 8, :hash "-1042549141"} {:id "defn-/flow-desc", :kind "defn-", :line 10, :end-line 34, :hash "-1620991121"} {:id "defn-/connector-desc", :kind "defn-", :line 36, :end-line 57, :hash "1129973901"} {:id "defn-/stock-desc", :kind "defn-", :line 59, :end-line 76, :hash "-839302979"} {:id "defn-/converter-desc", :kind "defn-", :line 78, :end-line 96, :hash "111892878"} {:id "defn-/cloud-desc", :kind "defn-", :line 98, :end-line 115, :hash "274228855"} {:id "defn-/overlay-segment", :kind "defn-", :line 117, :end-line 119, :hash "-1910896273"} {:id "defn/diagram-overlay-text", :kind "defn", :line 121, :end-line 134, :hash "1103884749"} {:id "defn-/canvas-background", :kind "defn-", :line 136, :end-line 141, :hash "538381286"} {:id "defn/canvas-stack", :kind "defn", :line 143, :end-line 159, :hash "1465581755"} {:id "defn/canvas-desc", :kind "defn", :line 161, :end-line 163, :hash "-772167057"}]}
;; clj-mutate-manifest-end
