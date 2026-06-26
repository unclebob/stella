(ns stella.dispatch-test
  (:require [clojure.test :refer [deftest is]]
            [stella.dispatch :as dispatch]
            [stella.events :as events]
            [stella.model :as model]))

(deftest event-type-test
  (is (= events/quit (:event {:event events/quit})))
  (is (= events/quit (:event/type {:event/type events/quit}))))

(deftest event->action-test
  (is (= :quit (dispatch/event->action {:event events/quit})))
  (is (= :show-about (dispatch/event->action {:event events/show-about})))
  (is (nil? (dispatch/event->action {:event :stella.ui/unknown}))))

(deftest apply-action-test
  (let [shell (model/default-shell)]
    (is (false? (:showing (dispatch/apply-action shell :quit))))
    (is (:about-visible (dispatch/apply-action shell :show-about)))
    (is (= shell (dispatch/apply-action shell :stella.test/unknown)))))

(deftest process-event-test
  (is (= {:action :quit :effect :platform-exit}
         (dispatch/process-event {:event events/quit})))
  (is (nil? (dispatch/process-event {:event :stella.ui/unknown}))))