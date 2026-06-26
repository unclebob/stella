(ns stella.dispatch-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.dispatch :as dispatch]
            [stella.events :as events]
            [stella.model :as model]))

(def ^:private known-events [events/quit events/show-about])

(defspec unknown-events-are-ignored
  100
  (for-all [event gen/keyword]
    (or (contains? (set known-events) event)
        (nil? (dispatch/event->action {:event event})))))

(defspec known-events-produce-actions-and-effects
  100
  (for-all [event (gen/elements known-events)]
    (let [result (dispatch/process-event {:event event})]
      (and (contains? result :action)
           (contains? result :effect)))))

(defspec quit-hides-shell
  25
  (prop/for-all [_ gen/int]
    (false? (:showing (dispatch/apply-action (model/default-shell) :quit)))))

(defspec show-about-marks-visible
  25
  (prop/for-all [_ gen/int]
    (true? (:about-visible (dispatch/apply-action (model/default-shell) :show-about)))))

(deftest unknown-action-preserves-shell
  (is (= (model/default-shell)
         (dispatch/apply-action (model/default-shell) :stella.test/unknown))))