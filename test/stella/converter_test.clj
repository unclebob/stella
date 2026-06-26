(ns stella.converter-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(deftest place-converter-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-converter-placement)
                    (model/place-converter 100 250))]
    (is (model/converter-exists? diagram "Converter1"))
    (is (= [100 250] (model/converter-position diagram "Converter1")))
    (is (= "0" (model/converter-value diagram "Converter1")))
    (is (model/placement-disarmed? diagram))))