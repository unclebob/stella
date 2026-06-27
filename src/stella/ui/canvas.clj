(ns stella.ui.canvas
  (:require [clojure.string :as str]
            [stella.events :as events]
            [stella.model :as model]))

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(def ^:private flow-pipe-stroke-width 8)
(def ^:private connector-stroke-width 1)

(defn- unit-vector
  [dx dy]
  (let [len (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (if (zero? len)
      [0.0 0.0]
      [(/ dx len) (/ dy len)])))

(defn- flow-arrowhead
  [tip-x tip-y base-x base-y]
  (let [[ux uy] (unit-vector (- tip-x base-x) (- tip-y base-y))
        half 6.0
        perp-x (- uy)
        perp-y ux
        left-x (+ base-x (* perp-x half))
        left-y (+ base-y (* perp-y half))
        right-x (- base-x (* perp-x half))
        right-y (- base-y (* perp-y half))]
    {:fx/type :polygon
     :points [tip-x tip-y left-x left-y right-x right-y]
     :fill "#333"}))

(defn- flow-pipe-body
  [start-x start-y end-x end-y]
  (let [[ux uy] (unit-vector (- end-x start-x) (- end-y start-y))
        arrow-size 12.0
        pipe-end-x (- end-x (* ux arrow-size))
        pipe-end-y (- end-y (* uy arrow-size))]
    {:line {:fx/type :line
            :start-x start-x
            :start-y start-y
            :end-x pipe-end-x
            :end-y pipe-end-y
            :stroke "#333"
            :stroke-width flow-pipe-stroke-width
            :stroke-line-cap :round}
     :arrow (flow-arrowhead end-x end-y pipe-end-x pipe-end-y)}))

(defn- flow-desc
  [diagram {:keys [name rate from to]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (model/endpoint-anchor from-pos (:kind from) :right)
            [end-x end-y] (model/endpoint-anchor to-pos (:kind to) :left)
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)
            {:keys [line arrow]} (flow-pipe-body start-x start-y end-x end-y)]
        (cond-> {:fx/type :group
                 :id (str "flow-" name)
                 :children [line
                            arrow
                            {:fx/type :v-box
                             :layout-x (- mid-x 30)
                             :layout-y (- mid-y 20)
                             :spacing 2
                             :children [{:fx/type :label :text name}
                                        {:fx/type :label :text (str rate)}]}]}
          (model/endpoint-clickable? diagram :flow)
          (assoc :on-mouse-clicked (endpoint-click :flow name)))))))

(def ^:private connector-label-style "-fx-font-size: 10px;")
(def ^:private connector-formula-style "-fx-font-size: 9px;")

(defn connector-canvas-labels
  [diagram connector-name]
  (when-let [connector (first (filter #(= connector-name (:name %))
                                      (model/connectors diagram)))]
    (when (and (model/endpoint-position diagram (:from connector))
               (model/endpoint-position diagram (:to connector)))
      (cond-> {:name (:name connector)}
        (seq (:formula connector)) (assoc :formula (:formula connector))))))

(defn- connector-desc
  [diagram {:keys [name from to formula]}]
  (when-let [from-pos (model/endpoint-position diagram from)]
    (when-let [to-pos (model/endpoint-position diagram to)]
      (let [[start-x start-y] (model/endpoint-anchor from-pos (:kind from) :right)
            [end-x end-y] (model/endpoint-anchor to-pos (:kind to) :left)
            mid-x (/ (+ start-x end-x) 2.0)
            mid-y (/ (+ start-y end-y) 2.0)
            label-children (into [{:fx/type :label :text name :style connector-label-style}]
                                 (when (seq formula)
                                   [{:fx/type :label
                                     :text formula
                                     :style connector-formula-style}]))]
        {:fx/type :group
         :id (str "connector-" name)
         :children [{:fx/type :line
                     :start-x start-x
                     :start-y start-y
                     :end-x end-x
                     :end-y end-y
                     :stroke "#666"
                     :stroke-width connector-stroke-width}
                    {:fx/type :v-box
                     :layout-x (- mid-x 30)
                     :layout-y (- mid-y 15)
                     :spacing 2
                     :children label-children}]}))))

(defn converter-canvas-labels
  [diagram converter-name]
  (when-let [{:keys [name]} (first (filter #(= converter-name (:name %))
                                            (model/converters diagram)))]
    {:name name}))

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
  [diagram {:keys [name x y]}]
  (cond-> {:fx/type :group
           :id (str "converter-" name)
           :layout-x x
           :layout-y y
           :children [{:fx/type :circle
                       :center-x 25
                       :center-y 25
                       :radius 25
                       :style "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;"}
                      {:fx/type :label
                       :layout-x 8
                       :layout-y 18
                       :text name}]}
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
;; {:version 1, :tested-at "2026-06-27T10:17:42.187541-05:00", :module-hash "2061787865", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "477415667"} {:id "defn-/endpoint-click", :kind "defn-", :line 6, :end-line 8, :hash "-1042549141"} {:id "def/flow-pipe-stroke-width", :kind "def", :line 10, :end-line 10, :hash "-936806796"} {:id "def/connector-stroke-width", :kind "def", :line 11, :end-line 11, :hash "-1722330092"} {:id "defn-/unit-vector", :kind "defn-", :line 13, :end-line 18, :hash "-518215995"} {:id "defn-/flow-arrowhead", :kind "defn-", :line 20, :end-line 32, :hash "-1459538016"} {:id "defn-/flow-pipe-body", :kind "defn-", :line 34, :end-line 48, :hash "312484768"} {:id "defn-/flow-desc", :kind "defn-", :line 50, :end-line 70, :hash "-1028093553"} {:id "def/connector-label-style", :kind "def", :line 72, :end-line 72, :hash "-514178943"} {:id "def/connector-formula-style", :kind "def", :line 73, :end-line 73, :hash "959198280"} {:id "defn/connector-canvas-labels", :kind "defn", :line 75, :end-line 82, :hash "-1053159609"} {:id "defn-/connector-desc", :kind "defn-", :line 84, :end-line 110, :hash "-2114511346"} {:id "defn/converter-canvas-labels", :kind "defn", :line 112, :end-line 116, :hash "-887333482"} {:id "def/bound-label-style", :kind "def", :line 118, :end-line 118, :hash "-2104208887"} {:id "defn/stock-icon-labels", :kind "defn", :line 120, :end-line 124, :hash "-2139438638"} {:id "defn/stock-canvas-labels", :kind "defn", :line 126, :end-line 131, :hash "1666309085"} {:id "defn/flow-icon-labels", :kind "defn", :line 133, :end-line 136, :hash "860011591"} {:id "defn-/flow-on-canvas", :kind "defn-", :line 138, :end-line 145, :hash "-1857371978"} {:id "defn/flow-canvas-labels", :kind "defn", :line 147, :end-line 149, :hash "-1659873221"} {:id "defn-/stock-desc", :kind "defn-", :line 151, :end-line 179, :hash "-1787538790"} {:id "defn-/converter-desc", :kind "defn-", :line 181, :end-line 197, :hash "399943550"} {:id "defn-/cloud-desc", :kind "defn-", :line 199, :end-line 216, :hash "273299004"} {:id "defn-/overlay-segment", :kind "defn-", :line 218, :end-line 220, :hash "-1910896273"} {:id "defn/diagram-overlay-text", :kind "defn", :line 222, :end-line 235, :hash "1103884749"} {:id "defn-/canvas-background", :kind "defn-", :line 237, :end-line 242, :hash "538381286"} {:id "defn/canvas-stack", :kind "defn", :line 244, :end-line 260, :hash "937153481"} {:id "defn/canvas-desc", :kind "defn", :line 262, :end-line 264, :hash "-772167057"}]}
;; clj-mutate-manifest-end
