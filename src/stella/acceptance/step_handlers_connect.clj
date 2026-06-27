(ns stella.acceptance.step-handlers-connect
  (:require [stella.acceptance.step-support :as support]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

(def connect-handlers
  [
   {:pattern #"^I arm the source placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-source-placement!))}
   {:pattern #"^I arm the sink placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-sink-placement!))}
   {:pattern #"^I place a source at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-source! % x y))))}
   {:pattern #"^I place a sink at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-sink! % x y))))}
   {:pattern #"^the diagram should contain source <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when-not (model/source-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram missing source " name)))
            world))}
   {:pattern #"^the diagram should contain source ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/source-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram missing source " name)))
          world)}
   {:pattern #"^the diagram should contain sink <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when-not (model/sink-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram missing sink " name)))
            world))}
   {:pattern #"^the diagram should contain sink ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/sink-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram missing sink " name)))
          world)}
   {:pattern #"^source <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (model/source-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "source " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^source ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/source-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "source " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^sink <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (model/sink-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "sink " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^sink ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/sink-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "sink " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^the diagram source count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/source-count (support/diagram-from world)))
            (support/fail! "expected diagram source count 0"))
          world)}
   {:pattern #"^the source placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/source-placement-disarmed? (support/diagram-from world))
            (support/fail! "expected source placement tool disarmed"))
          world)}
   {:pattern #"^source ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-source! diagram name (support/parse-int x-str "x") (support/parse-int y-str "y")))))}
   {:pattern #"^sink ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-sink! diagram name (support/parse-int x-str "x") (support/parse-int y-str "y")))))}
   {:pattern #"^I arm the converter placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-converter-placement!))}
   {:pattern #"^I place a converter at <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/place-converter! % x y))))}
   {:pattern #"^the diagram should contain converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when-not (model/converter-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram missing converter " name)))
            world))}
   {:pattern #"^the diagram should contain converter ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when-not (model/converter-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram missing converter " name)))
          world)}
   {:pattern #"^the diagram should not contain converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when (model/converter-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram still contains converter " name)))
            world))}
   {:pattern #"^the diagram should not contain converter ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when (model/converter-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram still contains converter " name)))
          world)}
   {:pattern #"^converter <([A-Za-z0-9_]+)> should be at position <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (model/converter-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter ([A-Za-z0-9]+) should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/converter-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> should be at position (\d+) (\d+)$"
    :fn (fn [world [_ name-param x-str y-str] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (model/converter-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "converter " name " at " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^I move converter <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-converter! % name x y))))}
   {:pattern #"^I move converter ([A-Za-z0-9]+) to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name x-param y-param] example]
          (let [x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)]
            (update world :diagram #(cmd/move-converter! % name x y))))}
   {:pattern #"^I move converter ([A-Za-z0-9]+) to (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (update world :diagram #(cmd/move-converter! % name
                                                       (support/parse-int x-str "x")
                                                       (support/parse-int y-str "y"))))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> canvas position should be <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param x-param y-param] example]
          (let [name (support/require-value example name-param)
                x (support/parse-int (support/require-value example x-param) x-param)
                y (support/parse-int (support/require-value example y-param) y-param)
                pos (canvas/converter-canvas-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "converter " name " canvas position " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter ([A-Za-z0-9]+) canvas position should be (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [x (support/parse-int x-str "x")
                y (support/parse-int y-str "y")
                pos (canvas/converter-canvas-position (support/diagram-from world) name)]
            (when-not (= [x y] pos)
              (support/fail! (str "converter " name " canvas position " pos " expected [" x " " y "]")))
            world))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param value-param] example]
          (let [name (support/require-value example name-param)
                value (support/require-value example value-param)
                actual (model/converter-value (support/diagram-from world) name)]
            (when-not (= value actual)
              (support/fail! (str "converter " name " value " actual " expected " value)))
            world))}
   {:pattern #"^converter ([A-Za-z0-9]+) value should be ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name value] _]
          (let [actual (model/converter-value (support/diagram-from world) name)]
            (when-not (= value actual)
              (support/fail! (str "converter " name " value " actual " expected " value)))
            world))}
   {:pattern #"^I set converter <([A-Za-z0-9_]+)> name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param new-name-param] example]
          (let [name (support/require-value example name-param)
                new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-converter-name! % name new-name))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) name to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name new-name-param] example]
          (let [new-name (support/require-value example new-name-param)]
            (support/apply-diagram-edit world #(cmd/set-converter-name! % name new-name))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) name to ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name new-name] _]
          (support/apply-diagram-edit world #(cmd/set-converter-name! % name new-name)))}
   {:pattern #"^I set converter <([A-Za-z0-9_]+)> formula to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param formula-param] example]
          (let [name (support/require-value example name-param)
                formula (support/require-value example formula-param)]
            (support/apply-diagram-edit world #(cmd/set-converter-formula! % name formula))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) formula to <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name formula-param] example]
          (let [formula (support/require-value example formula-param)]
            (support/apply-diagram-edit world #(cmd/set-converter-formula! % name formula))))}
   {:pattern #"^I set converter ([A-Za-z0-9]+) formula to (.+)$"
    :fn (fn [world [_ name formula] _]
          (support/apply-diagram-edit world #(cmd/set-converter-formula! % name formula)))}
   {:pattern #"^converter <([A-Za-z0-9_]+)> canvas name should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param text-param] example]
          (let [name (support/require-value example name-param)
                text (support/require-value example text-param)]
            (support/assert-converter-canvas-label world name :name text)))}
   {:pattern #"^converter ([A-Za-z0-9]+) canvas name should be ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name text] _]
          (support/assert-converter-canvas-label world name :name text))}
   {:pattern #"^the converter edit should be rejected$"
    :fn (fn [world _ _]
          (when-not (:last-edit-rejected? world)
            (support/fail! "expected converter edit to be rejected"))
          world)}
   {:pattern #"^the diagram converter count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/converter-count (support/diagram-from world)))
            (support/fail! "expected diagram converter count 0"))
          world)}
   {:pattern #"^the diagram converter count should be 1$"
    :fn (fn [world _ _]
          (when-not (= 1 (model/converter-count (support/diagram-from world)))
            (support/fail! (str "converter count " (model/converter-count (support/diagram-from world)) " expected 1")))
          world)}
   {:pattern #"^the converter placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/converter-placement-disarmed? (support/diagram-from world))
            (support/fail! "expected converter placement tool disarmed"))
          world)}
   {:pattern #"^converter ([A-Za-z0-9]+) at (\d+) (\d+)$"
    :fn (fn [world [_ name x-str y-str] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-converter! diagram name (support/parse-int x-str "x") (support/parse-int y-str "y")))))}
   {:pattern #"^I arm the connector placement tool$"
    :fn (fn [world _ _]
          (update world :diagram cmd/arm-connector-placement!))}
   {:pattern #"^I select converter <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :converter name))))}
   {:pattern #"^I select converter ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :converter name)))}
   {:pattern #"^I select stock <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :stock name))))}
   {:pattern #"^I select stock ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :stock name)))}
   {:pattern #"^I select flow <([A-Za-z0-9_]+)> as the connector origin$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/select-connector-origin! % :flow name))))}
   {:pattern #"^I select flow ([A-Za-z0-9]+) as the connector origin$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/select-connector-origin! % :flow name)))}
   {:pattern #"^I select flow <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :flow name))))}
   {:pattern #"^I select flow ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :flow name)))}
   {:pattern #"^I select converter <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :converter name))))}
   {:pattern #"^I select converter ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :converter name)))}
   {:pattern #"^I select source <([A-Za-z0-9_]+)> as the connector destination$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (update world :diagram #(cmd/connect-connector! % :source name))))}
   {:pattern #"^I select source ([A-Za-z0-9]+) as the connector destination$"
    :fn (fn [world [_ name] _]
          (update world :diagram #(cmd/connect-connector! % :source name)))}
   {:pattern #"^the diagram should contain connector <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when-not (model/connector-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram missing connector " name)))
            world))}
   {:pattern #"^the diagram should not contain connector <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name-param] example]
          (let [name (support/require-value example name-param)]
            (when (model/connector-exists? (support/diagram-from world) name)
              (support/fail! (str "diagram still contains connector " name)))
            world))}
   {:pattern #"^the diagram should not contain connector ([A-Za-z0-9]+)$"
    :fn (fn [world [_ name] _]
          (when (model/connector-exists? (support/diagram-from world) name)
            (support/fail! (str "diagram still contains connector " name)))
          world)}
   {:pattern #"^connector <([A-Za-z0-9_]+)> runs from converter <([A-Za-z0-9_]+)> to flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (support/require-value example connector-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-connector! diagram connector from to))))}
   {:pattern #"^connector ([A-Za-z0-9]+) runs from converter ([A-Za-z0-9]+) to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector from to] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-connector! diagram connector from to))))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> runs from stock <([A-Za-z0-9_]+)> to converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (support/require-value example connector-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock-connector! diagram connector from to))))}
   {:pattern #"^connector ([A-Za-z0-9]+) runs from stock ([A-Za-z0-9]+) to converter ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector from to] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-stock-connector! diagram connector from to))))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> formula should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param formula-param] example]
          (let [connector (support/require-value example connector-param)
                formula (support/require-value example formula-param)
                actual (model/connector-formula (support/diagram-from world) connector)]
            (when-not (= formula actual)
              (support/fail! (str "connector " connector " formula " actual " expected " formula)))
            world))}
   {:pattern #"^connector ([A-Za-z0-9]+) formula should be (.+)$"
    :fn (fn [world [_ connector formula] _]
          (let [actual (model/connector-formula (support/diagram-from world) connector)]
            (when-not (= formula actual)
              (support/fail! (str "connector " connector " formula " actual " expected " formula)))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should have no formula$"
    :fn (fn [world [_ connector-param] example]
          (let [connector (support/require-value example connector-param)
                actual (model/connector-formula (support/diagram-from world) connector)]
            (when (seq actual)
              (support/fail! (str "connector " connector " formula " actual " expected none")))
            world))}
   {:pattern #"^connector ([A-Za-z0-9]+) should have no formula$"
    :fn (fn [world [_ connector] _]
          (when (seq (model/connector-formula (support/diagram-from world) connector))
            (support/fail! (str "connector " connector " formula expected none")))
          world)}
   {:pattern #"^connector <([A-Za-z0-9_]+)> canvas formula should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param formula-param] example]
          (let [connector (support/require-value example connector-param)
                formula (support/require-value example formula-param)]
            (support/assert-connector-canvas-label world connector :formula formula)))}
   {:pattern #"^connector ([A-Za-z0-9]+) canvas formula should be (.+)$"
    :fn (fn [world [_ connector formula] _]
          (support/assert-connector-canvas-label world connector :formula formula))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from converter <([A-Za-z0-9_]+)> to flow <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (support/require-value example connector-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (support/fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (support/fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector ([A-Za-z0-9]+) should run from converter ([A-Za-z0-9]+) to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector from to] _]
          (let [diagram (support/diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (support/fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (support/fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from converter <([A-Za-z0-9_]+)> to flow ([A-Za-z0-9]+)$"
    :fn (fn [world [_ connector-param from-param to] example]
          (let [connector (support/require-value example connector-param)
                from (support/require-value example from-param)
                diagram (support/diagram-from world)]
            (when-not (= {:kind :converter :id from} (model/connector-from diagram connector))
              (support/fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :flow :id to} (model/connector-to diagram connector))
              (support/fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^connector <([A-Za-z0-9_]+)> should run from stock <([A-Za-z0-9_]+)> to converter <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ connector-param from-param to-param] example]
          (let [connector (support/require-value example connector-param)
                from (support/require-value example from-param)
                to (support/require-value example to-param)
                diagram (support/diagram-from world)]
            (when-not (= {:kind :stock :id from} (model/connector-from diagram connector))
              (support/fail! (str "connector " connector " from mismatch")))
            (when-not (= {:kind :converter :id to} (model/connector-to diagram connector))
              (support/fail! (str "connector " connector " to mismatch")))
            world))}
   {:pattern #"^the diagram connector count should be 0$"
    :fn (fn [world _ _]
          (when-not (zero? (model/connector-count (support/diagram-from world)))
            (support/fail! "expected diagram connector count 0"))
          world)}
   {:pattern #"^the connector placement tool should be armed$"
    :fn (fn [world _ _]
          (when-not (model/connector-placement-armed? (support/diagram-from world))
            (support/fail! "expected connector placement tool armed"))
          world)}
   {:pattern #"^the connector placement tool should be disarmed$"
    :fn (fn [world _ _]
          (when-not (model/connector-placement-disarmed? (support/diagram-from world))
            (support/fail! "expected connector placement tool disarmed"))
          world)}
  ])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:28:03.763581-05:00", :module-hash "373104817", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-1285667419"} {:id "def/connect-handlers", :kind "def", :line 7, :end-line 431, :hash "942905518"}]}
;; clj-mutate-manifest-end
