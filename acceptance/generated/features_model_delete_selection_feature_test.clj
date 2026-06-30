(ns features-model-delete-selection-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_delete_selection_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-delete-selection.json")]
    (is pass (str name " example_" (inc index) ": " error))))
