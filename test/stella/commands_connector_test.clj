(ns stella.commands-connector-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-fixtures []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 200 150)
      (cmd/fixture-stock! "Stock2" 350 150)
      (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
      (cmd/fixture-converter! "Converter1" 100 250)))

(deftest select-endpoint-on-shell-connector-test
  (let [shell (cmd/arm-connector-placement-on-shell!
                (assoc (cmd/default-shell! nil) :diagram (diagram-with-fixtures)))
        drafted (cmd/select-endpoint-on-shell! shell :converter "Converter1")
        connected (cmd/select-endpoint-on-shell! drafted :flow "Flow1")]
    (is (= {:kind :converter :id "Converter1"} (:from (:connector-draft (:diagram drafted)))))
    (is (model/connector-exists? (:diagram connected) "Connector1"))))

(deftest select-endpoint-on-shell-idle-test
  (let [shell (cmd/default-shell! nil)]
    (is (= shell (cmd/select-endpoint-on-shell! shell :stock "Stock1")))))