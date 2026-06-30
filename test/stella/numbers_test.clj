(ns stella.numbers-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.numbers :as numbers]))

(deftest parse-number-test
  (testing "decimals"
    (is (= 7.5 (numbers/parse-number "7.5")))
    (is (= 0.1 (numbers/parse-number "0.1"))))
  (testing "rationals"
    (is (= 0.5 (numbers/parse-number "1/2")))
    (is (= 0.75 (numbers/parse-number "3/4")))
    (is (= -0.25 (numbers/parse-number "-1/4")))
    (is (= 0.5 (numbers/parse-number " 1 / 2 "))))
  (testing "invalid"
    (is (thrown? IllegalArgumentException (numbers/parse-number "")))
    (is (thrown? IllegalArgumentException (numbers/parse-number "1/0")))
    (is (thrown? IllegalArgumentException (numbers/parse-number "abc")))
    (is (thrown? IllegalArgumentException (numbers/parse-number "1/2/3")))))

(deftest parseable-number-test
  (is (numbers/parseable-number? "1/2"))
  (is (numbers/parseable-number? "10"))
  (is (not (numbers/parseable-number? "abc")))
  (is (not (numbers/parseable-number? "1/0"))))