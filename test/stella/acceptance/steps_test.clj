(ns stella.acceptance.steps-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.acceptance.runtime :as runtime]
            [stella.commands :as cmd]
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

(deftest enabled-menu-item-must-exist-test
  (let [world {:shell (model/default-shell)}]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"unknown menu item: QUit"
         (runtime/run-steps world
                            [{:keyword "Then"
                              :text "the shell menu item <item> should be enabled"}]
                            {"item" "QUit"})))))

(deftest about-text-requires-exact-app-name-test
  (let [world (-> (run-step "a default shell application" {})
                  (update :shell #(assoc % :about-visible true :about-text "Stella\nsubtitle")))]
    (is (runtime/run-steps world
                           [{:keyword "Then"
                             :text "the about dialog text should include <app_name>"}]
                           {"app_name" "Stella"}))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"about text expected first line StelLa"
         (runtime/run-steps world
                            [{:keyword "Then"
                              :text "the about dialog text should include <app_name>"}]
                            {"app_name" "StelLa"})))))

(deftest converter-formula-edit-requires-stock-connector-test
  (let [world {:feature-name "Converter flow rate"
               :diagram (-> (cmd/default-diagram! nil)
                            (cmd/fixture-stock! "Stock1" 100 100)
                            (cmd/fixture-stock! "Stock2" 300 200)
                            (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                            (cmd/fixture-converter! "Converter1" 100 250)
                            (cmd/fixture-connector! "Connector1" "Converter1" "Flow1"))}
        result (runtime/run-steps world
                                  [{:keyword "When"
                                    :text "I set converter Converter1 formula to Stock1 * 0.1"}]
                                  {})]
    (is (:last-edit-rejected? result))))

(deftest simulation-run-steps-test
  (let [world (run-step "a default shell application" {})
        running (runtime/run-steps world
                                   [{:keyword "When" :text "I set simulation tick delay to 0"}
                                    {:keyword "And" :text "I click Run"}
                                    {:keyword "And" :text "3 simulation run ticks elapse"}]
                                   {})]
    (is (model/simulation-running? (:shell running)))
    (is (= "Stop" (model/run-button-label (:shell running))))
    (is (= "0.3" (model/simulation-time-display (:shell running))))
    (let [stopped (runtime/run-steps running
                                     [{:keyword "When" :text "I click Stop"}
                                      {:keyword "And" :text "2 simulation run ticks elapse"}]
                                     {})]
      (is (not (model/simulation-running? (:shell stopped))))
      (is (= "Run" (model/run-button-label (:shell stopped))))
      (is (= "0.3" (model/simulation-time-display (:shell stopped)))))))
