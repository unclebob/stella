(ns features-model-converter-flow-rate-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_converter_flow_rate_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-converter-flow-rate.json")]
    (is pass (str name " example_" (inc index) ": " error))))
