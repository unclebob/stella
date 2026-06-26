(ns stella.ui.palette-test
  (:require [clojure.test :refer [deftest is]]
            [stella.events :as events]
            [stella.ui.palette :as palette]))

(deftest palette-tools-test
  (let [desc (palette/palette-desc)
        [stock-btn flow-btn source-btn sink-btn converter-btn connector-btn] (:children desc)]
    (is (= :pane (:fx/type desc)))
    (is (= "Stock" (:text stock-btn)))
    (is (= {:event events/arm-stock} (:on-action stock-btn)))
    (is (= "Flow" (:text flow-btn)))
    (is (= {:event events/arm-flow} (:on-action flow-btn)))
    (is (= "Source" (:text source-btn)))
    (is (= {:event events/arm-source} (:on-action source-btn)))
    (is (= "Sink" (:text sink-btn)))
    (is (= {:event events/arm-sink} (:on-action sink-btn)))
    (is (= "Converter" (:text converter-btn)))
    (is (= {:event events/arm-converter} (:on-action converter-btn)))
    (is (= "Connector" (:text connector-btn)))
    (is (= {:event events/arm-connector} (:on-action connector-btn)))))