(ns stella.ui.palette
  (:require [stella.events :as events]
            [stella.model :as model]))

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

(defn- arrow-lines
  [tip-x tip-y base-x base-y]
  [{:fx/type :line
    :start-x tip-x
    :start-y tip-y
    :end-x (- base-x 4)
    :end-y base-y
    :stroke "#333"
    :stroke-width 1}
   {:fx/type :line
    :start-x tip-x
    :start-y tip-y
    :end-x (+ base-x 4)
    :end-y base-y
    :stroke "#333"
    :stroke-width 1}])

(defn- vertical-arrow
  [line-start-y line-end-y tip-y base-y]
  (into [{:fx/type :line
          :start-x 42
          :start-y line-start-y
          :end-x 42
          :end-y line-end-y
          :stroke "#333"
          :stroke-width 1}]
        (arrow-lines 42 tip-y 42 base-y)))

(defn- cloud-shape []
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

(defn- source-icon []
  (into (cloud-shape)
        (vertical-arrow 24 3 1 7)))

(defn- sink-icon []
  (into (cloud-shape)
        (vertical-arrow 1 22 24 18)))

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
   {:fx/type :line
    :start-x 62
    :start-y 24
    :end-x 53.2
    :end-y 22.5
    :stroke "#666"
    :stroke-width 1}
   {:fx/type :line
    :start-x 62
    :start-y 24
    :end-x 57.9
    :end-y 16.1
    :stroke "#666"
    :stroke-width 1}])

(def ^:private icon-builders
  {:stock stock-icon
   :flow flow-icon
   :source source-icon
   :sink sink-icon
   :converter converter-icon
   :connector connector-icon})

(defn- icon-nodes
  [kind]
  ((icon-builders kind)))

(defn- active?
  [shell mode]
  (= mode (get-in shell [:diagram :placement-mode])))

(defn palette-tool-active?
  [shell tool-label]
  (model/palette-tool-active? shell tool-label))

(defn no-palette-tool-active?
  [shell]
  (model/no-palette-tool-active? shell))

(defn- mouse-transparent
  [desc]
  (assoc desc :mouse-transparent true))

(defn- palette-tool
  [shell index {:keys [kind label event mode]}]
  {:fx/type :group
   :id (str "palette-" label)
   :layout-y (* index tool-spacing)
   :children (into [{:fx/type :rectangle
                     :width tool-width
                     :height tool-height
                     :arc-width 6
                     :arc-height 6
                     :style (if (active? shell mode)
                              active-tool-style
                              inactive-tool-style)
                     :on-mouse-clicked {:event event}}
                    (mouse-transparent
                     {:fx/type :label
                      :layout-y 39
                      :pref-width tool-width
                      :alignment :center
                      :text-alignment :center
                      :style label-style
                      :text label})]
                   (map mouse-transparent (icon-nodes kind)))})

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
    :min-height (+ tool-height (* (dec (count tools)) tool-spacing))
    :children (mapv #(palette-tool shell %1 %2) (range) tools)}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T10:04:41.718719-05:00", :module-hash "-769615969", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-1921853304"} {:id "def/tool-width", :kind "def", :line 5, :end-line 5, :hash "1090818608"} {:id "def/tool-height", :kind "def", :line 6, :end-line 6, :hash "1370478822"} {:id "def/tool-spacing", :kind "def", :line 7, :end-line 7, :hash "916458214"} {:id "def/icon-center-x", :kind "def", :line 8, :end-line 8, :hash "-462519176"} {:id "def/icon-center-y", :kind "def", :line 9, :end-line 9, :hash "1056310866"} {:id "def/active-tool-style", :kind "def", :line 11, :end-line 12, :hash "-787053818"} {:id "def/inactive-tool-style", :kind "def", :line 14, :end-line 15, :hash "763010742"} {:id "def/label-style", :kind "def", :line 17, :end-line 18, :hash "156767354"} {:id "def/icon-shape-style", :kind "def", :line 20, :end-line 21, :hash "1327547973"} {:id "defn-/stock-icon", :kind "defn-", :line 23, :end-line 29, :hash "1763730918"} {:id "defn-/arrow-lines", :kind "defn-", :line 31, :end-line 46, :hash "1259119072"} {:id "defn-/vertical-arrow", :kind "defn-", :line 48, :end-line 57, :hash "823963806"} {:id "defn-/cloud-shape", :kind "defn-", :line 59, :end-line 80, :hash "730835238"} {:id "defn-/source-icon", :kind "defn-", :line 82, :end-line 84, :hash "-1774341239"} {:id "defn-/sink-icon", :kind "defn-", :line 86, :end-line 88, :hash "-2139770616"} {:id "defn-/converter-icon", :kind "defn-", :line 90, :end-line 95, :hash "-268829089"} {:id "defn-/flow-icon", :kind "defn-", :line 97, :end-line 125, :hash "-1356788087"} {:id "defn-/connector-icon", :kind "defn-", :line 127, :end-line 151, :hash "2063818044"} {:id "def/icon-builders", :kind "def", :line 153, :end-line 159, :hash "123424901"} {:id "defn-/icon-nodes", :kind "defn-", :line 161, :end-line 163, :hash "1775495204"} {:id "defn-/active?", :kind "defn-", :line 165, :end-line 167, :hash "-2014389021"} {:id "defn/palette-tool-active?", :kind "defn", :line 169, :end-line 171, :hash "1064553190"} {:id "defn/no-palette-tool-active?", :kind "defn", :line 173, :end-line 175, :hash "313692840"} {:id "defn-/mouse-transparent", :kind "defn-", :line 177, :end-line 179, :hash "-821078899"} {:id "defn-/palette-tool", :kind "defn-", :line 181, :end-line 203, :hash "213123278"} {:id "def/tools", :kind "def", :line 205, :end-line 211, :hash "1150421922"} {:id "defn/palette-desc", :kind "defn", :line 213, :end-line 221, :hash "-710422638"}]}
;; clj-mutate-manifest-end
