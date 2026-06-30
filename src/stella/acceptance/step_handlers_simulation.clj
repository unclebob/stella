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

(defn- update-world-diagram
  [world diagram]
  (-> world
      (assoc :diagram diagram)
      (sync-shell-diagram diagram)))

(defn- step-world
  [world]
  (update-world-diagram world (cmd/step-simulation! (support/diagram-from world))))

(defn- run-simulation-world
  [world steps]
  (update-world-diagram world
                        (cmd/run-simulation-steps! (support/diagram-from world) steps)))

(defn- click-step-times
  [world clicks]
  (reduce (fn [w _] (step-world w)) world (repeat clicks nil)))

(defn- assert-stock-value
  [world name expected]
  (let [actual (simulation/stock-value (support/diagram-from world) name)]
    (when-not (= (str expected) actual)
      (support/fail! (str "stock " name " value " actual " expected " expected)))
    world))

(defn- assert-simulation-time
  [world expected]
  (let [actual (simulation/format-time
                (simulation/simulation-time (support/diagram-from world)))]
    (when-not (= (str expected) actual)
      (support/fail! (str "simulation time " actual " expected " expected)))
    world))

(defn- assert-simulation-time-display
  [world expected]
  (let [actual (model/simulation-time-display (:shell world))]
    (when-not (= (str expected) actual)
      (support/fail! (str "simulation time display " actual " expected " expected)))
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
          (run-simulation-world world
                                (support/parse-int (support/require-value example steps-param)
                                                   "steps")))}
   {:pattern #"^I run the simulation for (\d+) steps$"
    :fn (fn [world [_ steps-str] _]
          (run-simulation-world world (support/parse-int steps-str "steps")))}
   {:pattern #"^stock ([A-Za-z0-9]+) value should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ name value-param] example]
          (assert-stock-value world name (support/require-value example value-param)))}
   {:pattern #"^stock ([A-Za-z0-9]+) value should be ([0-9.]+)$"
    :fn (fn [world [_ name value] _]
          (assert-stock-value world name value))}
   {:pattern #"^simulation time should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ time-param] example]
          (assert-simulation-time world (support/require-value example time-param)))}
   {:pattern #"^simulation time should be ([0-9.]+)$"
    :fn (fn [world [_ time] _]
          (assert-simulation-time world time))}
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
          (assert-simulation-time-display world (support/require-value example time-param)))}
   {:pattern #"^the simulation time display should show ([0-9.]+)$"
    :fn (fn [world [_ time] _]
          (assert-simulation-time-display world time))}
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
          (click-step-times world
                            (support/parse-int (support/require-value example clicks-param)
                                               "clicks")))}
   {:pattern #"^I click Step (\d+) times$"
    :fn (fn [world [_ clicks-str] _]
          (click-step-times world (support/parse-int clicks-str "clicks")))}])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T08:34:43.58468-05:00", :module-hash "551925947", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "-823618584"} {:id "defn-/sync-shell-diagram", :kind "defn-", :line 7, :end-line 11, :hash "408181774"} {:id "defn-/update-world-diagram", :kind "defn-", :line 13, :end-line 17, :hash "-1107158169"} {:id "defn-/step-world", :kind "defn-", :line 19, :end-line 21, :hash "448535140"} {:id "defn-/run-simulation-world", :kind "defn-", :line 23, :end-line 26, :hash "-1310644101"} {:id "defn-/click-step-times", :kind "defn-", :line 28, :end-line 30, :hash "-1706903199"} {:id "defn-/assert-stock-value", :kind "defn-", :line 32, :end-line 37, :hash "-1833212178"} {:id "defn-/assert-simulation-time", :kind "defn-", :line 39, :end-line 45, :hash "482476428"} {:id "defn-/assert-simulation-time-display", :kind "defn-", :line 47, :end-line 52, :hash "-1662108345"} {:id "def/simulation-handlers", :kind "def", :line 54, :end-line 116, :hash "-2095281290"}]}
;; clj-mutate-manifest-end
