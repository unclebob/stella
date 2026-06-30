(ns features-model-connect-connector-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_connect_connector_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-connect-connector.json")]
    (is pass (str name " example_" (inc index) ": " error))))
