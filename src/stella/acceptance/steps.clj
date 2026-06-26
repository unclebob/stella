(ns stella.acceptance.steps
  (:require [stella.commands :as cmd]
            [stella.model :as model]))

(defn- fail!
  [message]
  (throw (ex-info message {})))

(defn- require-value
  [example param-name]
  (or (get example param-name)
      (get example (keyword param-name))
      (fail! (str "missing example value for " param-name))))

(defn- assert-menu-includes
  [shell menu]
  (when-not (model/menu-includes? shell menu)
    (fail! (str "menu bar missing " menu))))

(defn- assert-menu-item-disabled
  [shell item]
  (when-not (model/menu-item-disabled? shell item)
    (fail! (str "expected menu item disabled: " item))))

(defn- assert-menu-item-enabled
  [shell item]
  (when (model/menu-item-disabled? shell item)
    (fail! (str "expected menu item enabled: " item))))

(defn- parse-int
  [value label]
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException _
      (fail! (str "invalid integer for " label ": " value)))))

(defn- diagram-from
  [world]
  (or (:diagram world) (model/default-diagram)))

(def step-handlers
  [{:pattern #"^a default shell application$"
    :fn (fn [world _ _]
          (assoc world :shell (cmd/default-shell! nil)))}
   {:pattern #"^the shell menu bar should include <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (assert-menu-includes (:shell world) (require-value example param-name))
          world)}
   {:pattern #"^the shell menu item <([A-Za-z0-9_]+)> should be disabled$"
    :fn (fn [world [_ param-name] example]
          (assert-menu-item-disabled (:shell world) (require-value example param-name))
          world)}
   {:pattern #"^the shell menu item <([A-Za-z0-9_]+)> should be enabled$"
    :fn (fn [world [_ param-name] example]
          (assert-menu-item-enabled (:shell world) (require-value example param-name))
          world)}
   {:pattern #"^the shell window title should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (let [title (require-value example param-name)]
            (when-not (= title (model/window-title (:shell world)))
              (fail! (str "expected window title " title))))
          world)}
   {:pattern #"^the shell should be showing$"
    :fn (fn [world _ _]
          (when-not (model/showing? (:shell world))
            (fail! "expected shell to be showing"))
          world)}
   {:pattern #"^the shell should not be showing$"
    :fn (fn [world _ _]
          (when (model/showing? (:shell world))
            (fail! "expected shell not to be showing"))
          world)}
   {:pattern #"^I show the about dialog$"
    :fn (fn [world _ _]
          (update world :shell cmd/show-about!))}
   {:pattern #"^the about dialog should be visible$"
    :fn (fn [world _ _]
          (when-not (model/about-visible? (:shell world))
            (fail! "expected about dialog visible"))
          world)}
   {:pattern #"^the about dialog text should include <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (let [app-name (require-value example param-name)
                text (model/about-text (:shell world))]
            (when-not (re-find (re-pattern (str "(?i)" (java.util.regex.Pattern/quote app-name))) text)
              (fail! (str "about text missing " app-name))))
          world)}
   {:pattern #"^I quit the shell application$"
    :fn (fn [world _ _]
          (update world :shell cmd/quit!))}
   {:pattern #"^the diagram canvas should be empty$"
    :fn (fn [world _ _]
          (when-not (model/diagram-empty? (:shell world))
            (fail! "expected empty diagram canvas"))
          world)}
   {:pattern #"^an empty diagram model$"
    :fn (fn [world _ _]
          (assoc world :diagram (cmd/default-diagram! nil)))}
   {:pattern #"^I arm the stock placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-stock-placement!))}
   {:pattern #"^I place a stock at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-stock! % x y))))}
   {:pattern #"^the diagram should contain stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/stock-exists? (diagram-from world) name)
              (fail! (str "diagram missing stock " name)))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/stock-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> initial value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (require-value example name-param)
                value (require-value example value-param)
                actual (model/stock-initial-value (diagram-from world) name)]
            (when-not (= value actual)
              (fail! (str "stock " name " value " actual " expected " value)))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> initial value should be 0$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)
                actual (model/stock-initial-value (diagram-from world) name)]
            (when-not (= "0" actual)
              (fail! (str "stock " name " value " actual " expected 0")))
            world))}
   {:pattern #"^the diagram stock count should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (let [count (parse-int (require-value example count-param) count-param)
                actual (model/stock-count (diagram-from world))]
            (when-not (= count actual)
              (fail! (str "stock count " actual " expected " count)))
            world))}
   {:pattern #"^the diagram stock count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/stock-count (diagram-from world)))
            (fail! "expected diagram stock count 0"))
          world)}
   {:pattern #"^the stock placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/placement-disarmed? (diagram-from world))
            (fail! "expected stock placement tool disarmed"))
          world)}])

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (fail! (str "unsupported step: " text)))))