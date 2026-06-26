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
  (is (true? (app/diagram-event? events/arm-source)))
  (is (true? (app/diagram-event? events/arm-converter)))
  (is (true? (app/diagram-event? events/arm-connector)))
  (is (true? (app/diagram-event? events/endpoint-click)))
  (is (false? (app/diagram-event? events/quit))))

(deftest update-shell-for-diagram-event-test
  (let [shell (model/default-shell)]
    (is (= :stock (:placement-mode (:diagram (app/update-shell-for-diagram-event
                                               shell
                                               {:event events/arm-stock})))))
    (is (= :flow (:placement-mode (:diagram (app/update-shell-for-diagram-event
                                               shell
                                               {:event events/arm-flow})))))
    (is (= :source (:placement-mode (:diagram (app/update-shell-for-diagram-event
                                                 shell
                                                 {:event events/arm-source})))))))

(deftest place-stock-at-coordinates-test
  (let [shell (cmd/arm-stock-placement-on-shell! (model/default-shell))
        placed (app/place-stock-at-coordinates shell [10 20])]
    (is (model/stock-exists? (:diagram placed) "Stock1"))
    (is (= [10 20] (model/stock-position (:diagram placed) "Stock1")))))

(deftest place-on-canvas-test
  (let [stock-shell (cmd/arm-stock-placement-on-shell! (model/default-shell))
        source-shell (cmd/arm-source-placement-on-shell! (model/default-shell))
        sink-shell (cmd/arm-sink-placement-on-shell! (model/default-shell))
        converter-shell (cmd/arm-converter-placement-on-shell! (model/default-shell))
        idle-shell (model/default-shell)]
    (is (model/stock-exists? (:diagram (app/place-on-canvas stock-shell 10 20)) "Stock1"))
    (is (model/source-exists? (:diagram (app/place-on-canvas source-shell 50 150)) "Source1"))
    (is (model/sink-exists? (:diagram (app/place-on-canvas sink-shell 400 150)) "Sink1"))
    (is (model/converter-exists? (:diagram (app/place-on-canvas converter-shell 100 250)) "Converter1"))
    (is (= idle-shell (app/place-on-canvas idle-shell 1 2)))))

(deftest select-endpoint-from-event-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/arm-flow-placement!))))
        drafted (app/select-endpoint-from-event
                  shell
                  {:event events/endpoint-click
                   :endpoint-kind :stock
                   :endpoint-name "Stock1"})
        connected (app/select-endpoint-from-event
                    drafted
                    {:event events/endpoint-click
                     :endpoint-kind :stock
                     :endpoint-name "Stock2"})]
    (is (= {:kind :stock :id "Stock1"} (:from (:flow-draft (:diagram drafted)))))
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