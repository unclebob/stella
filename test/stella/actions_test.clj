(ns stella.actions-test
  (:require [clojure.test :refer [deftest is]]
            [stella.actions :as actions]
            [stella.events :as events]))

(deftest event->action-test
  (is (= :quit (actions/event->action events/quit)))
  (is (= :show-about (actions/event->action events/show-about)))
  (is (nil? (actions/event->action :stella.ui/unknown)))
  (is (nil? (actions/event->action nil))))

(deftest action->effect-test
  (is (= :platform-exit (actions/action->effect :quit)))
  (is (= :about-dialog (actions/action->effect :show-about)))
  (is (nil? (actions/action->effect nil)) "Unknown actions are ignored"))