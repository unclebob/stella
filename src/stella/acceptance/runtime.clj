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
  [feature-name background scenario idx example]
  {:name (:name scenario)
   :feature-name feature-name
   :index idx
   :steps (into (vec background) (:steps scenario))
   :example (or example {})})

(defn plan-scenario-executions
  [{:keys [name background scenarios]}]
  (for [scenario scenarios
        [idx example] (map-indexed vector (scenario-rows scenario))]
    (scenario-execution name background scenario idx example)))

(defn run-feature
  [ir]
  (mapv (fn [{:keys [name feature-name index steps example]}]
          (try
            (let [world (run-steps {:feature-name feature-name} steps example)]
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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:38:20.268534-05:00", :module-hash "1881254806", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-1245636593"} {:id "defn/run-steps", :kind "defn", :line 5, :end-line 10, :hash "-7113136"} {:id "defn/scenario-rows", :kind "defn", :line 12, :end-line 16, :hash "-1202994938"} {:id "defn/scenario-execution", :kind "defn", :line 18, :end-line 23, :hash "430028343"} {:id "defn/plan-scenario-executions", :kind "defn", :line 25, :end-line 29, :hash "304061189"} {:id "defn/run-feature", :kind "defn", :line 31, :end-line 42, :hash "-304848862"} {:id "defn/run-feature-file", :kind "defn", :line 44, :end-line 46, :hash "-1369621380"} {:id "defn/all-passed?", :kind "defn", :line 48, :end-line 50, :hash "-656521916"}]}
;; clj-mutate-manifest-end
