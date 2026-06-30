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
  (let [therm (canvas/stock-canvas-thermometer (diagram-from world) stock-name)]
    (when-not therm
      (fail! (str "stock " stock-name " not on canvas")))
    (let [actual (get therm field)]
      (when-not (= expected actual)
        (fail! (str "stock " stock-name " canvas thermometer "
                    (name field) " " actual " expected " expected))))
    world))
