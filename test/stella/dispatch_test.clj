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
    (is (= shell (dispatch/apply-action shell :window-close)))
    (is (:about-visible (dispatch/apply-action shell :show-about)))
    (is (= shell (dispatch/apply-action shell :stella.test/unknown)))))

(deftest process-event-test
  (is (= {:action :quit :effect :platform-exit}
         (dispatch/process-event {:event events/quit})))
  (is (= {:action :window-close :effect :platform-exit}
         (dispatch/process-event {:event events/window-close})))
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
  (is (true? (dispatch/diagram-event? events/canvas-move)))
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

(deftest apply-event-idle-canvas-click-selects-flow-by-coordinate-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 80 110)
                                        (cmd/fixture-stock! "Stock2" 429 141)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))))
        selected (dispatch/apply-event shell {:event events/canvas-click
                                              :coordinates [348 160]})]
    (is (model/selected? (:diagram selected) :flow "Flow1"))))

(deftest apply-event-canvas-move-updates-preview-test
  (let [shell (dispatch/apply-event (model/default-shell)
                                    {:event events/canvas-move
                                     :coordinates [120 140]})
        cleared (dispatch/apply-event shell {:event events/arm-stock})]
    (is (= {:x 120 :y 140} (:canvas-preview shell)))
    (is (nil? (:canvas-preview cleared)))))

(deftest apply-event-marquee-drag-updates-live-rectangle-test
  (let [started (dispatch/apply-event (model/default-shell)
                                      {:event events/marquee-drag-start
                                       :canvas-coordinates [10 20]})
        dragged (dispatch/apply-event started
                                      {:event events/marquee-drag
                                       :canvas-coordinates [80 90]})]
    (is (= {:start-x 10 :start-y 20}
           (:marquee-drag started)))
    (is (= {:start-x 10 :start-y 20 :current-x 80 :current-y 90}
           (:marquee-drag dragged)))))

(deftest apply-event-marquee-does-not-start-on-flow-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 226 172)
                                        (cmd/fixture-stock! "Stock2" 475 184)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))))
        pressed (dispatch/apply-event shell {:event events/marquee-drag-start
                                             :canvas-coordinates [414 208]})
        selected (dispatch/apply-event pressed {:event events/selection-click
                                                :object-kind :flow
                                                :object-name "Flow1"
                                                :canvas-coordinates [414 208]})]
    (is (nil? (:marquee-drag pressed)))
    (is (model/selected? (:diagram selected) :flow "Flow1"))))

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

(deftest apply-event-select-connector-does-not-select-flow-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 200 150)
                                        (cmd/fixture-stock! "Stock2" 350 150)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                                        (cmd/fixture-converter! "Converter1" 100 250)
                                        (cmd/fixture-connector! "Connector1" "Converter1" "Flow1"))))
        selected (dispatch/apply-event shell {:event events/selection-click
                                              :object-kind :connector
                                              :object-name "Connector1"})
        diagram (:diagram selected)]
    (is (model/selected? diagram :connector "Connector1"))
    (is (not (model/selected? diagram :flow "Flow1")))))

(deftest apply-event-selection-click-uses-coordinate-priority-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                                        (cmd/fixture-converter! "Converter1" 230 150))))
        connector-shell (-> (model/default-shell)
                            (update :diagram #(-> %
                                                  (cmd/fixture-stock! "Stock1" 100 100)
                                                  (cmd/fixture-stock! "Stock2" 300 200)
                                                  (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                                                  (cmd/fixture-converter! "Converter1" 100 250)
                                                  (cmd/fixture-connector! "Connector1" "Converter1" "Flow1"))))
        stock-over-flow (dispatch/apply-event shell {:event events/selection-click
                                                     :object-kind :flow
                                                     :object-name "Flow1"
                                                     :canvas-coordinates [305 225]})
        stock-click-circle (dispatch/apply-event shell {:event events/selection-click
                                                        :canvas-coordinates [295 225]})
        flow-over-converter (dispatch/apply-event shell {:event events/selection-click
                                                         :object-kind :converter
                                                         :object-name "Converter1"
                                                         :canvas-coordinates [240 175]})
        flow-over-connector (dispatch/apply-event shell {:event events/selection-click
                                                         :object-kind :connector
                                                         :object-name "Connector1"
                                                         :canvas-coordinates [240 175]})
        outside-flow-pipe (dispatch/apply-event shell {:event events/selection-click
                                                       :object-kind :flow
                                                       :object-name "Flow1"
                                                       :canvas-coordinates [210 176]})
        connector-alone (dispatch/apply-event connector-shell {:event events/selection-click
                                                               :object-kind :connector
                                                               :object-name "Connector1"
                                                               :canvas-coordinates [190 225]})]
    (is (model/selected? (:diagram stock-over-flow) :stock "Stock2"))
    (is (not (model/selected? (:diagram stock-over-flow) :flow "Flow1")))
    (is (model/selected? (:diagram stock-click-circle) :stock "Stock2"))
    (is (model/selected? (:diagram flow-over-converter) :flow "Flow1"))
    (is (not (model/selected? (:diagram flow-over-converter) :converter "Converter1")))
    (is (model/selected? (:diagram flow-over-connector) :flow "Flow1"))
    (is (not (model/selected? (:diagram flow-over-connector) :connector "Connector1")))
    (is (not (model/selected? (:diagram outside-flow-pipe) :flow "Flow1")))
    (is (model/nothing-selected? (:diagram outside-flow-pipe)))
    (is (model/selected? (:diagram connector-alone) :connector "Connector1"))
    (is (not (model/selected? (:diagram connector-alone) :flow "Flow1")))))

(deftest apply-event-cloud-drag-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(model/fixture-source % "Source1" 50 150)))
        dragging (dispatch/apply-event shell {:event events/cloud-drag-start
                                              :cloud-kind :source
                                              :cloud-name "Source1"
                                              :scene-coordinates [60 160]})
        live-dragged (dispatch/apply-event dragging {:event events/cloud-drag
                                                     :scene-coordinates [80 190]})
        dragged (dispatch/apply-event dragging {:event events/cloud-drag-end
                                                :scene-coordinates [100 210]})]
    (is (:cloud-drag dragging))
    (is (:cloud-drag live-dragged))
    (is (= [70 180] (model/source-position (:diagram live-dragged) "Source1")))
    (is (= [90 200] (model/source-position (:diagram dragged) "Source1")))
    (is (nil? (:cloud-drag dragged)))))

(deftest apply-event-stock-drag-rubber-bands-links-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 100 100)
                                        (cmd/fixture-stock! "Stock2" 300 200)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2"))))
        dragging (dispatch/apply-event shell {:event events/stock-drag-start
                                              :stock-name "Stock1"
                                              :scene-coordinates [140 125]})
        live-dragged (dispatch/apply-event dragging {:event events/stock-drag
                                                     :scene-coordinates [170 165]})
        released (dispatch/apply-event live-dragged {:event events/stock-drag-end
                                                     :scene-coordinates [180 175]})]
    (is (:stock-drag live-dragged))
    (is (= [130 140] (model/stock-position (:diagram live-dragged) "Stock1")))
    (is (= [255.0 195.0] (model/flow-midpoint (:diagram live-dragged) "Flow1")))
    (is (= [140 150] (model/stock-position (:diagram released) "Stock1")))
    (is (nil? (:stock-drag released)))))

(deftest apply-event-converter-drag-rubber-bands-connectors-test
  (let [shell (-> (model/default-shell)
                  (update :diagram #(-> %
                                        (cmd/fixture-stock! "Stock1" 200 150)
                                        (cmd/fixture-stock! "Stock2" 350 150)
                                        (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                                        (cmd/fixture-converter! "Converter1" 100 250)
                                        (cmd/fixture-connector! "Connector1" "Converter1" "Flow1"))))
        dragging (dispatch/apply-event shell {:event events/converter-drag-start
                                              :converter-name "Converter1"
                                              :scene-coordinates [125 275]})
        live-dragged (dispatch/apply-event dragging {:event events/converter-drag
                                                     :scene-coordinates [165 315]})
        released (dispatch/apply-event live-dragged {:event events/converter-drag-end
                                                     :scene-coordinates [175 325]})]
    (is (:converter-drag live-dragged))
    (is (= [140 290] (model/converter-position (:diagram live-dragged) "Converter1")))
    (is (= [150 300] (model/converter-position (:diagram released) "Converter1")))
    (is (nil? (:converter-drag released)))))
