(ns stella.acceptance.mutation-runner
  (:require [stella.acceptance.runtime :as runtime]
            [clojure.data.json :as json])
  (:import [java.io BufferedReader InputStreamReader]))

(defn- response
  [id started-ns outcome output error]
  (json/write-str {:id id
                   :outcome outcome
                   :output output
                   :error error
                   :duration (- (System/nanoTime) started-ns)}))

(defn- run-job
  [{:keys [id feature_json]}]
  (let [started (System/nanoTime)]
    (try
      (let [results (runtime/run-feature-file feature_json)
            passed? (runtime/all-passed? results)]
        (response id started
                  (if passed? "test_success" "test_failure")
                  (pr-str results)
                  ""))
      (catch Exception e
        (response id started "infrastructure_error" "" (.getMessage e))))))

(defn -main
  [& _]
  (let [reader (BufferedReader. (InputStreamReader. System/in))]
    (loop []
      (when-let [line (.readLine reader)]
        (let [job (json/read-str line :key-fn keyword)]
          (println (run-job job))
          (flush)
          (recur))))))