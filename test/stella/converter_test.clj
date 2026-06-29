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
    (is (= :converter (:placement-mode diagram)))))

(deftest place-multiple-converters-without-rearming-test
  (let [diagram (-> (model/default-diagram)
                    (model/arm-converter-placement)
                    (model/place-converter 100 250)
                    (model/place-converter 200 300))]
    (is (model/converter-exists? diagram "Converter1"))
    (is (model/converter-exists? diagram "Converter2"))
    (is (= :converter (:placement-mode diagram)))))