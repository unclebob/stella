(ns stella.acceptance.step-handlers-placement
  (:require [stella.acceptance.step-support :as support]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

(def placement-handlers
  [{:pattern #"^a default shell application$"
    :fn (fn [world _ _]
          (assoc world :shell (cmd/default-shell! nil)))}
   {:pattern #"^the shell menu bar should include <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (support/assert-menu-includes (:shell world) (support/require-value example param-name))
          world)}
   {:pattern #"^the shell menu item <([A-Za-z0-9_]+)> should be disabled$"
    :fn (fn [world [_ param-name] example]
          (support/assert-menu-item-disabled (:shell world) (support/require-value example param-name))
          world)}
   {:pattern #"^the shell menu item <([A-Za-z0-9_]+)> should be enabled$"
    :fn (fn [world [_ param-name] example]
          (support/assert-menu-item-enabled (:shell world) (support/require-value example param-name))
          world)}
   {:pattern #"^the shell window title should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (let [title (support/require-value example param-name)]
            (when-not (= title (model/window-title (:shell world)))
              (support/fail! (str "expected window title " title))))
          world)}
   {:pattern #"^the shell should be showing$"
    :fn (fn [world _ _]
          (when-not (model/showing? (:shell world))
            (support/fail! "expected shell to be showing"))
          world)}
   {:pattern #"^the shell should not be showing$"
    :fn (fn [world _ _]
          (when (model/showing? (:shell world))
            (support/fail! "expected shell not to be showing"))
          world)}
   {:pattern #"^I show the about dialog$"
    :fn (fn [world _ _]
          (update world :shell cmd/show-about!))}
   {:pattern #"^the about dialog should be visible$"
    :fn (fn [world _ _]
          (when-not (model/about-visible? (:shell world))
            (support/fail! "expected about dialog visible"))
          world)}
   {:pattern #"^the about dialog text should include <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ param-name] example]
          (support/assert-about-includes (:shell world) (support/require-value example param-name))
          world)}
   {:pattern #"^I quit the shell application$"
    :fn (fn [world _ _]
          (update world :shell cmd/quit!))}
   {:pattern #"^the diagram canvas should be empty$"
    :fn (fn [world _ _]
          (when-not (model/diagram-empty? (:shell world))
            (support/fail! "expected empty diagram canvas"))
          world)}
   {:pattern #"^an empty diagram model$"
    :fn (fn [world _ _]
          (assoc world :diagram (cmd/default-diagram! nil)))}
   {:pattern #"^I arm the stock placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-stock-placement!))}
   {:pattern #"^I place a stock at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-stock! % x y))))}
   {:pattern #"^the diagram should contain stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when-not (model/stock-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram missing stock " name)))
            world))}
   {:pattern #"^the diagram should contain stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/stock-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram missing stock " name)))
          world)}
   {:pattern #"^the diagram should not contain stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when (model/stock-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram still contains stock " name)))
            world))}
   {:pattern #"^the diagram should not contain stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when (model/stock-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram still contains stock " name)))
          world)}
   {:pattern #"^stock <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (model/stock-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/stock-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name-param x-str y-str] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/stock-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "stock " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^I move stock <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-stock! % name x y))))}
   {:pattern #"^I move stock ([A-Za-z0-9]+) to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-stock! % name x y))))}
   {:pattern #"^I move stock ([A-Za-z0-9]+) to (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (update world :diagram #(cmd/move-stock! % name
                                                   (support/parse-int x-str "x")
                                                   (support/parse-int y-str "y"))))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas position should be <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (canvas/stock-canvas-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "stock " name " canvas position " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas position should be (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (canvas/stock-canvas-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "stock " name " canvas position " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> initial value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (support/require-value example name-param)
                value (support/require-value example value-param)
                actual (model/stock-initial-value (support/diagram-from world) name)]
            (when-not (= value actual)
              (support/fail! (str "stock " name " value " actual " expected " value)))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> initial value should be 0$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)
                actual (model/stock-initial-value (support/diagram-from world) name)]
            (when-not (= "0" actual)
              (support/fail! (str "stock " name " value " actual " expected 0")))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) initial value should be 0$"
    :fn (fn [world [_ name] _]
          (let [actual (model/stock-initial-value (support/diagram-from world) name)]
            (when-not (= "0" actual)
              (support/fail! (str "stock " name " value " actual " expected 0")))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) initial value should be (\d+)$"
    :fn (fn [world [_ name value] _]
          (let [actual (model/stock-initial-value (support/diagram-from world) name)]
            (when-not (= value actual)
              (support/fail! (str "stock " name " value " actual " expected " value)))
            world))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (support/require-value example name-param)
                new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-name! % name new-name))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (support/apply-diagram-edit world #(cmd/set-stock-name! % name new-name)))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name new-name-param] example]
          (let [new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-name! % name new-name))))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> initial value to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (support/require-value example name-param)
                value (support/require-value example value-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-initial-value! % name value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) initial value to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name value-param] example]
          (let [value (support/require-value example value-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-initial-value! % name value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) initial value to (\d+)$"
    :fn (fn [world [_ name value] _]
          (support/apply-diagram-edit world #(cmd/set-stock-initial-value! % name value)))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> minimum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (support/require-value example name-param)
                min-value (support/require-value example min-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-min! % name min-value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) minimum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name min-param] example]
          (let [min-value (support/require-value example min-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-min! % name min-value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) minimum to (\d+)$"
    :fn (fn [world [_ name min-value] _]
          (support/apply-diagram-edit world #(cmd/set-stock-min! % name min-value)))}
   {:pattern #"^I set stock <([A-Za-z0-9_]+)> maximum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (support/require-value example name-param)
                max-value (support/require-value example max-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-max! % name max-value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) maximum to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name max-param] example]
          (let [max-value (support/require-value example max-param)]
            (support/apply-diagram-edit world #(cmd/set-stock-max! % name max-value))))}
   {:pattern #"^I set stock ([A-Za-z0-9]+) maximum to (\d+)$"
    :fn (fn [world [_ name max-value] _]
          (support/apply-diagram-edit world #(cmd/set-stock-max! % name max-value)))}
   {:pattern #"^I clear stock <([A-Za-z0-9_]+)> maximum$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (support/apply-diagram-edit world #(cmd/clear-stock-max! % name))))}
   {:pattern #"^I clear stock ([A-Za-z0-9]+) maximum$"
    :fn (fn [world [_ name] _]
          (support/apply-diagram-edit world #(cmd/clear-stock-max! % name)))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> minimum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (support/require-value example name-param)
                min-value (support/require-value example min-param)
                actual (model/stock-min-value (support/diagram-from world) name)]
            (when-not (= min-value actual)
              (support/fail! (str "stock " name " minimum " actual " expected " min-value)))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) minimum should be (\d+)$"
    :fn (fn [world [_ name min-value] _]
          (let [actual (model/stock-min-value (support/diagram-from world) name)]
            (when-not (= min-value actual)
              (support/fail! (str "stock " name " minimum " actual " expected " min-value)))
            world))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> maximum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (support/require-value example name-param)
                max-value (support/require-value example max-param)
                actual (model/stock-max-value (support/diagram-from world) name)]
            (when-not (= max-value actual)
              (support/fail! (str "stock " name " maximum " actual " expected " max-value)))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) maximum should be (\d+)$"
    :fn (fn [world [_ name max-value] _]
          (let [actual (model/stock-max-value (support/diagram-from world) name)]
            (when-not (= max-value actual)
              (support/fail! (str "stock " name " maximum " actual " expected " max-value)))
            world))}
   {:pattern #"^stock ([A-Za-z0-9]+) should have no maximum$"
    :fn (fn [world [_ name] _]
          (when-let [actual (model/stock-max-value (support/diagram-from world) name)]
            (support/fail! (str "stock " name " maximum " actual " expected none")))
          world)}
   {:pattern #"^the stock edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (support/fail! "expected stock edit to be rejected"))
          world)}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (support/require-value example name-param)
                text (support/require-value example text-param)]
            (support/assert-stock-canvas-label world name :name text)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas name should be ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name text] _]
          (support/assert-stock-canvas-label world name :name text))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas minimum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param min-param] example]
          (let [name (support/require-value example name-param)
                min-value (support/require-value example min-param)]
            (support/assert-stock-canvas-label world name :min min-value)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas minimum should be (\d+)$"
    :fn (fn [world [_ name min-value] _]
          (support/assert-stock-canvas-label world name :min min-value))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> canvas maximum should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param max-param] example]
          (let [name (support/require-value example name-param)
                max-value (support/require-value example max-param)]
            (support/assert-stock-canvas-label world name :max max-value)))}
   {:pattern #"^stock ([A-Za-z0-9]+) canvas maximum should be (\d+)$"
    :fn (fn [world [_ name max-value] _]
          (support/assert-stock-canvas-label world name :max max-value))}
   {:pattern #"^stock ([A-Za-z0-9]+) should display no maximum on canvas$"
    :fn (fn [world [_ name] _]
          (let [labels (canvas/stock-canvas-labels (support/diagram-from world) name)]
            (when-not labels
              (support/fail! (str "stock " name " not on canvas")))
            (when (:max labels)
              (support/fail! (str "stock " name " canvas maximum " (:max labels) " expected none")))
            world))}
   {:pattern #"^the diagram stock count should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (let [count (support/parse-int (support/require-value example count-param) count-param)
                actual (model/stock-count (support/diagram-from world))]
            (when-not (= count actual)
              (support/fail! (str "stock count " actual " expected " count)))
            world))}
   {:pattern #"^the diagram stock count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/stock-count (support/diagram-from world)))
            (support/fail! "expected diagram stock count 0"))
          world)}
   {:pattern #"^the diagram stock count should be 1$"
    :fn (fn [world _ _]
          (when-not (= 1 (model/stock-count (support/diagram-from world)))
            (support/fail! (str "stock count " (model/stock-count (support/diagram-from world)) " expected 1")))
          world)}
   {:pattern #"^the diagram stock count should be 2$"
    :fn (fn [world _ _]
          (when-not (= 2 (model/stock-count (support/diagram-from world)))
            (support/fail! (str "stock count " (model/stock-count (support/diagram-from world)) " expected 2")))
          world)}
   {:pattern #"^the stock placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/placement-disarmed? (support/diagram-from world))
            (support/fail! "expected stock placement tool disarmed"))
          world)}
   {:pattern #"^the stock placement tool should be armed$"
    :fn (fn [world _ _]
          (when-not (= :stock (:placement-mode (support/diagram-from world)))
            (support/fail! "expected stock placement tool armed"))
          world)}
   {:pattern #"^a diagram model with stock <([A-Za-z0-9_]+)> at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name x y))))}
   {:pattern #"^a diagram model with stock ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name (support/parse-int x-str "x") (support/parse-int y-str "y")))))}
   {:pattern #"^stock <([A-Za-z0-9_]+)> at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name x y))))}
   {:pattern #"^stock ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock! diagram name (support/parse-int x-str "x") (support/parse-int y-str "y")))))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> runs from stock <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (support/require-value example flow-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow! diagram flow from to))))}
   {:pattern #"^flow ([A-Za-z0-9]+) runs from stock ([A-Za-z0-9]+) to stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow from to] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow! diagram flow from to))))}
   {:pattern #"^I arm the flow placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-flow-placement!))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :stock name)))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :stock name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :source name)))}
   {:pattern #"^I select sink <([A-Za-z0-9_]+)> as the flow source$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-flow-source! % :sink name))))}
   {:pattern #"^I select sink ([A-Za-z0-9]+) as the flow source$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-flow-source! % :sink name)))}
   {:pattern #"^I select sink <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :sink name))))}
   {:pattern #"^I select sink ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :sink name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the flow destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-flow! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the flow destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-flow! % :source name)))}
   {:pattern #"^the diagram should contain flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param] example]
          (let [flow (support/require-value example flow-param)]
            (when-not (model/flow-exists? (support/diagram-from world) flow)
              (support/fail! (str "diagram missing flow " flow)))
            world))}
   {:pattern #"^the diagram should contain flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow] _]
          (when-not (model/flow-exists? (support/diagram-from world) flow)
            (support/fail! (str "diagram missing flow " flow)))
          world)}
   {:pattern #"^the diagram should not contain flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param] example]
          (let [flow (support/require-value example flow-param)]
            (when (model/flow-exists? (support/diagram-from world) flow)
              (support/fail! (str "diagram still contains flow " flow)))
            world))}
   {:pattern #"^the diagram should not contain flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow] _]
          (when (model/flow-exists? (support/diagram-from world) flow)
            (support/fail! (str "diagram still contains flow " flow)))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (support/require-value example flow-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                endpoints (model/flow-endpoints (support/diagram-from world) flow)]
            (when-not (= [from to] endpoints)
              (support/fail! (str "flow " flow " endpoints " endpoints " expected [" from " " to "]")))
            world))}
   {:pattern #"^flow ([A-Za-z0-9]+) should run from stock ([A-Za-z0-9]+) to stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow from to] _]
          (let [endpoints (model/flow-endpoints (support/diagram-from world) flow)]
            (when-not (= [from to] endpoints)
              (support/fail! (str "flow " flow " endpoints " endpoints " expected [" from " " to "]")))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> rate should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param rate-param] example]
          (let [flow (support/require-value example flow-param)
                rate (support/require-value example rate-param)
                actual (model/flow-rate (support/diagram-from world) flow)]
            (when-not (= rate actual)
              (support/fail! (str "flow " flow " rate " actual " expected " rate)))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> rate should be 0$"
    :fn (fn [world [_ flow-param] example]
          (let [flow (support/require-value example flow-param)
                actual (model/flow-rate (support/diagram-from world) flow)]
            (when-not (= "0" actual)
              (support/fail! (str "flow " flow " rate " actual " expected 0")))
            world))}
   {:pattern #"^flow ([A-Za-z0-9]+) rate should be ([0-9.]+)$"
    :fn (fn [world [_ flow rate] _]
          (let [actual (model/flow-rate (support/diagram-from world) flow)]
            (when-not (= rate actual)
              (support/fail! (str "flow " flow " rate " actual " expected " rate)))
            world))}
   {:pattern #"^flow ([A-Za-z0-9]+) rate should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow rate-param] example]
          (let [rate (support/require-value example rate-param)
                actual (model/flow-rate (support/diagram-from world) flow)]
            (when-not (= rate actual)
              (support/fail! (str "flow " flow " rate " actual " expected " rate)))
            world))}
   {:pattern #"^flow ([A-Za-z0-9]+) rate should be 0$"
    :fn (fn [world [_ flow] _]
          (let [actual (model/flow-rate (support/diagram-from world) flow)]
            (when-not (= "0" actual)
              (support/fail! (str "flow " flow " rate " actual " expected 0")))
            world))}
   {:pattern #"^I set flow <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (support/require-value example name-param)
                new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-flow-name! % name new-name))))}
   {:pattern #"^I set flow ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (support/apply-diagram-edit world #(cmd/set-flow-name! % name new-name)))}
   {:pattern #"^I set flow ([A-Za-z0-9]+) name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name new-name-param] example]
          (let [new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-flow-name! % name new-name))))}
   {:pattern #"^I set flow <([A-Za-z0-9_]+)> rate to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param rate-param] example]
          (let [name (support/require-value example name-param)
                rate (support/require-value example rate-param)]
            (support/apply-diagram-edit world #(cmd/set-flow-rate! % name rate))))}
   {:pattern #"^I set flow ([A-Za-z0-9]+) rate to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name rate-param] example]
          (let [rate (support/require-value example rate-param)]
            (support/apply-diagram-edit world #(cmd/set-flow-rate! % name rate))))}
   {:pattern #"^I set flow ([A-Za-z0-9]+) rate to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name rate] _]
          (support/apply-diagram-edit world #(cmd/set-flow-rate! % name rate)))}
   {:pattern #"^the flow edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (support/fail! "expected flow edit to be rejected"))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (support/require-value example name-param)
                text (support/require-value example text-param)]
            (support/assert-flow-canvas-label world name :name text)))}
   {:pattern #"^flow ([A-Za-z0-9]+) canvas name should be ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name text] _]
          (support/assert-flow-canvas-label world name :name text))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> canvas rate should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param rate-param] example]
          (let [name (support/require-value example name-param)
                rate (support/require-value example rate-param)]
            (support/assert-flow-canvas-label world name :rate rate)))}
   {:pattern #"^flow ([A-Za-z0-9]+) canvas rate should be (\d+)$"
    :fn (fn [world [_ name rate] _]
          (support/assert-flow-canvas-label world name :rate rate))}
   {:pattern #"^the diagram flow count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/flow-count (support/diagram-from world)))
            (support/fail! "expected diagram flow count 0"))
          world)}
   {:pattern #"^the flow placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/flow-placement-disarmed? (support/diagram-from world))
            (support/fail! "expected flow placement tool disarmed"))
          world)}
   {:pattern #"^the flow placement tool should be armed$"
    :fn (fn [world _ _]
          (when-not (model/flow-placement-armed? (support/diagram-from world))
            (support/fail! "expected flow placement tool armed"))
          world)}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from source <([A-Za-z0-9_]+)> to stock <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (support/require-value example flow-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (when-not (= {:kind :source :id from} (model/flow-from diagram flow))
              (support/fail! (str "flow " flow " from mismatch")))
            (when-not (= {:kind :stock :id to} (model/flow-to diagram flow))
              (support/fail! (str "flow " flow " to mismatch")))
            world))}
   {:pattern #"^flow <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to sink <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ flow-param from-param to-param] example]
          (let [flow (support/require-value example flow-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (when-not (= {:kind :stock :id from} (model/flow-from diagram flow))
              (support/fail! (str "flow " flow " from mismatch")))
            (when-not (= {:kind :sink :id to} (model/flow-to diagram flow))
              (support/fail! (str "flow " flow " to mismatch")))
            world))}
  ])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T11:23:50.682198-05:00", :module-hash "-969656619", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-1666922602"} {:id "def/placement-handlers", :kind "def", :line 7, :end-line 566, :hash "1051998448"}]}
;; clj-mutate-manifest-end
