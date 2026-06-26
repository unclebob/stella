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
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (require-value example flow-param)
                from (require-value example from-param)
                to (require-value example to-param)
                endpoints (model/flow-endpoints (diagram-from world) flow)]
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
   {:pattern #"^the diagram should contain sink <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/sink-exists? (diagram-from world) name)
              (fail! (str "diagram missing sink " name)))
            world))}
   {:pattern #"^source <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
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
   {:pattern #"^converter <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (require-value example name-param)
                x (parse-int (require-value example x-param) x-param)
                y (parse-int (require-value example y-param) y-param)
                pos (model/converter-position (diagram-from world) name)]
            (when-not (= [x y] pos)
              (fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (require-value example name-param)
                value (require-value example value-param)
                actual (model/converter-value (diagram-from world) name)]
            (when-not (= value actual)
              (fail! (str "converter " name " value " actual " expected " value)))
            world))}
   {:pattern #"^the diagram converter count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/converter-count (diagram-from world)))
            (fail! "expected diagram converter count 0"))
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
   {:pattern #"^the diagram should contain connector <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (require-value example name-param)]
            (when-not (model/connector-exists? (diagram-from world) name)
              (fail! (str "diagram missing connector " name)))
            world))}
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
          world)}])

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (fail! (str "unsupported step: " text)))))