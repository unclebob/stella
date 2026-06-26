(ns stella.app-test
  (:require [clojure.test :refer [deftest is]]
            [stella.app :as app]
            [stella.events :as events]
            [stella.model :as model]))

(deftest dispatch-map-event-test
  (let [state (atom (model/default-shell))
        effects (atom [])]
    (with-redefs [stella.fx.effects/run-effect #(swap! effects conj %)]
      (app/dispatch-map-event! {:event events/quit} state)
      (app/dispatch-map-event! {:event events/show-about} state)
      (app/dispatch-map-event! {:event events/arm-stock} state))
    (is (false? (:showing @state)))
    (is (:about-visible @state))
    (is (= :stock (:placement-mode (:diagram @state))))
    (is (= [:platform-exit :about-dialog] @effects))))