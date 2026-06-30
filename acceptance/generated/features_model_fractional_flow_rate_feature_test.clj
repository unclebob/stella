(ns features-model-fractional-flow-rate-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_fractional_flow_rate_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-fractional-flow-rate.json")]
    (is pass (str name " example_" (inc index) ": " error))))
