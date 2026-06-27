(ns stella.drag-converter-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-converter []
  (cmd/fixture-converter! (cmd/default-diagram! nil) "Converter1" 100 250))

(deftest move-converter-test
  (let [diagram (cmd/move-converter! (diagram-with-converter) "Converter1" 220 300)]
    (is (= [220 300] (model/converter-position diagram "Converter1")))))

(deftest move-converter-preserves-count-test
  (let [diagram (cmd/move-converter! (diagram-with-converter) "Converter1" 150 200)]
    (is (= 1 (model/converter-count diagram)))
    (is (model/converter-exists? diagram "Converter1"))))

(deftest move-converter-preserves-connector-endpoints-test
  (let [diagram (-> (diagram-with-converter)
                    (cmd/fixture-stock! "Stock1" 100 100)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/fixture-connector! "Connector1" "Converter1" "Flow1")
                    (cmd/move-converter! "Converter1" 50 180))]
    (is (= [50 180] (model/converter-position diagram "Converter1")))
    (is (= {:kind :converter :id "Converter1"} (model/connector-from diagram "Connector1")))
    (is (= {:kind :flow :id "Flow1"} (model/connector-to diagram "Connector1")))))

(deftest move-one-converter-leaves-other-untouched-test
  (let [diagram (-> (diagram-with-converter)
                    (cmd/fixture-converter! "Converter2" 300 250)
                    (cmd/move-converter! "Converter1" 120 90))]
    (is (= [120 90] (model/converter-position diagram "Converter1")))
    (is (= [300 250] (model/converter-position diagram "Converter2")))))

(deftest move-missing-converter-no-op-test
  (let [before (diagram-with-converter)
        after (cmd/move-converter! before "Missing" 1 2)]
    (is (= before after))))

(deftest converter-at-canvas-point-test
  (let [diagram (diagram-with-converter)]
    (is (= "Converter1" (model/converter-at-canvas-point diagram 120 270)))
    (is (nil? (model/converter-at-canvas-point diagram 0 0)))))