(ns features-model-place-cloud-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_place_cloud_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-place-cloud.json")]
    (is pass (str name " example_" (inc index) ": " error))))
