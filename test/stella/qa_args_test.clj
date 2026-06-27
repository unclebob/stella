(ns stella.qa-args-test
  (:require [clojure.test :refer [deftest is]]
            [stella.qa.args :as qa-args]))

(deftest parse-qa-flag-test
  (is (= {:qa-seconds nil :args ["shell-launch"]}
         (qa-args/parse-qa-flag ["shell-launch"])))
  (is (= {:qa-seconds 90 :args ["shell-launch"]}
         (qa-args/parse-qa-flag ["--qa" "90" "shell-launch"])))
  (is (= {:qa-seconds 30 :args []}
         (qa-args/parse-qa-flag ["--qa" "30"]))))