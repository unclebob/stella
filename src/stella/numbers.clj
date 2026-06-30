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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T11:22:01.376893-05:00", :module-hash "-811329669", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "72819312"} {:id "def/fraction-pattern", :kind "def", :line 4, :end-line 5, :hash "-1407507275"} {:id "defn/parse-number", :kind "defn", :line 7, :end-line 19, :hash "-38142770"} {:id "defn/parseable-number?", :kind "defn", :line 21, :end-line 26, :hash "223002966"} {:id "defn/normalize-number-string", :kind "defn", :line 28, :end-line 30, :hash "-448712648"}]}
;; clj-mutate-manifest-end
