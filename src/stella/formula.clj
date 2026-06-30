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