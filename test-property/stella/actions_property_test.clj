(ns stella.actions-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [clojure.test.check.generators :as gen]
            [stella.actions :as actions]
            [stella.events :as events]))

(def ^:private known-events [events/quit events/show-about])

(def ^:private known-actions [:quit :show-about])

(defspec unknown-events-are-ignored
  100
  (for-all [event gen/keyword]
    (or (contains? (set known-events) event)
        (= nil (actions/event->action event)))))

(defspec known-events-map-to-actions
  100
  (for-all [event (gen/elements known-events)]
    (some? (actions/event->action event))))

(defspec known-actions-map-to-effects
  100
  (for-all [action (gen/elements known-actions)]
    (some? (actions/action->effect action))))

(defspec action-effect-round-trip
  100
  (for-all [event (gen/elements known-events)]
    (let [action (actions/event->action event)
          effect (actions/action->effect action)]
      (and (some? action)
           (some? effect)
           (= effect (cond
                       (= event events/quit) :platform-exit
                       (= event events/show-about) :about-dialog
                       :else nil))))))

(deftest ignored-event-samples
  (is (nil? (actions/event->action :stella.events/not-real))))