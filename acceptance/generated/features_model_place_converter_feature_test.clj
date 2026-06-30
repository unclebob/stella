(ns features-model-place-converter-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_model_place_converter_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/model-place-converter.json")]
    (is pass (str name " example_" (inc index) ": " error))))
