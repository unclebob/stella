(ns stella.cloud-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(deftest place-source-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-source-placement)
                    (model/place-source 50 150))]
    (is (model/source-exists? diagram "Source1"))
    (is (= [50 150] (model/source-position diagram "Source1")))
    (is (model/source-placement-disarmed? diagram))))

(deftest place-multiple-sources-requires-rearming-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-source-placement)
                    (model/place-source 50 150)
                    (model/arm-source-placement)
                    (model/place-source 120 180))]
    (is (model/source-exists? diagram "Source1"))
    (is (model/source-exists? diagram "Source2"))
    (is (model/source-placement-disarmed? diagram))))

(deftest place-sink-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-sink-placement)
                    (model/place-sink 400 150))]
    (is (model/sink-exists? diagram "Sink1"))
    (is (= [400 150] (model/sink-position diagram "Sink1")))
    (is (model/sink-placement-disarmed? diagram))))

(deftest move-source-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-source "Source1" 50 150)
                    (model/move-source "Source1" 80 120))]
    (is (= [80 120] (model/source-position diagram "Source1")))))

(deftest move-sink-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/move-sink "Sink1" 420 180))]
    (is (= [420 180] (model/sink-position diagram "Sink1")))))

(deftest cloud-at-canvas-point-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-source "Source1" 50 150)
                    (model/fixture-sink "Sink1" 400 150))]
    (is (= "Source1" (model/source-at-canvas-point diagram 60 160)))
    (is (= "Sink1" (model/sink-at-canvas-point diagram 410 160)))
    (is (= {:kind :source :name "Source1"}
           (model/cloud-at-canvas-point diagram 60 160)))
    (is (= {:kind :sink :name "Sink1"}
           (model/cloud-at-canvas-point diagram 410 160)))
    (is (nil? (model/cloud-at-canvas-point diagram 0 0)))))

(deftest source-to-stock-flow-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-source "Source1" 50 150)
                    (model/arm-flow-placement)
                    (model/select-flow-source :source "Source1")
                    (model/connect-flow :stock "Stock1"))]
    (is (model/flow-exists? diagram "Flow1"))
    (is (= {:kind :source :id "Source1"} (model/flow-from diagram "Flow1")))
    (is (= {:kind :stock :id "Stock1"} (model/flow-to diagram "Flow1")))))

(deftest stock-to-sink-flow-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/arm-flow-placement)
                    (model/select-flow-source :stock "Stock1")
                    (model/connect-flow :sink "Sink1"))]
    (is (model/flow-exists? diagram "Flow1"))
    (is (= {:kind :stock :id "Stock1"} (model/flow-from diagram "Flow1")))
    (is (= {:kind :sink :id "Sink1"} (model/flow-to diagram "Flow1")))))

(deftest reject-sink-as-source-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/arm-flow-placement)
                    (model/select-flow-source :sink "Sink1"))]
    (is (zero? (model/flow-count diagram)))
    (is (model/flow-placement-armed? diagram))))

(deftest reject-source-as-destination-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 200 150)
                    (model/fixture-source "Source1" 50 150)
                    (model/arm-flow-placement)
                    (model/select-flow-source :stock "Stock1")
                    (model/connect-flow :source "Source1"))]
    (is (zero? (model/flow-count diagram)))))

(deftest endpoint-geometry-test
  (let [diagram (-> (model/default-diagram)
                    (model/fixture-stock "Stock1" 100 100)
                    (model/fixture-stock "Stock2" 300 200)
                    (model/fixture-flow "Flow1" "Stock1" "Stock2")
                    (model/fixture-source "Source1" 50 150)
                    (model/fixture-sink "Sink1" 400 150)
                    (model/fixture-converter "Converter1" 25 75))]
    (is (= [100 100] (model/endpoint-position diagram {:kind :stock :id "Stock1"})))
    (is (= [50 150] (model/endpoint-position diagram {:kind :source :id "Source1"})))
    (is (= [400 150] (model/endpoint-position diagram {:kind :sink :id "Sink1"})))
    (is (= [25 75] (model/endpoint-position diagram {:kind :converter :id "Converter1"})))
    (is (= [240.0 175.0] (model/endpoint-position diagram {:kind :flow :id "Flow1"})))
    (is (= [240.0 175.0] (model/flow-midpoint diagram "Flow1")))
    (is (nil? (model/endpoint-position diagram {:kind :unknown :id "X"})))
    (is (= [180.0 125.0] (model/endpoint-anchor [100 100] :stock :right)))
    (is (= [100.0 125.0] (model/endpoint-anchor [100 100] :stock :left)))
    (is (= [130.0 175.0] (model/endpoint-anchor [50 150] :source :right)))
    (is (= [400.0 175.0] (model/endpoint-anchor [400 150] :sink :left)))))

(deftest endpoint-clickable-test
  (let [diagram (model/default-diagram)]
    (is (model/endpoint-clickable? (model/arm-flow-placement diagram) :stock))
    (is (model/endpoint-clickable? (model/arm-flow-placement diagram) :source))
    (is (not (model/endpoint-clickable? (model/arm-flow-placement diagram) :flow)))
    (is (model/endpoint-clickable? (model/arm-connector-placement diagram) :converter))
    (is (not (model/endpoint-clickable? diagram :stock)))))
