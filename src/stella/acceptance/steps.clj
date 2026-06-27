(ns stella.acceptance.steps
  (:require [clojure.string :as str]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

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
  (let [disabled? (model/menu-item-disabled? shell item)]
    (cond
      (nil? disabled?) (fail! (str "unknown menu item: " item))
      disabled? (fail! (str "expected menu item enabled: " item)))))

(defn- assert-about-includes
  [shell app-name]
  (let [text (model/about-text shell)
        first-line (first (str/split-lines (or text "")))]
    (when-not (= app-name first-line)
      (fail! (str "about text expected first line " app-name)))))

(defn- parse-int
  [value label]
  (try
    (Integer/parseInt (str value))
    (catch NumberFormatException _
      (fail! (str "invalid integer for " label ": " value)))))

(defn- diagram-from
  [world]
  (or (:diagram world) (model/default-diagram)))

(defn- selection-kind
  [kind-str]
  (keyword kind-str))

(defn- apply-diagram-edit
  [world op]
  (let [before (diagram-from world)
        after (op before)]
    (-> world
        (assoc :diagram after)
        (assoc :last-edit-rejected? (= before after)))))

(defn- assert-stock-canvas-label
  [world stock-name field expected]
  (let [labels (canvas/stock-canvas-labels (diagram-from world) stock-name)]
    (when-not labels
      (fail! (str "stock " stock-name " not on canvas")))
    (when-not (= expected (get labels field))
      (fail! (str "stock " stock-name " canvas " (name field) " "
                  (get labels field) " expected " expected)))
    world))

(defn- assert-flow-canvas-label
  [world flow-name field expected]
  (let [labels (canvas/flow-canvas-labels (diagram-from world) flow-name)]
    (when-not labels
      (fail! (str "flow " flow-name " not on canvas")))
    (when-not (= expected (get labels field))
      (fail! (str "flow " flow-name " canvas " (name field) " "
                  (get labels field) " expected " expected)))
    world))

(defn- assert-converter-canvas-label
  [world converter-name field expected]
  (let [labels (canvas/converter-canvas-labels (diagram-from world) converter-name)]
    (when-not labels
      (fail! (str "converter " converter-name " not on canvas")))
    (when-not (= expected (get labels field))
      (fail! (str "converter " converter-name " canvas " (name field) " "
                  (get labels field) " expected " expected)))
    world))

(defn- assert-connector-canvas-label
  [world connector-name field expected]
  (let [labels (canvas/connector-canvas-labels (diagram-from world) connector-name)]
    (when-not labels
      (fail! (str "connector " connector-name " not on canvas")))
    (when-not (= expected (get labels field))
      (fail! (str "connector " connector-name " canvas " (name field) " "
                  (get labels field) " expected " expected)))
    world))

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
          (assert-about-includes (:shell world) (require-value example param-name))
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
   {:pattern #"^the diagram should contain stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/stock-exists? (diagram-from world) name)
            (fail! (str "diagram missing stock " name)))
          world)}
   {:pattern #"^stock <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/stock-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/stock-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name-param x-str y-str] example]
          (let [name (require-value example name-param)
                x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/stock-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^I move stock <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-stock! % name x y))))}
   {:pattern #"^I move stock ([A-Za-z0-9]+) to (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (update world :diagram #(cmd/move-stock! % name
                                                   (parse-int x-str "x")
                                                   (parse-int y-str "y"))))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas position should be <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (canvas/stock-canvas-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "stock " name " canvas position " pos " expected [" x " " y "]")))
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
   {:pattern #"^stock ([A-Za-z0-9]+) initial value should be 0$"
    :fn (fn [world [_ name] _]
          (let [actual (model/stock-initial-value (diagram-from world) name)]
            (when-not (= "0" actual)
              (fail! (str "stock " name " value " actual " expected 0")))
            world))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (require-value example name-param)
                new-name (require-value example new-name-param)]
            (apply-diagram-edit world #(cmd/set-stock-name! % name new-name))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (apply-diagram-edit world #(cmd/set-stock-name! % name new-name)))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> initial value to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (require-value example name-param)
                value (require-value example value-param)]
            (apply-diagram-edit world #(cmd/set-stock-initial-value! % name value))))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> minimum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (require-value example name-param)
                min-value (require-value example min-param)]
            (apply-diagram-edit world #(cmd/set-stock-min! % name min-value))))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> maximum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (require-value example name-param)
                max-value (require-value example max-param)]
            (apply-diagram-edit world #(cmd/set-stock-max! % name max-value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) maximum to (\d+)$"
    :fn (fn [world [_ name max-value] _]
          (apply-diagram-edit world #(cmd/set-stock-max! % name max-value)))}
   {:pattern #"^I clear stock <([A-Za-z0-9_]+)> maximum$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (apply-diagram-edit world #(cmd/clear-stock-max! % name))))}
   {:pattern #"^I clear stock ([A-Za-z0-9]+) maximum$"
    :fn (fn [world [_ name] _]
          (apply-diagram-edit world #(cmd/clear-stock-max! % name)))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> minimum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (require-value example name-param)
                min-value (require-value example min-param)
                actual (model/stock-min-value (diagram-from world) name)]
            (when-not (= min-value actual)
              (fail! (str "stock " name " minimum " actual " expected " min-value)))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> maximum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (require-value example name-param)
                max-value (require-value example max-param)
                actual (model/stock-max-value (diagram-from world) name)]
            (when-not (= max-value actual)
              (fail! (str "stock " name " maximum " actual " expected " max-value)))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) should have no maximum$"
    :fn (fn [world [_ name] _]
          (when-let [actual (model/stock-max-value (diagram-from world) name)]
            (fail! (str "stock " name " maximum " actual " expected none")))
          world)}
   {:pattern #"^the stock edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (fail! "expected stock edit to be rejected"))
          world)}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (require-value example name-param)
                text (require-value example text-param)]
            (assert-stock-canvas-label world name :name text)))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas minimum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (require-value example name-param)
                min-value (require-value example min-param)]
            (assert-stock-canvas-label world name :min min-value)))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas maximum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (require-value example name-param)
                max-value (require-value example max-param)]
            (assert-stock-canvas-label world name :max max-value)))}
   {:pattern #"^stock ([A-Za-z0-9]+) should display no maximum on canvas$"
    :fn (fn [world [_ name] _]
          (let [labels (canvas/stock-canvas-labels (diagram-from world) name)]
            (when-not labels
              (fail! (str "stock " name " not on canvas")))
            (when (:max labels)
              (fail! (str "stock " name " canvas maximum " (:max labels) " expected none")))
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
   {:pattern #"^the diagram stock count should be 1$"
    :fn (fn [world _ _]
          (when-not (= 1 (model/stock-count (diagram-from world)))
            (fail! (str "stock count " (model/stock-count (diagram-from world)) " expected 1")))
          world)}
   {:pattern #"^the stock placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/placement-disarmed? (diagram-from world))
            (fail! "expected stock placement tool disarmed"))
          world)}
   {:pattern #"^a diagram model with stock <([A-Za-z0-9_]+)> at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name x y))))}
   {:pattern #"^a diagram model with stock ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name (parse-int x-str "x") (parse-int y-str "y")))))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name x y))))}
   {:pattern #"^stock ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name (parse-int x-str "x") (parse-int y-str "y")))))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> runs from stock <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (require-value example flow-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow! diagram flow from to))))}
   {:pattern #"^flow ([A-Za-z0-9]+) runs from stock ([A-Za-z0-9]+) to stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow from to] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow! diagram flow from to))))}
   {:pattern #"^I arm the flow placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-flow-placement!))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :stock name)))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :stock name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :source name)))}
   {:pattern #"^I select sink <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :sink name))))}
   {:pattern #"^I select sink ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :sink name)))}
   {:pattern #"^I select sink <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :sink name))))}
   {:pattern #"^I select sink ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :sink name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :source name)))}
   {:pattern #"^the diagram should contain flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param] example]
          (let [flow (require-value example flow-param)]
            (when-not (model/flow-exists? (diagram-from world) flow)
              (fail! (str "diagram missing flow " flow)))
            world))}
   {:pattern #"^the diagram should contain flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow] _]
          (when-not (model/flow-exists? (diagram-from world) flow)
            (fail! (str "diagram missing flow " flow)))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (require-value example flow-param)
                from (require-value example from-param)
                to (require-value example to-param)
                endpoints (model/flow-endpoints (diagram-from world) flow)]
            (when-not (= [from to] endpoints)
              (fail! (str "flow " flow " endpoints " endpoints " expected [" from " " to "]")))
            world))}
   {:pattern #"^flow ([A-Za-z0-9]+) should run from stock ([A-Za-z0-9]+) to stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow from to] _]
          (let [endpoints (model/flow-endpoints (diagram-from world) flow)]
            (when-not (= [from to] endpoints)
              (fail! (str "flow " flow " endpoints " endpoints " expected [" from " " to "]")))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> rate should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param rate-param] example]
          (let [flow (require-value example flow-param)
                rate (require-value example rate-param)
                actual (model/flow-rate (diagram-from world) flow)]
            (when-not (= rate actual)
              (fail! (str "flow " flow " rate " actual " expected " rate)))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> rate should be 0$"
    :fn (fn [world [_ flow-param] example]
          (let [flow (require-value example flow-param)
                actual (model/flow-rate (diagram-from world) flow)]
            (when-not (= "0" actual)
              (fail! (str "flow " flow " rate " actual " expected 0")))
            world))}
   {:pattern #"^I set flow <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (require-value example name-param)
                new-name (require-value example new-name-param)]
            (apply-diagram-edit world #(cmd/set-flow-name! % name new-name))))}
   {:pattern #"^I set flow ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (apply-diagram-edit world #(cmd/set-flow-name! % name new-name)))}
   {:pattern #"^I set flow <([A-Za-z0-9_]+)> rate to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param rate-param] example]
          (let [name (require-value example name-param)
                rate (require-value example rate-param)]
            (apply-diagram-edit world #(cmd/set-flow-rate! % name rate))))}
   {:pattern #"^the flow edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (fail! "expected flow edit to be rejected"))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (require-value example name-param)
                text (require-value example text-param)]
            (assert-flow-canvas-label world name :name text)))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> canvas rate should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param rate-param] example]
          (let [name (require-value example name-param)
                rate (require-value example rate-param)]
            (assert-flow-canvas-label world name :rate rate)))}
   {:pattern #"^the diagram flow count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/flow-count (diagram-from world)))
            (fail! "expected diagram flow count 0"))
          world)}
   {:pattern #"^the flow placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/flow-placement-disarmed? (diagram-from world))
            (fail! "expected flow placement tool disarmed"))
          world)}
   {:pattern #"^the flow placement tool should be armed$"
    :fn (fn [world _ _]
          (when-not (model/flow-placement-armed? (diagram-from world))
            (fail! "expected flow placement tool armed"))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from source <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (require-value example flow-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (when-not (= {:kind :source :id from} (model/flow-from diagram flow))
              (fail! (str "flow " flow " from mismatch")))
            (when-not (= {:kind :stock :id to} (model/flow-to diagram flow))
              (fail! (str "flow " flow " to mismatch")))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to sink <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (require-value example flow-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (when-not (= {:kind :stock :id from} (model/flow-from diagram flow))
              (fail! (str "flow " flow " from mismatch")))
            (when-not (= {:kind :sink :id to} (model/flow-to diagram flow))
              (fail! (str "flow " flow " to mismatch")))
            world))}
   {:pattern #"^I arm the source placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-source-placement!))}
   {:pattern #"^I arm the sink placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-sink-placement!))}
   {:pattern #"^I place a source at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-source! % x y))))}
   {:pattern #"^I place a sink at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-sink! % x y))))}
   {:pattern #"^the diagram should contain source <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/source-exists? (diagram-from world) name)
              (fail! (str "diagram missing source " name)))
            world))}
   {:pattern #"^the diagram should contain source ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/source-exists? (diagram-from world) name)
            (fail! (str "diagram missing source " name)))
          world)}
   {:pattern #"^the diagram should contain sink <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/sink-exists? (diagram-from world) name)
              (fail! (str "diagram missing sink " name)))
            world))}
   {:pattern #"^the diagram should contain sink ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/sink-exists? (diagram-from world) name)
            (fail! (str "diagram missing sink " name)))
          world)}
   {:pattern #"^source <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/source-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "source " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^source ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/source-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "source " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^sink <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/sink-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "sink " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^sink ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/sink-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "sink " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^the diagram source count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/source-count (diagram-from world)))
            (fail! "expected diagram source count 0"))
          world)}
   {:pattern #"^the source placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/source-placement-disarmed? (diagram-from world))
            (fail! "expected source placement tool disarmed"))
          world)}
   {:pattern #"^source ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-source! diagram name (parse-int x-str "x") (parse-int y-str "y")))))}
   {:pattern #"^sink ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-sink! diagram name (parse-int x-str "x") (parse-int y-str "y")))))}
   {:pattern #"^I arm the converter placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-converter-placement!))}
   {:pattern #"^I place a converter at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-converter! % x y))))}
   {:pattern #"^the diagram should contain converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/converter-exists? (diagram-from world) name)
              (fail! (str "diagram missing converter " name)))
            world))}
   {:pattern #"^the diagram should contain converter ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/converter-exists? (diagram-from world) name)
            (fail! (str "diagram missing converter " name)))
          world)}
   {:pattern #"^converter <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/converter-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/converter-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name-param x-str y-str] example]
          (let [name (require-value example name-param)
                x (parse-int x-str "x")
                y (parse-int y-str "y")
                pos (model/converter-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^I move converter <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-converter! % name x y))))}
   {:pattern #"^I move converter ([A-Za-z0-9]+) to (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (update world :diagram #(cmd/move-converter! % name
                                                       (parse-int x-str "x")
                                                       (parse-int y-str "y"))))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> canvas position should be <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (canvas/converter-canvas-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "converter " name " canvas position " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (require-value example name-param)
                value (require-value example value-param)
                actual (model/converter-value (diagram-from world) name)]
            (when-not (= value actual)
              (fail! (str "converter " name " value " actual " expected " value)))
            world))}
   {:pattern #"^converter ([A-Za-z0-9]+) value should be ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name value] _]
          (let [actual (model/converter-value (diagram-from world) name)]
            (when-not (= value actual)
              (fail! (str "converter " name " value " actual " expected " value)))
            world))}
   {:pattern #"^I set converter <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (require-value example name-param)
                new-name (require-value example new-name-param)]
            (apply-diagram-edit world #(cmd/set-converter-name! % name new-name))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (apply-diagram-edit world #(cmd/set-converter-name! % name new-name)))}
   {:pattern #"^I set converter <([A-Za-z0-9_]+)> formula to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param formula-param] example]
          (let [name (require-value example name-param)
                formula (require-value example formula-param)]
            (apply-diagram-edit world #(cmd/set-converter-formula! % name formula))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) formula to (.+)$"
    :fn (fn [world [_ name formula] _]
          (apply-diagram-edit world #(cmd/set-converter-formula! % name formula)))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (require-value example name-param)
                text (require-value example text-param)]
            (assert-converter-canvas-label world name :name text)))}
   {:pattern #"^the converter edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (fail! "expected converter edit to be rejected"))
          world)}
   {:pattern #"^the diagram converter count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/converter-count (diagram-from world)))
            (fail! "expected diagram converter count 0"))
          world)}
   {:pattern #"^the diagram converter count should be 1$"
    :fn (fn [world _ _]
          (when-not (= 1 (model/converter-count (diagram-from world)))
            (fail! (str "converter count " (model/converter-count (diagram-from world)) " expected 1")))
          world)}
   {:pattern #"^the converter placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/converter-placement-disarmed? (diagram-from world))
            (fail! "expected converter placement tool disarmed"))
          world)}
   {:pattern #"^converter ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-converter! diagram name (parse-int x-str "x") (parse-int y-str "y")))))}
   {:pattern #"^I arm the connector placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-connector-placement!))}
   {:pattern #"^I select converter <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :converter name))))}
   {:pattern #"^I select converter ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :converter name)))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :stock name)))}
   {:pattern #"^I select flow <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :flow name))))}
   {:pattern #"^I select flow ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :flow name)))}
   {:pattern #"^I select flow <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :flow name))))}
   {:pattern #"^I select flow ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :flow name)))}
   {:pattern #"^I select converter <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :converter name))))}
   {:pattern #"^I select converter ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :converter name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :source name)))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> runs from converter <([A-Za-z0-9_]+)> to flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (require-value example connector-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-connector! diagram connector from to))))}
   {:pattern #"^connector ([A-Za-z0-9]+) runs from converter ([A-Za-z0-9]+) to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector from to] _]
          (let [diagram (diagram-from world)]
            (assoc world :diagram (cmd/fixture-connector! diagram connector from to))))}
   {:pattern #"^the diagram should contain connector <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/connector-exists? (diagram-from world) name)
              (fail! (str "diagram missing connector " name)))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> formula should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param formula-param] example]
          (let [connector (require-value example connector-param)
                formula (require-value example formula-param)
                actual (model/connector-formula (diagram-from world) connector)]
            (when-not (= formula actual)
              (fail! (str "connector " connector " formula " actual " expected " formula)))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should have no formula$"
    :fn (fn [world [_ connector-param] example]
          (let [connector (require-value example connector-param)
                actual (model/connector-formula (diagram-from world) connector)]
            (when (seq actual)
              (fail! (str "connector " connector " formula " actual " expected none")))
            world))}
   {:pattern #"^connector ([A-Za-z0-9]+) should have no formula$"
    :fn (fn [world [_ connector] _]
          (when (seq (model/connector-formula (diagram-from world) connector))
            (fail! (str "connector " connector " formula expected none")))
          world)}
   {:pattern #"^connector <([A-Za-z0-9_]+)> canvas formula should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param formula-param] example]
          (let [connector (require-value example connector-param)
                formula (require-value example formula-param)]
            (assert-connector-canvas-label world connector :formula formula)))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from converter <([A-Za-z0-9_]+)> to flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (require-value example connector-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector ([A-Za-z0-9]+) should run from converter ([A-Za-z0-9]+) to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector from to] _]
          (let [diagram (diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from converter <([A-Za-z0-9_]+)> to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector-param from-param to] example]
          (let [connector (require-value example connector-param)
                from (require-value example from-param)
                diagram (diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (require-value example connector-param)
                from (require-value example from-param)
                to (require-value example to-param)
                diagram (diagram-from world)]
            (when-not (= {:kind :stock :id from} (model/connector-from diagram connector))
              (fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :converter :id to} (model/connector-to diagram connector))
              (fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^the diagram connector count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/connector-count (diagram-from world)))
            (fail! "expected diagram connector count 0"))
          world)}
   {:pattern #"^the connector placement tool should be armed$"
    :fn (fn [world _ _]
          (when-not (model/connector-placement-armed? (diagram-from world))
            (fail! "expected connector placement tool armed"))
          world)}
   {:pattern #"^the connector placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/connector-placement-disarmed? (diagram-from world))
            (fail! "expected connector placement tool disarmed"))
          world)}
   {:pattern #"^I click select <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (selection-kind (require-value example kind-param))
                name (require-value example name-param)]
            (update world :diagram #(cmd/click-select! % kind name))))}
   {:pattern #"^I click select (stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+)$"
    :fn (fn [world [_ kind name] _]
          (update world :diagram #(cmd/click-select! % (selection-kind kind) name)))}
   {:pattern #"^I shift click select <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (selection-kind (require-value example kind-param))
                name (require-value example name-param)]
            (update world :diagram #(cmd/shift-click-select! % kind name))))}
   {:pattern #"^I shift click select (stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+)$"
    :fn (fn [world [_ kind name] _]
          (update world :diagram #(cmd/shift-click-select! % (selection-kind kind) name)))}
   {:pattern #"^I marquee select from <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x1-param y1-param x2-param y2-param] example]
          (let [x1 (parse-int (require-value example x1-param) x1-param)
                y1 (parse-int (require-value example y1-param) y1-param)
                x2 (parse-int (require-value example x2-param) x2-param)
                y2 (parse-int (require-value example y2-param) y2-param)]
            (update world :diagram #(cmd/marquee-select! % x1 y1 x2 y2))))}
   {:pattern #"^I clear the selection$"
    :fn (fn [world _ _]
          (update world :diagram cmd/clear-selection!))}
   {:pattern #"^<([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> should be selected$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (selection-kind (require-value example kind-param))
                name (require-value example name-param)]
            (when-not (model/selected? (diagram-from world) kind name)
              (fail! (str kind " " name " not selected")))
            world))}
   {:pattern #"^(stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+) should be selected$"
    :fn (fn [world [_ kind name] _]
          (when-not (model/selected? (diagram-from world) (selection-kind kind) name)
            (fail! (str kind " " name " not selected")))
          world)}
   {:pattern #"^<([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> should not be selected$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (selection-kind (require-value example kind-param))
                name (require-value example name-param)]
            (when (model/selected? (diagram-from world) kind name)
              (fail! (str kind " " name " should not be selected")))
            world))}
   {:pattern #"^(stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+) should not be selected$"
    :fn (fn [world [_ kind name] _]
          (when (model/selected? (diagram-from world) (selection-kind kind) name)
            (fail! (str kind " " name " should not be selected")))
          world)}
   {:pattern #"^the selection count should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (let [count (parse-int (require-value example count-param) count-param)
                actual (model/selection-count (diagram-from world))]
            (when-not (= count actual)
              (fail! (str "selection count " actual " expected " count)))
            world))}
   {:pattern #"^the selection count should be (\d+)$"
    :fn (fn [world [_ count-str] _]
          (let [count (parse-int count-str "count")
                actual (model/selection-count (diagram-from world))]
            (when-not (= count actual)
              (fail! (str "selection count " actual " expected " count)))
            world))}
   {:pattern #"^nothing should be selected$"
    :fn (fn [world _ _]
          (when-not (model/nothing-selected? (diagram-from world))
            (fail! (str "selection count " (model/selection-count (diagram-from world)) " expected 0")))
          world)}])

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (fail! (str "unsupported step: " text)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T17:14:23.075047-05:00", :module-hash "454439326", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-113792008"} {:id "defn-/fail!", :kind "defn-", :line 6, :end-line 8, :hash "425420929"} {:id "defn-/require-value", :kind "defn-", :line 10, :end-line 14, :hash "369839950"} {:id "defn-/assert-menu-includes", :kind "defn-", :line 16, :end-line 19, :hash "-1322422941"} {:id "defn-/assert-menu-item-disabled", :kind "defn-", :line 21, :end-line 24, :hash "580418853"} {:id "defn-/assert-menu-item-enabled", :kind "defn-", :line 26, :end-line 31, :hash "-1393044992"} {:id "defn-/assert-about-includes", :kind "defn-", :line 33, :end-line 38, :hash "1963673308"} {:id "defn-/parse-int", :kind "defn-", :line 40, :end-line 45, :hash "60081232"} {:id "defn-/diagram-from", :kind "defn-", :line 47, :end-line 49, :hash "1678108818"} {:id "def/step-handlers", :kind "def", :line 51, :end-line 564, :hash "-1558652395"} {:id "defn/dispatch-step", :kind "defn", :line 566, :end-line 571, :hash "602476927"}]}
;; clj-mutate-manifest-end
