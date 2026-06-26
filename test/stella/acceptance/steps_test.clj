(ns stella.acceptance.steps-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.acceptance.runtime :as runtime]
            [stella.model :as model]))

(defn- run-step [text example]
  (runtime/run-steps {} [{:keyword "Given" :text text}] example))

(deftest given-default-shell-test
  (let [world (run-step "a default shell application" {})]
    (is (= (model/default-shell) (:shell world)))))

(deftest menu-include-step-test
  (let [world {:shell (model/default-shell)}]
    (is (runtime/run-steps world
                          [{:keyword "Then"
                            :text "the shell menu bar should include <menu>"}]
                          {"menu" "File"}))))

(deftest quit-step-test
  (let [world (run-step "a default shell application" {})
        result (runtime/run-steps world
                                  [{:keyword "When"
                                    :text "I quit the shell application"}]
                                  {})]
    (is (false? (:showing (:shell result))))))