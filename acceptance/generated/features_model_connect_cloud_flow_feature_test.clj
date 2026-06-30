(ns features-model-connect-cloud-flow-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_connect_cloud_flow_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-connect-cloud-flow.json")]
    (is pass (str name " example_" (inc index) ": " error))))
