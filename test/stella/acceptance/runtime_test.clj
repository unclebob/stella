(ns stella.acceptance.runtime-test
  (:require [clojure.test :refer [deftest is]]
            [stella.acceptance.runtime :as runtime]))

(deftest scenario-rows-test
  (is (= [{}] (runtime/scenario-rows {:name "solo"})))
  (is (= [{:menu "File"}]
         (runtime/scenario-rows {:name "table" :examples [{:menu "File"}]}))))

(deftest plan-scenario-executions-test
  (let [ir {:background [{:text "given"}]
            :scenarios [{:name "Menu"
                         :steps [{:text "then"}]
                         :examples [{:menu "File"} {:menu "Help"}]}]}
        plans (vec (runtime/plan-scenario-executions ir))]
    (is (= 2 (count plans)))
    (is (= [{:text "given"} {:text "then"}] (:steps (first plans))))
    (is (= {:menu "File"} (:example (first plans))))
    (is (= 1 (:index (second plans))))))