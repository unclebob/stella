(ns stella.acceptance.step-handlers-simulation
  (:require [stella.acceptance.step-support :as support]
            [stella.commands :as cmd]
            [stella.model :as model]
            [stella.simulation :as simulation]))

(defn- sync-shell-diagram
  [world diagram]
  (if (:shell world)
    (update world :shell assoc :diagram diagram)
    world))

(defn- step-world
  [world]
  (let [diagram (cmd/step-simulation! (support/diagram-from world))]
    (-> world
        (assoc :diagram diagram)
        (sync-shell-diagram diagram))))

(defn- assert-stock-value
  [world name expected]
  (let [actual (simulation/stock-value (support/diagram-from world) name)]
    (when-not (= (str expected) actual)
      (support/fail! (str "stock " name " value " actual " expected " expected)))
    world))

(def simulation-handlers
  [{:pattern #"^flow ([A-Za-z0-9]+) runs from source ([A-Za-z0-9]+) to stock ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow source stock] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow-from-source! diagram flow source stock))))}
   {:pattern #"^flow ([A-Za-z0-9]+) runs from stock ([A-Za-z0-9]+) to sink ([A-Za-z0-9]+)$"
    :fn (fn [world [_ flow stock sink] _]
          (let [diagram (support/diagram-from world)]
            (assoc world :diagram (cmd/fixture-flow-to-sink! diagram flow stock sink))))}
   {:pattern #"^I run the simulation for <([A-Za-z0-9_]+)> steps$"
    :fn (fn [world [_ steps-param] example]
          (let [steps (support/parse-int (support/require-value example steps-param) "steps")
                diagram (cmd/run-simulation-steps! (support/diagram-from world) steps)]
            (-> world
                (assoc :diagram diagram)
                (sync-shell-diagram diagram))))}
   {:pattern #"^I run the simulation for (\d+) steps$"
    :fn (fn [world [_ steps-str] _]
          (let [steps (support/parse-int steps-str "steps")
                diagram (cmd/run-simulation-steps! (support/diagram-from world) steps)]
            (-> world
                (assoc :diagram diagram)
                (sync-shell-diagram diagram))))}
   {:pattern #"^stock ([A-Za-z0-9]+) value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name value-param] example]
          (assert-stock-value world name (support/require-value example value-param)))}
   {:pattern #"^stock ([A-Za-z0-9]+) value should be ([0-9.]+)$"
    :fn (fn [world [_ name value] _]
          (assert-stock-value world name value))}
   {:pattern #"^simulation time should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ time-param] example]
          (let [expected (support/require-value example time-param)
                actual (simulation/format-time
                        (simulation/simulation-time (support/diagram-from world)))]
            (when-not (= (str expected) actual)
              (support/fail! (str "simulation time " actual " expected " expected)))
            world))}
   {:pattern #"^simulation time should be ([0-9.]+)$"
    :fn (fn [world [_ time] _]
          (let [actual (simulation/format-time
                        (simulation/simulation-time (support/diagram-from world)))]
            (when-not (= time actual)
              (support/fail! (str "simulation time " actual " expected " time)))
            world))}
   {:pattern #"^the control panel should be visible$"
    :fn (fn [world _ _]
          (when-not (model/control-panel-visible? (:shell world))
            (support/fail! "expected control panel visible"))
          world)}
   {:pattern #"^the Step button should be visible$"
    :fn (fn [world _ _]
          (when-not (model/step-button-visible? (:shell world))
            (support/fail! "expected Step button visible"))
          world)}
   {:pattern #"^the simulation time display should show <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ time-param] example]
          (let [expected (support/require-value example time-param)
                actual (simulation/shell-time-display (:shell world))]
            (when-not (= (str expected) actual)
              (support/fail! (str "simulation time display " actual " expected " expected)))
            world))}
   {:pattern #"^the simulation time display should show ([0-9.]+)$"
    :fn (fn [world [_ time] _]
          (let [actual (simulation/shell-time-display (:shell world))]
            (when-not (= time actual)
              (support/fail! (str "simulation time display " actual " expected " time)))
            world))}
   {:pattern #"^I click in the control panel$"
    :fn (fn [world _ _]
          (if (:shell world)
            (update world :shell cmd/click-in-control-panel-on-shell!)
            world))}
   {:pattern #"^I drag stock ([A-Za-z0-9]+) within the control panel$"
    :fn (fn [world [_ stock-name] _]
          (if (:shell world)
            (update world :shell #(cmd/drag-stock-within-control-panel-on-shell! % stock-name))
            world))}
   {:pattern #"^I click Step <([A-Za-z0-9_]+)> times$"
    :fn (fn [world [_ clicks-param] example]
          (let [clicks (support/parse-int (support/require-value example clicks-param) "clicks")]
            (reduce (fn [w _] (step-world w)) world (repeat clicks nil))))}
   {:pattern #"^I click Step (\d+) times$"
    :fn (fn [world [_ clicks-str] _]
          (let [clicks (support/parse-int clicks-str "clicks")]
            (reduce (fn [w _] (step-world w)) world (repeat clicks nil))))}])