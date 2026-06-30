(ns stella.formula-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.formula :as formula]))

(defn- stock-values [& pairs]
  (let [values (apply hash-map pairs)]
    #(get values %)))

(deftest evaluate-constant-test
  (is (= 5.0 (formula/evaluate "5" (stock-values)))))

(deftest evaluate-stock-reference-test
  (is (= 10.0 (formula/evaluate "Stock1" (stock-values "Stock1" 10.0)))))

(deftest evaluate-multiplication-test
  (is (= 10.0 (formula/evaluate "Stock1 * 0.1" (stock-values "Stock1" 100.0)))))

(deftest evaluate-rational-multiplication-test
  (is (= 0.5 (formula/evaluate "Stock1 * 1/2" (stock-values "Stock1" 1.0)))))

(deftest evaluate-addition-test
  (is (= 50.0 (formula/evaluate "Stock1 + Stock2"
                                (stock-values "Stock1" 30.0 "Stock2" 20.0)))))

(deftest evaluate-parentheses-and-division-test
  (is (= 15.0 (formula/evaluate "(Stock1 + Stock2) * 0.1"
                               (stock-values "Stock1" 100.0 "Stock2" 50.0))))
  (is (= 30.0 (formula/evaluate "(Stock1 - Stock2) / 2"
                               (stock-values "Stock1" 80.0 "Stock2" 20.0)))))

(deftest evaluate-functions-and-exponent-test
  (is (= 9.0 (formula/evaluate "Stock1 ^ 2" (stock-values "Stock1" 3.0))))
  (is (= 2.0 (formula/evaluate "sqrt(Stock1)" (stock-values "Stock1" 4.0))))
  (is (= 1.0 (formula/evaluate "cos(Stock1)" (stock-values "Stock1" 0.0)))))

(deftest evaluate-extended-functions-test
  (is (= 5.0 (formula/evaluate "abs(-5)" (stock-values))))
  (is (= 3.0 (formula/evaluate "floor(3.7)" (stock-values))))
  (is (= 4.0 (formula/evaluate "ceil(3.2)" (stock-values))))
  (is (= 4.0 (formula/evaluate "round(3.6)" (stock-values))))
  (is (= 2.0 (formula/evaluate "log(100)" (stock-values))))
  (is (= 20.0 (formula/evaluate "min(20, 30)" (stock-values))))
  (is (= 30.0 (formula/evaluate "max(20, 30)" (stock-values))))
  (is (= 10.0 (formula/evaluate "clamp(15, 0, 10)" (stock-values))))
  (is (= 5.0 (formula/evaluate "hypot(3, 4)" (stock-values))))
  (is (= 1.0 (formula/evaluate "mod(7, 3)" (stock-values))))
  (is (= 1.0 (formula/evaluate "7 % 3" (stock-values))))
  (is (= -1.0 (formula/evaluate "sign(-5)" (stock-values)))))

(deftest evaluate-constants-random-and-unary-minus-test
  (is (= Math/PI (formula/evaluate "pi" (stock-values))))
  (is (= Math/E (formula/evaluate "e" (stock-values))))
  (let [value (formula/evaluate "? * 10" (stock-values))]
    (is (<= 0.0 value))
    (is (< value 10.0)))
  (is (= -5.0 (formula/evaluate "-5" (stock-values))))
  (is (= -7.0 (formula/evaluate "-Stock1" (stock-values "Stock1" 7.0))))
  (is (= -5.0 (formula/evaluate "-(Stock1 + 2)" (stock-values "Stock1" 3.0)))))

(deftest reject-invalid-syntax-test
  (is (false? (formula/valid-syntax? "Stock1 & 0.1"))))

(deftest reject-unknown-stock-test
  (is (false? (formula/valid-for-stocks? "Missing * 2" #{"Stock1"})))
  (is (false? (formula/valid-for-stocks? "Stock1 * 0.1" #{})))
  (is (true? (formula/valid-for-stocks? "Stock1 * 0.1" #{"Stock1"})))
  (is (true? (formula/valid-for-stocks? "5" #{}))))
