(ns stella.commands-flow-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-stocks []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 100 100)
      (cmd/fixture-stock! "Stock2" 300 200)))

(deftest flow-commands-test
  (let [diagram (-> (diagram-with-stocks)
                    (cmd/arm-flow-placement!)
                    (cmd/select-flow-source! "Stock1")
                    (cmd/connect-flow! "Stock2"))]
    (is (model/flow-exists? diagram "Flow1"))))

(deftest fixture-flow-command-test
  (let [diagram (cmd/fixture-flow! (diagram-with-stocks) "Flow1" "Stock1" "Stock2")]
    (is (model/flow-exists? diagram "Flow1"))
    (is (= ["Stock1" "Stock2"] (model/flow-endpoints diagram "Flow1")))))

(deftest reverse-flow-requires-rearm-test
  (let [diagram (-> (diagram-with-stocks)
                    (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
                    (cmd/arm-flow-placement!)
                    (cmd/select-flow-source! "Stock2")
                    (cmd/connect-flow! "Stock1"))]
    (is (model/flow-exists? diagram "Flow2"))
    (is (= ["Stock2" "Stock1"] (model/flow-endpoints diagram "Flow2")))))