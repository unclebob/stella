(ns features-shell-simulation-run-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_shell_simulation_run_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/shell-simulation-run.json")]
    (is pass (str name " example_" (inc index) ": " error))))
