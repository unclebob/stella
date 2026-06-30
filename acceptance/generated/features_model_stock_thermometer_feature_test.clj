(ns features-model-stock-thermometer-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_stock_thermometer_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-stock-thermometer.json")]
    (is pass (str name " example_" (inc index) ": " error))))
