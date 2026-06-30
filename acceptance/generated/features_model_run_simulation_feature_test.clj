(ns features-model-run-simulation-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_run_simulation_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-run-simulation.json")]
    (is pass (str name " example_" (inc index) ": " error))))
