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

(deftest diagram-event-test
  (is (true? (app/diagram-event? events/arm-stock)))
  (is (true? (app/diagram-event? events/arm-flow)))
  (is (true? (app/diagram-event? events/stock-click)))
  (is (false? (app/diagram-event? events/quit))))

(deftest update-shell-for-diagram-event-test
  (let [shell (model/default-shell)]
    (is (= :stock (:placement-mode (:diagram (app/update-shell-for-diagram-event
                                               shell
                                               {:event events/arm-stock})))))
    (is (= :flow (:placement-mode (:diagram (app/update-shell-for-diagram-event
                                               shell
                                               {:event events/arm-flow})))))))

(deftest place-stock-at-coordinates-test
  (let [shell (cmd/arm-stock-placement-on-shell! (model/default-shell))
        placed (app/place-stock-at-coordinates shell [10 20])]
    (is (model/stock-exists? (:diagram placed) "Stock1"))
    (is (= [10 20] (model/stock-position (:diagram placed) "Stock1")))))

(deftest select-flow-stock-from-event-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/arm-flow-placement!))))
        drafted (app/select-flow-stock-from-event
                  shell
                  {:event events/stock-click :stock-name "Stock1"})
        connected (app/select-flow-stock-from-event
                    drafted
                    {:event events/stock-click :stock-name "Stock2"})]
    (is (= {:from "Stock1"} (:flow-draft (:diagram drafted))))
    (is (model/flow-exists? (:diagram connected) "Flow1"))))

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