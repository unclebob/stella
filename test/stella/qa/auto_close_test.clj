(ns stella.qa.auto-close-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.qa.args :as qa-args]
            [stella.qa.auto-close :as auto-close]))

(deftest configured-seconds-reads-property-test
  (testing "with property set"
    (System/setProperty "stella.qa.auto-close-seconds" "12")
    (is (= 12 (auto-close/configured-seconds))))
  (testing "without property"
    (System/clearProperty "stella.qa.auto-close-seconds")
    (is (nil? (auto-close/configured-seconds)))))

(deftest daemon-thread-factory-marks-threads-daemon-test
  (let [factory (#'auto-close/daemon-thread-factory)
        thread (.newThread factory (fn []))]
    (is (.isDaemon thread))))

(deftest schedule-and-shutdown-clears-executor-test
  (System/setProperty "stella.qa.auto-close-seconds" "30")
  (try
    (auto-close/schedule-if-configured!)
    (is (qa-args/qa-mode?))
    (is (some? @@#'auto-close/executor))
    (auto-close/shutdown-executor!)
    (is (nil? @@#'auto-close/executor))
    (finally
      (System/clearProperty "stella.qa.auto-close-seconds")
      (auto-close/shutdown-executor!))))