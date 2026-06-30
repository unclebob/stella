(ns features-shell-cloud-palette-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_shell_cloud_palette_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/shell-cloud-palette.json")]
    (is pass (str name " example_" (inc index) ": " error))))
