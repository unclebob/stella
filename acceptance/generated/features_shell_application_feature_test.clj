(ns features-shell-application-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_shell_application_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/shell-application.json")]
    (is pass (str name " example_" (inc index) ": " error))))
