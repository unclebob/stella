(ns stella.select-objects-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- base-diagram []
  (-> (cmd/default-diagram! nil)
      (cmd/fixture-stock! "Stock1" 100 100)
      (cmd/fixture-stock! "Stock2" 300 200)
      (cmd/fixture-converter! "Converter1" 100 250)))

(deftest click-select-test
  (let [diagram (cmd/click-select! (base-diagram) :stock "Stock1")]
    (is (model/selected? diagram :stock "Stock1"))
    (is (= 1 (model/selection-count diagram)))))

(deftest click-select-clouds-test
  (let [diagram (-> (base-diagram)
                    (cmd/fixture-source! "Source1" 50 150)
                    (cmd/fixture-sink! "Sink1" 400 150))]
    (is (model/selected? (cmd/click-select! diagram :source "Source1")
                         :source
                         "Source1"))
    (is (model/selected? (cmd/click-select! diagram :sink "Sink1")
                         :sink
                         "Sink1"))))

(deftest click-select-keeps-selection-test
  (let [diagram (-> (base-diagram)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/click-select! :stock "Stock1"))]
    (is (model/selected? diagram :stock "Stock1"))
    (is (= 1 (model/selection-count diagram)))))

(deftest click-select-replaces-selection-test
  (let [diagram (-> (base-diagram)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/click-select! :stock "Stock2"))]
    (is (not (model/selected? diagram :stock "Stock1")))
    (is (model/selected? diagram :stock "Stock2"))))

(deftest shift-click-select-adds-test
  (let [diagram (-> (base-diagram)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/shift-click-select! :stock "Stock2"))]
    (is (model/selected? diagram :stock "Stock1"))
    (is (model/selected? diagram :stock "Stock2"))
    (is (= 2 (model/selection-count diagram)))))

(deftest shift-click-select-removes-test
  (let [diagram (-> (base-diagram)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/shift-click-select! :stock "Stock2")
                    (cmd/shift-click-select! :stock "Stock1"))]
    (is (not (model/selected? diagram :stock "Stock1")))
    (is (model/selected? diagram :stock "Stock2"))))

(deftest marquee-select-test
  (let [diagram (cmd/marquee-select! (base-diagram) 50 50 200 200)]
    (is (model/selected? diagram :stock "Stock1"))
    (is (not (model/selected? diagram :stock "Stock2")))))

(deftest clear-selection-test
  (let [diagram (-> (base-diagram)
                    (cmd/click-select! :stock "Stock1")
                    (cmd/clear-selection!))]
    (is (model/nothing-selected? diagram))))

(deftest selection-disabled-when-placement-armed-test
  (let [diagram (-> (base-diagram)
                    (cmd/arm-flow-placement!)
                    (cmd/click-select! :stock "Stock1"))]
    (is (model/nothing-selected? diagram))))
