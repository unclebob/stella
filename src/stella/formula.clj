(ns stella.formula
  (:require [clojure.string :as str]
            [stella.numbers :as numbers]))

(declare ^:private parse-expr)

(def ^:private function-names
  #{"sqrt" "sin" "cos" "tan" "ln" "exp"})

(def ^:private token-pattern
  #"sqrt|sin|cos|tan|ln|exp|[A-Za-z][A-Za-z0-9]*|-?\d+(?:\.\d+)?(?:\s*/\s*\d+)?|[-+^*/()]")

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

(defn- function-token?
  [token]
  (contains? function-names token))

(defn- stock-token?
  [token]
  (and (re-matches #"[A-Za-z][A-Za-z0-9]*" token)
       (not (function-token? token))))

(def ^:private function-implementations
  {"sqrt" Math/sqrt
   "sin" Math/sin
   "cos" Math/cos
   "tan" Math/tan
   "ln" Math/log
   "exp" Math/exp})

(defn- apply-function
  [name value]
  ((function-implementations name) value))

(defn- parse-parenthesized
  [tokens stock-value]
  (when-not (= "(" (first tokens))
    (throw (ex-info "expected opening parenthesis" {})))
  (let [[value remaining] (parse-expr (rest tokens) stock-value)]
    (when-not (= ")" (first remaining))
      (throw (ex-info "missing closing parenthesis" {})))
    [value (rest remaining)]))

(defn- parse-factor
  [tokens stock-value]
  (let [token (first tokens)
        rest-tokens (rest tokens)]
    (cond
      (nil? token)
      (throw (ex-info "unexpected end of formula" {}))

      (= "(" token)
      (parse-parenthesized tokens stock-value)

      (function-token? token)
      (let [[value remaining] (parse-parenthesized rest-tokens stock-value)]
        [(apply-function token value) remaining])

      (stock-token? token)
      [(or (stock-value token)
           (throw (ex-info "unknown stock" {:stock token})))
       rest-tokens]

      :else
      [(numbers/parse-number token) rest-tokens])))

(defn- parse-power
  [tokens stock-value]
  (let [[base tokens'] (parse-factor tokens stock-value)]
    (if (= "^" (first tokens'))
      (let [[exponent tokens''] (parse-factor (rest tokens') stock-value)]
        [(Math/pow base exponent) tokens''])
      [base tokens'])))

(defn- parse-term
  [tokens stock-value]
  (loop [tokens tokens
         [value tokens'] (parse-power tokens stock-value)]
    (case (first tokens')
      "*" (let [[factor tokens''] (parse-power (rest tokens') stock-value)]
            (recur tokens'' [(* value factor) tokens'']))
      "/" (let [[factor tokens''] (parse-power (rest tokens') stock-value)]
            (when (zero? factor)
              (throw (ex-info "division by zero" {})))
            (recur tokens'' [(/ value factor) tokens'']))
      [value tokens'])))

(defn- parse-expr
  [tokens stock-value]
  (loop [tokens tokens
         [value tokens'] (parse-term tokens stock-value)]
    (cond
      (= ")" (first tokens'))
      [value tokens']

      (= "+" (first tokens'))
      (let [[term tokens''] (parse-term (rest tokens') stock-value)]
        (recur tokens'' [(+ value term) tokens'']))

      (= "-" (first tokens'))
      (let [[term tokens''] (parse-term (rest tokens') stock-value)]
        (recur tokens'' [(- value term) tokens'']))

      (seq tokens')
      (throw (ex-info "unexpected tokens" {:tokens tokens'}))

      :else
      [value tokens'])))

(defn evaluate
  [formula stock-value]
  (let [[value _] (parse-expr (tokenize formula) stock-value)]
    value))

(defn- formula-stocks
  [formula]
  (filter stock-token? (tokenize formula)))

(defn valid-syntax?
  [formula]
  (try
    (tokenize formula)
    true
    (catch Exception _ false)))

(defn valid-for-stocks?
  [formula bound-stocks]
  (and (valid-syntax? formula)
       (let [stocks (set (formula-stocks formula))]
         (and (every? bound-stocks stocks)
              (try
                (evaluate formula (constantly 0.0))
                true
                (catch Exception _ false))))))
