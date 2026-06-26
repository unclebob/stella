(ns stella.dispatch-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
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

(deftest diagram-event-test
  (is (true? (dispatch/diagram-event? events/arm-stock)))
  (is (true? (dispatch/diagram-event? events/arm-flow)))
  (is (true? (dispatch/diagram-event? events/stock-click)))
  (is (true? (dispatch/diagram-event? events/canvas-click)))
  (is (false? (dispatch/diagram-event? events/quit))))

(deftest apply-event-arm-placement-test
  (let [shell (model/default-shell)]
    (is (= :stock (:placement-mode (:diagram (dispatch/apply-event
                                                shell
                                                {:event events/arm-stock})))))
    (is (= :flow (:placement-mode (:diagram (dispatch/apply-event
                                               shell
                                               {:event events/arm-flow})))))))

(deftest apply-event-place-stock-test
  (let [shell (cmd/arm-stock-placement-on-shell! (model/default-shell))
        placed (dispatch/apply-event shell {:event events/canvas-click
                                            :coordinates [10 20]})]
    (is (model/stock-exists? (:diagram placed) "Stock1"))
    (is (= [10 20] (model/stock-position (:diagram placed) "Stock1")))))

(deftest apply-event-connect-flow-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/arm-flow-placement!)))
        drafted (dispatch/apply-event shell {:event events/stock-click
                                             :stock-name "Stock1"})
        connected (dispatch/apply-event drafted {:event events/stock-click
                                                 :stock-name "Stock2"})]
    (is (= {:from "Stock1"} (:flow-draft (:diagram drafted))))
    (is (model/flow-exists? (:diagram connected) "Flow1"))))