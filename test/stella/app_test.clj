(ns stella.app-test
  (:require [clojure.test :refer [deftest is]]
            [stella.app :as app]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.model :as model]))

(deftest event-type-test
  (is (= events/quit (:event {:event events/quit})))
  (is (= events/quit (:event/type {:event/type events/quit}))))

(deftest event-action-test
  (is (= :quit (app/event-action {:event events/quit})))
  (is (= :show-about (app/event-action {:event events/show-about})))
  (is (nil? (app/event-action {:event :stella.ui/unknown}))))

(deftest update-shell-test
  (let [shell (model/default-shell)]
    (is (false? (:showing (app/update-shell shell :quit))))
    (is (:about-visible (app/update-shell shell :show-about)))
    (is (= shell (app/update-shell shell :stella.test/unknown)))))

(deftest process-app-event-test
  (is (= {:action :quit :effect :platform-exit}
         (app/process-app-event {:event events/quit})))
  (is (nil? (app/process-app-event {:event :stella.ui/unknown}))))

(deftest dispatch-map-event-test
  (let [state (atom (model/default-shell))
        effects (atom [])]
    (with-redefs [stella.fx.effects/run-effect #(swap! effects conj %)]
      (app/dispatch-map-event! {:event events/quit} state)
      (app/dispatch-map-event! {:event events/show-about} state))
    (is (false? (:showing @state)))
    (is (:about-visible @state))
    (is (= [:platform-exit :about-dialog] @effects))))