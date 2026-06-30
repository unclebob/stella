(ns features-shell-menu-feature-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest features_shell_menu_feature_acceptance
  (doseq [{:keys [pass name index error]} (runtime/run-feature-file "build/acceptance/ir/shell-menu.json")]
    (is pass (str name " example_" (inc index) ": " error))))
