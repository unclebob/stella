(ns stella.acceptance.step-support
  (:require [clojure.string :as str]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.thermometer :as thermometer]
            [stella.ui.canvas :as canvas]))

(defn fail!
  [message]
  (throw (ex-info message {})))

(defn require-value
  [example param-name]
  (or (get example param-name)
      (get example (keyword param-name))
      (fail! (str "missing example value for " param-name))))

(defn assert-menu-includes
  [shell menu]
  (when-not (model/menu-includes? shell menu)
    (fail! (str "menu bar missing " menu))))

(defn assert-menu-item-disabled
  [shell item]
  (when-not (model/menu-item-disabled? shell item)
    (fail! (str "expected menu item disabled: " item))))

(defn assert-menu-item-enabled
  [shell item]
  (let [disabled? (model/menu-item-disabled? shell item)]
    (cond
      (nil? disabled?) (fail! (str "unknown menu item: " item))
      disabled? (fail! (str "expected menu item enabled: " item)))))

(defn assert-about-includes
  [shell app-name]
  (let [text (model/about-text shell)
        first-line (first (str/split-lines (or text "")))]
    (when-not (= app-name first-line)
      (fail! (str "about text expected first line " app-name)))))

(defn parse-int
  [value label]
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException _
      (fail! (str "invalid integer for " label ": " value)))))

(defn diagram-from
  [world]
  (or (:diagram world) (model/default-diagram)))

(defn selection-kind
  [kind-str]
  (keyword kind-str))

(defn apply-diagram-edit
  [world op]
  (let [before (diagram-from world)
        after (op before)]
    (-> world
        (assoc :diagram after)
        (assoc :last-edit-rejected? (= before after)))))

(defn- assert-canvas-label
  [world entity-name entity-label labels-fn field expected]
  (let [labels (labels-fn (diagram-from world) entity-name)]
    (when-not labels
      (fail! (str entity-label " " entity-name " not on canvas")))
    (when-not (= expected (get labels field))
      (fail! (str entity-label " " entity-name " canvas " (name field) " "
                  (get labels field) " expected " expected)))
    world))

(defn assert-stock-canvas-label
  [world stock-name field expected]
  (assert-canvas-label world stock-name "stock" canvas/stock-canvas-labels field expected))

(defn assert-flow-canvas-label
  [world flow-name field expected]
  (assert-canvas-label world flow-name "flow" canvas/flow-canvas-labels field expected))

(defn assert-converter-canvas-label
  [world converter-name field expected]
  (assert-canvas-label world converter-name "converter"
                       canvas/converter-canvas-labels field expected))

(defn assert-connector-canvas-label
  [world connector-name field expected]
  (assert-canvas-label world connector-name "connector"
                       canvas/connector-canvas-labels field expected))

(defn assert-stock-canvas-thermometer
  [world stock-name field expected]
  (let [therm (thermometer/stock-thermometer (diagram-from world) stock-name)]
    (when-not therm
      (fail! (str "stock " stock-name " not on canvas")))
    (let [actual (get therm field)]
      (when-not (= expected actual)
        (fail! (str "stock " stock-name " canvas thermometer "
                    (name field) " " actual " expected " expected))))
    world))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T09:56:50.074435-05:00", :module-hash "-2018738001", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "-1561984709"} {:id "defn/fail!", :kind "defn", :line 8, :end-line 10, :hash "-526803145"} {:id "defn/require-value", :kind "defn", :line 12, :end-line 16, :hash "-545191389"} {:id "defn/assert-menu-includes", :kind "defn", :line 18, :end-line 21, :hash "-349348030"} {:id "defn/assert-menu-item-disabled", :kind "defn", :line 23, :end-line 26, :hash "-1165811575"} {:id "defn/assert-menu-item-enabled", :kind "defn", :line 28, :end-line 33, :hash "-2090116262"} {:id "defn/assert-about-includes", :kind "defn", :line 35, :end-line 40, :hash "606493738"} {:id "defn/parse-int", :kind "defn", :line 42, :end-line 47, :hash "-520638366"} {:id "defn/diagram-from", :kind "defn", :line 49, :end-line 51, :hash "641980991"} {:id "defn/selection-kind", :kind "defn", :line 53, :end-line 55, :hash "-1911988122"} {:id "defn/apply-diagram-edit", :kind "defn", :line 57, :end-line 63, :hash "-1350231762"} {:id "defn-/assert-canvas-label", :kind "defn-", :line 65, :end-line 73, :hash "1218037688"} {:id "defn/assert-stock-canvas-label", :kind "defn", :line 75, :end-line 77, :hash "142324445"} {:id "defn/assert-flow-canvas-label", :kind "defn", :line 79, :end-line 81, :hash "-730263822"} {:id "defn/assert-converter-canvas-label", :kind "defn", :line 83, :end-line 86, :hash "906876011"} {:id "defn/assert-connector-canvas-label", :kind "defn", :line 88, :end-line 91, :hash "-590705219"} {:id "defn/assert-stock-canvas-thermometer", :kind "defn", :line 93, :end-line 102, :hash "122590513"}]}
;; clj-mutate-manifest-end
