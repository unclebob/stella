(ns stella.ui.palette
  (:require [stella.events :as events]))

(def ^:private tool-width 82)
(def ^:private tool-height 58)
(def ^:private tool-spacing 64)
(def ^:private icon-center-x 41)
(def ^:private icon-center-y 22)

(def ^:private active-tool-style
  "-fx-fill: #d8e8ff; -fx-stroke: #2f80ed; -fx-stroke-width: 1;")

(def ^:private inactive-tool-style
  "-fx-fill: transparent; -fx-stroke: transparent; -fx-stroke-width: 1;")

(def ^:private label-style
  "-fx-font-size: 10px;")

(def ^:private icon-shape-style
  "-fx-fill: white; -fx-stroke: #333; -fx-stroke-width: 1;")

(defn- stock-icon []
  [{:fx/type :rectangle
    :x 21
    :y 10
    :width 40
    :height 24
    :style icon-shape-style}])

(defn- cloud-icon []
  [{:fx/type :circle
    :center-x 29
    :center-y 24
    :radius 11
    :style icon-shape-style}
   {:fx/type :circle
    :center-x 42
    :center-y 17
    :radius 14
    :style icon-shape-style}
   {:fx/type :circle
    :center-x 55
    :center-y 24
    :radius 10
    :style icon-shape-style}
   {:fx/type :rectangle
    :x 27
    :y 23
    :width 31
    :height 12
    :style "-fx-fill: white; -fx-stroke: transparent;"}])

(defn- converter-icon []
  [{:fx/type :circle
    :center-x icon-center-x
    :center-y icon-center-y
    :radius 14
    :style icon-shape-style}])

(defn- flow-icon []
  [{:fx/type :line
    :start-x 17
    :start-y icon-center-y
    :end-x 56
    :end-y icon-center-y
    :stroke "#555"
    :stroke-width 5
    :stroke-line-cap :round}
   {:fx/type :line
    :start-x 17
    :start-y icon-center-y
    :end-x 56
    :end-y icon-center-y
    :stroke "#eef4f7"
    :stroke-width 3
    :stroke-line-cap :round}
   {:fx/type :polygon
    :points [63 icon-center-y 54 14 54 30]
    :fill "#eef4f7"
    :stroke "#555"
    :stroke-width 1}
   {:fx/type :circle
    :center-x icon-center-x
    :center-y icon-center-y
    :radius 7
    :fill "white"
    :stroke "#555"
    :stroke-width 1}])

(defn- connector-icon []
  [{:fx/type :quad-curve
    :start-x 20
    :start-y 30
    :control-x 40
    :control-y 8
    :end-x 62
    :end-y 24
    :fill "transparent"
    :stroke "#666"
    :stroke-width 1}
   {:fx/type :polygon
    :points [62 24 53 18 55 30]
    :fill "#666"}])

(defn- icon-nodes
  [kind]
  (case kind
    :stock (stock-icon)
    :flow (flow-icon)
    :source (cloud-icon)
    :sink (cloud-icon)
    :converter (converter-icon)
    :connector (connector-icon)))

(defn- active?
  [shell mode]
  (= mode (get-in shell [:diagram :placement-mode])))

(defn- palette-tool
  [shell index {:keys [kind label event mode]}]
  {:fx/type :group
   :id (str "palette-" label)
   :layout-y (* index tool-spacing)
   :on-mouse-clicked {:event event}
   :children (into [{:fx/type :rectangle
                     :width tool-width
                     :height tool-height
                     :arc-width 6
                     :arc-height 6
                     :style (if (active? shell mode)
                              active-tool-style
                              inactive-tool-style)}
                    {:fx/type :label
                     :layout-y 39
                     :pref-width tool-width
                     :alignment :center
                     :text-alignment :center
                     :style label-style
                     :text label}]
                   (icon-nodes kind))})

(def ^:private tools
  [{:kind :stock :label "Stock" :event events/arm-stock :mode :stock}
   {:kind :flow :label "Flow" :event events/arm-flow :mode :flow}
   {:kind :source :label "Source" :event events/arm-source :mode :source}
   {:kind :sink :label "Sink" :event events/arm-sink :mode :sink}
   {:kind :converter :label "Converter" :event events/arm-converter :mode :converter}
   {:kind :connector :label "Connector" :event events/arm-connector :mode :connector}])

(defn palette-desc
  ([] (palette-desc nil))
  ([shell]
   {:fx/type :pane
    :style "-fx-background-color: #e8e8e8;"
    :pref-width tool-width
    :min-width tool-width
    :children (mapv #(palette-tool shell %1 %2) (range) tools)}))
