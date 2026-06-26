(ns stella.app-test
  (:require [clojure.test :refer [deftest is]]
            [stella.app :as app]))

(deftest app-event-action-test
  (is (= :quit (app/app-event-action {:fx/event-type :stella.app/quit})))
  (is (= :show-about (app/app-event-action {:fx/event-type :stella.app/show-about})))
  (is (nil? (app/app-event-action {:fx/event-type :stella.ui/unknown})))
  (is (nil? (app/app-event-action {}))))

(deftest effect-for-action-test
  (is (= :platform-exit (app/effect-for-action :quit)))
  (is (= :about-dialog (app/effect-for-action :show-about)))
  (is (nil? (app/effect-for-action nil)) "Unknown actions are ignored"))