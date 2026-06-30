(ns features-shell-control-panel-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_shell_control_panel_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/shell-control-panel.json")]
    (is pass (str name " example_" (inc index) ": " error))))
