(ns stella.commands-diagram-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(deftest arm-and-place-commands-test
  (let [diagram (-> (cmd/default-diagram! nil)
                     (cmd/arm-stock-placement!)
                     (cmd/place-stock! 100 100))]
    (is (model/stock-exists? diagram "Stock1"))))

(deftest place-without-arm-is-noop-test
  (let [diagram (cmd/place-stock! (cmd/default-diagram! nil) 100 100)]
    (is (zero? (model/stock-count diagram)))))

(deftest second-stock-requires-rearm-test
  (let [diagram (-> (cmd/default-diagram! nil)
                    (cmd/arm-stock-placement!)
                    (cmd/place-stock! 100 100)
                    (cmd/arm-stock-placement!)
                    (cmd/place-stock! 300 200))]
    (is (model/stock-exists? diagram "Stock1"))
    (is (model/stock-exists? diagram "Stock2"))
    (is (= [300 200] (model/stock-position diagram "Stock2")))))