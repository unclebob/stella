(ns stella.ui.canvas
  (:require [clojure.string :as str]
            [stella.events :as events]
            [stella.model :as model]))

(defn- endpoint-click
  [kind name]
  {:event events/endpoint-click :endpoint-kind kind :endpoint-name name})

(def flow-pipe-stroke-width 8)
(def connector-stroke-width 1)

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
                            {:fx/type :vbox
                             :layout-x (- mid-x 30)
                             :layout-y (- mid-y 20)
                             :spacing 2
                             :children [{:fx/type :label :text name}
                                        {:fx/type :label :text (str rate)}]}]}
          (model/endpoint-clickable? diagram :flow)
          (assoc :on-mouse-clicked (endpoint-click :flow name))
          :always
          (assoc :on-context-menu-requested
                 {:event events/edit-flow-open :flow-name name}))))))

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
                     :stroke-width connector-stroke-width}
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
      (assoc :on-mouse-clicked (endpoint-click :stock name))
      :always
      (assoc :on-context-menu-requested
             {:event events/edit-stock-open :stock-name name}))))

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
  (->> [(overlay-segment (map (fn [{:keys [name min-value max-value]}]
                                 (str name " "
                                      (or min-value "0")
                                      (when max-value (str " " max-value))))
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
;; {:version 1, :tested-at "2026-06-27T10:13:09.093626-05:00", :module-hash "1416123531", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "477415667"} {:id "defn-/endpoint-click", :kind "defn-", :line 6, :end-line 8, :hash "-1042549141"} {:id "def/flow-pipe-stroke-width", :kind "def", :line 10, :end-line 10, :hash "-936806796"} {:id "def/connector-stroke-width", :kind "def", :line 11, :end-line 11, :hash "-1722330092"} {:id "defn-/unit-vector", :kind "defn-", :line 13, :end-line 18, :hash "-518215995"} {:id "defn-/flow-arrowhead", :kind "defn-", :line 20, :end-line 32, :hash "-1459538016"} {:id "defn-/flow-pipe-body", :kind "defn-", :line 34, :end-line 48, :hash "312484768"} {:id "defn-/flow-desc", :kind "defn-", :line 50, :end-line 70, :hash "-1028093553"} {:id "defn-/connector-desc", :kind "defn-", :line 72, :end-line 93, :hash "-1650792294"} {:id "def/bound-label-style", :kind "def", :line 95, :end-line 95, :hash "-2104208887"} {:id "defn/stock-icon-labels", :kind "defn", :line 97, :end-line 101, :hash "-2139438638"} {:id "defn/stock-canvas-labels", :kind "defn", :line 103, :end-line 108, :hash "-667198372"} {:id "defn/flow-icon-labels", :kind "defn", :line 110, :end-line 113, :hash "860011591"} {:id "defn-/flow-on-canvas", :kind "defn-", :line 115, :end-line 122, :hash "1774993296"} {:id "defn/flow-canvas-labels", :kind "defn", :line 124, :end-line 126, :hash "-1659873221"} {:id "defn-/stock-desc", :kind "defn-", :line 128, :end-line 156, :hash "-1787538790"} {:id "defn-/converter-desc", :kind "defn-", :line 158, :end-line 176, :hash "359382606"} {:id "defn-/cloud-desc", :kind "defn-", :line 178, :end-line 195, :hash "273299004"} {:id "defn-/overlay-segment", :kind "defn-", :line 197, :end-line 199, :hash "-1910896273"} {:id "defn/diagram-overlay-text", :kind "defn", :line 201, :end-line 214, :hash "1103884749"} {:id "defn-/canvas-background", :kind "defn-", :line 216, :end-line 221, :hash "538381286"} {:id "defn/canvas-stack", :kind "defn", :line 223, :end-line 239, :hash "-101801743"} {:id "defn/canvas-desc", :kind "defn", :line 241, :end-line 243, :hash "-772167057"}]}
;; clj-mutate-manifest-end
