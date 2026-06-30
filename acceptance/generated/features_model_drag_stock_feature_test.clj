(ns features-model-drag-stock-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_drag_stock_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-drag-stock.json")]
    (is pass (str name " example_" (inc index) ": " error))))
