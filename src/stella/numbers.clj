(ns stella.numbers
  (:require [clojure.string :as str]))

(def ^:private fraction-pattern
  #"^\s*(-?\d+)\s*/\s*(\d+)\s*$")

(defn parse-number
  "Parse a decimal or simple rational literal such as 1/2."
  [value]
  (let [s (str/trim (str value))]
    (when (empty? s)
      (throw (IllegalArgumentException. "empty number")))
    (if-let [[_ numerator denominator] (re-matches fraction-pattern s)]
      (let [n (Long/parseLong numerator)
            d (Long/parseLong denominator)]
        (when (zero? d)
          (throw (IllegalArgumentException. "zero denominator")))
        (/ (double n) d))
      (Double/parseDouble s))))

(defn parseable-number?
  [value]
  (and (seq (str value))
       (try (parse-number value)
            true
            (catch Exception _ false))))

(defn normalize-number-string
  [value]
  (str/trim (str value)))