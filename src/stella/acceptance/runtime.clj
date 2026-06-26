(ns stella.acceptance.runtime
  (:require [stella.acceptance.steps :as steps]
            [clojure.data.json :as json]))

(defn run-steps
  [world steps example]
  (reduce (fn [w step]
            (steps/dispatch-step w step example))
          world
          steps))

(defn scenario-rows
  [scenario]
  (if (seq (:examples scenario))
    (:examples scenario)
    [{}]))

(defn scenario-execution
  [background scenario idx example]
  {:name (:name scenario)
   :index idx
   :steps (into (vec background) (:steps scenario))
   :example (or example {})})

(defn plan-scenario-executions
  [{:keys [background scenarios]}]
  (for [scenario scenarios
        [idx example] (map-indexed vector (scenario-rows scenario))]
    (scenario-execution background scenario idx example)))

(defn run-feature
  [ir]
  (mapv (fn [{:keys [name index steps example]}]
          (try
            (let [world (run-steps {} steps example)]
              {:name name :index index :pass true :world world})
            (catch Exception e
              {:name name
               :index index
               :pass false
               :error (.getMessage e)})))
        (plan-scenario-executions ir)))

(defn run-feature-file
  [ir-path]
  (run-feature (json/read-str (slurp ir-path) :key-fn keyword)))

(defn all-passed?
  [results]
  (every? :pass results))