(ns stella.acceptance.step-support
  (:require [clojure.string :as str]
            [stella.commands :as cmd]
            [stella.model :as model]
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:13:21.036755-05:00", :module-hash "1685934464", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1159229129"} {:id "defn/fail!", :kind "defn", :line 7, :end-line 9, :hash "-526803145"} {:id "defn/require-value", :kind "defn", :line 11, :end-line 15, :hash "-545191389"} {:id "defn/assert-menu-includes", :kind "defn", :line 17, :end-line 20, :hash "-349348030"} {:id "defn/assert-menu-item-disabled", :kind "defn", :line 22, :end-line 25, :hash "-1165811575"} {:id "defn/assert-menu-item-enabled", :kind "defn", :line 27, :end-line 32, :hash "-2090116262"} {:id "defn/assert-about-includes", :kind "defn", :line 34, :end-line 39, :hash "606493738"} {:id "defn/parse-int", :kind "defn", :line 41, :end-line 46, :hash "-520638366"} {:id "defn/diagram-from", :kind "defn", :line 48, :end-line 50, :hash "641980991"} {:id "defn/apply-diagram-edit", :kind "defn", :line 52, :end-line 58, :hash "-1350231762"} {:id "defn-/assert-canvas-label", :kind "defn-", :line 60, :end-line 68, :hash "1218037688"} {:id "defn/assert-stock-canvas-label", :kind "defn", :line 70, :end-line 72, :hash "142324445"} {:id "defn/assert-flow-canvas-label", :kind "defn", :line 74, :end-line 76, :hash "-730263822"}]}
;; clj-mutate-manifest-end
