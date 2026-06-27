(ns stella.edit-flow-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-flow []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 100 100)
      (cmd/fixture-stock! "Stock2" 300 200)
      (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")))

(deftest rename-flow-test
  (let [diagram (cmd/set-flow-name! (diagram-with-flow) "Flow1" "Drain")]
    (is (not (model/flow-exists? diagram "Flow1")))
    (is (model/flow-exists? diagram "Drain"))
    (is (= "0" (model/flow-rate diagram "Drain")))))

(deftest reject-duplicate-flow-name-test
  (let [diagram (-> (diagram-with-flow)
                    (cmd/fixture-flow! "Flow2" "Stock2" "Stock1")
                    (cmd/set-flow-name! "Flow1" "Flow2"))]
    (is (model/flow-exists? diagram "Flow1"))
    (is (model/flow-exists? diagram "Flow2"))))

(deftest set-flow-rate-test
  (let [diagram (cmd/set-flow-rate! (diagram-with-flow) "Flow1" "5")]
    (is (= "5" (model/flow-rate diagram "Flow1")))))

(deftest reject-non-numeric-flow-rate-test
  (let [diagram (cmd/set-flow-rate! (diagram-with-flow) "Flow1" "abc")]
    (is (= "0" (model/flow-rate diagram "Flow1")))))

(deftest rename-flow-updates-connector-refs-test
  (let [diagram (-> (diagram-with-flow)
                    (cmd/fixture-converter! "Converter1" 100 250)
                    (cmd/arm-connector-placement!)
                    (cmd/select-connector-origin! :converter "Converter1")
                    (cmd/connect-connector! :flow "Flow1")
                    (cmd/set-flow-name! "Flow1" "Drain"))]
    (is (model/flow-exists? diagram "Drain"))
    (is (= {:kind :flow :id "Drain"} (model/connector-to diagram "Connector1")))))