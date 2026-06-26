(ns stella.cloud-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(deftest place-source-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-source-placement)
                    (model/place-source 50 150))]
    (is (model/source-exists? diagram "Source1"))
    (is (= [50 150] (model/source-position diagram "Source1")))
    (is (model/placement-disarmed? diagram))))

(deftest place-sink-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-sink-placement)
                    (model/place-sink 400 150))]
    (is (model/sink-exists? diagram "Sink1"))
    (is (= [400 150] (model/sink-position diagram "Sink1")))))

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
                    (model/fixture-source "Source1" 50 150))]
    (is (= [100 100] (model/endpoint-position diagram {:kind :stock :id "Stock1"})))
    (is (= [50 150] (model/endpoint-position diagram {:kind :source :id "Source1"})))
    (is (= [180.0 125.0] (model/endpoint-anchor [100 100] :stock :right)))
    (is (= [100.0 125.0] (model/endpoint-anchor [100 100] :stock :left)))
    (is (= [130.0 175.0] (model/endpoint-anchor [50 150] :source :right)))
    (is (= [400.0 175.0] (model/endpoint-anchor [400 150] :sink :left)))))