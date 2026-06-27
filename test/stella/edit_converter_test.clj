(ns stella.edit-converter-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-connector []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 100 100)
      (cmd/fixture-stock! "Stock2" 300 200)
      (cmd/fixture-flow! "Flow1" "Stock1" "Stock2")
      (cmd/fixture-converter! "Converter1" 100 250)
      (cmd/fixture-connector! "Connector1" "Converter1" "Flow1")))

(deftest rename-converter-test
  (let [diagram (cmd/set-converter-name! (diagram-with-connector) "Converter1" "Growth")]
    (is (not (model/converter-exists? diagram "Converter1")))
    (is (model/converter-exists? diagram "Growth"))
    (is (= {:kind :converter :id "Growth"} (model/connector-from diagram "Connector1")))))

(deftest reject-duplicate-converter-name-test
  (let [diagram (-> (diagram-with-connector)
                    (cmd/fixture-converter! "Converter2" 300 250)
                    (cmd/set-converter-name! "Converter1" "Converter2"))]
    (is (model/converter-exists? diagram "Converter1"))
    (is (model/converter-exists? diagram "Converter2"))))

(deftest set-converter-formula-test
  (let [diagram (cmd/set-converter-formula! (diagram-with-connector) "Converter1" "Stock1 * 0.1")]
    (is (= "Stock1 * 0.1" (model/connector-formula diagram "Connector1")))))

(deftest reject-formula-without-connector-test
  (let [diagram (-> (diagram-with-connector)
                    (cmd/fixture-converter! "Converter2" 300 250)
                    (cmd/set-converter-formula! "Converter2" "Stock1 * 0.2"))]
    (is (not (seq (model/connector-formula diagram "Connector1"))))))

(deftest new-connector-has-empty-formula-test
  (is (= "" (model/connector-formula (diagram-with-connector) "Connector1"))))