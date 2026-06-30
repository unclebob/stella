(ns stella.formula
  (:require [clojure.string :as str]
            [stella.numbers :as numbers]))

(def ^:private token-pattern
  #"[A-Za-z][A-Za-z0-9]*|-?\d+(?:\.\d+)?(?:\s*/\s*\d+)?|[+*]")

(defn- tokenize
  [formula]
  (let [trimmed (str/trim (str formula))]
    (when (empty? trimmed)
      (throw (ex-info "empty formula" {})))
    (let [tokens (vec (re-seq token-pattern trimmed))
          covered (str/join tokens)]
      (when (not= (str/replace trimmed #"\s+" "") covered)
        (throw (ex-info "invalid formula syntax" {:formula formula})))
      tokens)))

(defn- stock-token?
  [token]
  (re-matches #"[A-Za-z][A-Za-z0-9]*" token))

(defn- parse-factor
  [tokens stock-value]
  (let [token (first tokens)
        rest-tokens (rest tokens)]
    (cond
      (nil? token)
      (throw (ex-info "unexpected end of formula" {}))

      (stock-token? token)
      [(or (stock-value token)
            (throw (ex-info "unknown stock" {:stock token})))
       rest-tokens]

      :else
      [(numbers/parse-number token) rest-tokens])))

(defn- parse-term
  [tokens stock-value]
  (loop [tokens tokens
         [value tokens'] (parse-factor tokens stock-value)]
    (if (= "*" (first tokens'))
      (recur (rest tokens')
             (let [[factor tokens''] (parse-factor (rest tokens') stock-value)]
               [(* value factor) tokens'']))
      [value tokens'])))

(defn- parse-expr
  [tokens stock-value]
  (loop [tokens tokens
         [value tokens'] (parse-term tokens stock-value)]
    (cond
      (= "+" (first tokens'))
      (recur (rest tokens')
             (let [[term tokens''] (parse-term (rest tokens') stock-value)]
               [(+ value term) tokens'']))

      (seq tokens')
      (throw (ex-info "unexpected tokens" {:tokens tokens'}))

      :else
      value)))

(defn evaluate
  [formula stock-value]
  (parse-expr (tokenize formula) stock-value))

(defn valid-syntax?
  [formula]
  (try
    (tokenize formula)
    true
    (catch Exception _ false)))

(defn valid-for-stocks?
  [formula stock-names]
  (and (valid-syntax? formula)
       (let [tokens (tokenize formula)
             stocks (filter stock-token? tokens)]
         (every? stock-names stocks))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T11:22:31.689898-05:00", :module-hash "-532523093", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "282585715"} {:id "def/token-pattern", :kind "def", :line 5, :end-line 6, :hash "-1255527670"} {:id "defn-/tokenize", :kind "defn-", :line 8, :end-line 17, :hash "1599254320"} {:id "defn-/stock-token?", :kind "defn-", :line 19, :end-line 21, :hash "-1566555243"} {:id "defn-/parse-factor", :kind "defn-", :line 23, :end-line 37, :hash "1688837602"} {:id "defn-/parse-term", :kind "defn-", :line 39, :end-line 47, :hash "-253635061"} {:id "defn-/parse-expr", :kind "defn-", :line 49, :end-line 63, :hash "1787569189"} {:id "defn/evaluate", :kind "defn", :line 65, :end-line 67, :hash "1450074755"} {:id "defn/valid-syntax?", :kind "defn", :line 69, :end-line 74, :hash "-1478954409"} {:id "defn/valid-for-stocks?", :kind "defn", :line 76, :end-line 81, :hash "14519027"}]}
;; clj-mutate-manifest-end
