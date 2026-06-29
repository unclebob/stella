(ns stella.dispatch-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.dispatch :as dispatch]
            [stella.events :as events]
            [stella.fx.input :as fx-input]
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
  (is (true? (dispatch/diagram-event? events/arm-source)))
  (is (true? (dispatch/diagram-event? events/arm-sink)))
  (is (true? (dispatch/diagram-event? events/arm-converter)))
  (is (true? (dispatch/diagram-event? events/arm-connector)))
  (is (true? (dispatch/diagram-event? events/endpoint-click)))
  (is (true? (dispatch/diagram-event? events/canvas-click)))
  (is (false? (dispatch/diagram-event? events/quit))))

(deftest apply-event-arm-placement-test
  (let [shell (model/default-shell)]
    (is (= :stock (:placement-mode (:diagram (dispatch/apply-event
                                                shell
                                                {:event events/arm-stock})))))
    (is (= :flow (:placement-mode (:diagram (dispatch/apply-event
                                               shell
                                               {:event events/arm-flow})))))
    (is (= :source (:placement-mode (:diagram (dispatch/apply-event
                                                 shell
                                                 {:event events/arm-source})))))
    (is (= :sink (:placement-mode (:diagram (dispatch/apply-event
                                               shell
                                               {:event events/arm-sink})))))
    (is (= :converter (:placement-mode (:diagram (dispatch/apply-event
                                                    shell
                                                    {:event events/arm-converter})))))
    (is (= :connector (:placement-mode (:diagram (dispatch/apply-event
                                                     shell
                                                     {:event events/arm-connector})))))))

(deftest apply-event-place-on-canvas-test
  (let [stock-shell (cmd/arm-stock-placement-on-shell! (model/default-shell))
        source-shell (cmd/arm-source-placement-on-shell! (model/default-shell))
        sink-shell (cmd/arm-sink-placement-on-shell! (model/default-shell))
        converter-shell (cmd/arm-converter-placement-on-shell! (model/default-shell))
        idle-shell (model/default-shell)]
    (is (model/stock-exists? (:diagram (dispatch/apply-event
                                          stock-shell
                                          {:event events/canvas-click
                                           :coordinates [10 20]}))
                              "Stock1"))
    (is (model/source-exists? (:diagram (dispatch/apply-event
                                           source-shell
                                           {:event events/canvas-click
                                            :coordinates [50 150]}))
                               "Source1"))
    (is (model/sink-exists? (:diagram (dispatch/apply-event
                                         sink-shell
                                         {:event events/canvas-click
                                          :coordinates [400 150]}))
                             "Sink1"))
    (is (model/converter-exists? (:diagram (dispatch/apply-event
                                              converter-shell
                                              {:event events/canvas-click
                                               :coordinates [100 250]}))
                                  "Converter1"))
    (is (= idle-shell (dispatch/apply-event idle-shell
                                             {:event events/canvas-click
                                              :coordinates [1 2]})))))

(deftest apply-event-connect-flow-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/arm-flow-placement!))))
        drafted (dispatch/apply-event shell {:event events/endpoint-click
                                             :endpoint-kind :stock
                                             :endpoint-name "Stock1"})
        connected (dispatch/apply-event drafted {:event events/endpoint-click
                                                 :endpoint-kind :stock
                                                 :endpoint-name "Stock2"})]
    (is (= {:kind :stock :id "Stock1"} (:from (:flow-draft (:diagram drafted)))))
    (is (model/flow-exists? (:diagram connected) "Flow1"))))

(deftest apply-event-connect-connector-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 200 150)
                                        (cmd/fixture-stock! "Stock2" 350 150)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                                        (cmd/fixture-converter! "Converter1" 100 250)
                                        (cmd/arm-connector-placement!))))
        drafted (dispatch/apply-event shell {:event events/endpoint-click
                                             :endpoint-kind :converter
                                             :endpoint-name "Converter1"})
        connected (dispatch/apply-event drafted {:event events/endpoint-click
                                                 :endpoint-kind :flow
                                                 :endpoint-name "Flow1"})]
     (is (= {:kind :converter :id "Converter1"} (:from (:connector-draft (:diagram drafted)))))
     (is (model/connector-exists? (:diagram connected) "Connector1"))))

(deftest stock-drag-shell-lifecycle-test
  (let [shell (-> (model/default-shell)
                  (update :diagram cmd/fixture-stock! "Stock1" 100 100))
        started (cmd/start-stock-drag-on-shell!
                 shell
                 {:stock-name "Stock1"
                  :scene-coordinates [10 20]})
        moved (cmd/end-stock-drag-on-shell!
               started
               {:scene-coordinates [40 70]})]
    (is (= {:name "Stock1"
            :start-x 100
            :start-y 100
            :press-scene-x 10
            :press-scene-y 20}
           (:stock-drag started)))
    (is (= [130 150] (model/stock-position (:diagram moved) "Stock1")))
    (is (not (contains? moved :stock-drag)))))

(deftest stock-drag-shell-no-op-paths-test
  (let [shell (-> (model/default-shell)
                  (update :diagram cmd/fixture-stock! "Stock1" 100 100))
        armed-shell (cmd/arm-stock-placement-on-shell! shell)]
    (is (= shell (cmd/start-stock-drag-on-shell! shell {:stock-name "Stock1"})))
    (is (= armed-shell (cmd/start-stock-drag-on-shell!
                  armed-shell
                  {:stock-name "Stock1" :scene-coordinates [10 20]})))
    (is (= shell (cmd/end-stock-drag-on-shell! shell {:scene-coordinates [10 20]})))
    (is (not (contains? (cmd/end-stock-drag-on-shell!
                         (assoc shell :stock-drag {:name "Stock1"})
                         {})
                        :stock-drag)))))

(deftest converter-drag-shell-lifecycle-test
  (let [shell (-> (model/default-shell)
                  (update :diagram cmd/fixture-converter! "Converter1" 100 250))
        started (cmd/start-converter-drag-on-shell!
                 shell
                 {:converter-name "Converter1"
                  :scene-coordinates [10 20]})
        moved (cmd/end-converter-drag-on-shell!
               started
               {:scene-coordinates [40 70]})]
    (is (= {:name "Converter1"
            :start-x 100
            :start-y 250
            :press-scene-x 10
            :press-scene-y 20}
           (:converter-drag started)))
    (is (= [130 300] (model/converter-position (:diagram moved) "Converter1")))
    (is (not (contains? moved :converter-drag)))))

(deftest object-drag-dispatch-falls-through-to-converter-test
  (let [shell (-> (model/default-shell)
                  (update :diagram cmd/fixture-converter! "Converter1" 100 250))
        started (dispatch/apply-event
                 shell
                 {:event events/stock-drag-start
                  :converter-name "Converter1"
                  :scene-coordinates [10 20]})
        moved (dispatch/apply-event
               started
               {:event events/stock-drag-end
                :scene-coordinates [20 40]})]
    (is (= "Converter1" (:name (:converter-drag started))))
    (is (= [110 270] (model/converter-position (:diagram moved) "Converter1")))))

(deftest enrich-drag-event-preserves-existing-coordinates-test
  (is (= {:event events/stock-drag-start
          :scene-coordinates [10 20]
          :canvas-coordinates [1 2]
          :from-canvas true}
         (fx-input/enrich-event
          {:event events/stock-drag-start
           :scene-coordinates [10 20]
           :canvas-coordinates [1 2]
           :from-canvas true}))))

(deftest enrich-edit-apply-preserves-existing-draft-test
  (is (= {:event events/edit-stock-apply
          :draft {:name "Stock1"}}
         (fx-input/enrich-event
          {:event events/edit-stock-apply
           :draft {:name "Stock1"}}))))
