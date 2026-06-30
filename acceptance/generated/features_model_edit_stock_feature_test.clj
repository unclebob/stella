(ns features-model-edit-stock-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_edit_stock_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-edit-stock.json")]
    (is pass (str name " example_" (inc index) ": " error))))
