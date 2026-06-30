(ns features-model-select-objects-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_select_objects_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-select-objects.json")]
    (is pass (str name " example_" (inc index) ": " error))))
