(ns stella.formula
  (:require [clojure.string :as str]
            [stella.numbers :as numbers]))

(declare ^:private parse-expr)

(def ^:private function-arities
  {"sqrt" 1
   "sin" 1
   "cos" 1
   "tan" 1
   "ln" 1
   "exp" 1
   "abs" 1
   "floor" 1
   "ceil" 1
   "round" 1
   "log" 1
   "asin" 1
   "acos" 1
   "atan" 1
   "sign" 1
   "min" 2
   "max" 2
   "mod" 2
   "hypot" 2
   "atan2" 2
   "clamp" 3})

(def ^:private token-pattern
  #"sqrt|sin|cos|tan|ln|exp|abs|floor|ceil|round|log|asin|acos|atan2|atan|sign|min|max|mod|hypot|clamp|pi|e|\?|[A-Za-z][A-Za-z0-9]*|\d+(?:\.\d+)?(?:\s*/\s*\d+)?|[-+^*/%(),]")

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
  (contains? function-arities token))

(defn- stock-token?
  [token]
  (and (re-matches #"[A-Za-z][A-Za-z0-9]*" token)
       (not (function-token? token))
       (not (#{"pi" "e"} token))))

(def ^:private function-implementations
  {"sqrt" Math/sqrt
   "sin" Math/sin
   "cos" Math/cos
   "tan" Math/tan
   "ln" Math/log
   "exp" Math/exp
   "abs" #(Math/abs (double %))
   "floor" Math/floor
   "ceil" Math/ceil
   "round" #(double (Math/round (double %)))
   "log" #(Math/log10 (double %))
   "asin" Math/asin
   "acos" Math/acos
   "atan" Math/atan
   "sign" #(double (cond (neg? %) -1 (pos? %) 1 :else 0))
   "min" #(Math/min (double %1) (double %2))
   "max" #(Math/max (double %1) (double %2))
   "mod" #(mod %1 %2)
   "hypot" #(Math/hypot (double %1) (double %2))
   "atan2" #(Math/atan2 (double %1) (double %2))
   "clamp" #(min (max %1 %2) %3)})

(defn- apply-function
  [name values]
  (apply (function-implementations name) values))

(defn- parse-parenthesized
  [tokens stock-value]
  (when-not (= "(" (first tokens))
    (throw (ex-info "expected opening parenthesis" {})))
  (let [[value remaining] (parse-expr (rest tokens) stock-value)]
    (when-not (= ")" (first remaining))
      (throw (ex-info "missing closing parenthesis" {})))
    [value (rest remaining)]))

(defn- parse-argument-list
  [tokens stock-value]
  (when-not (= "(" (first tokens))
    (throw (ex-info "expected opening parenthesis" {})))
  (loop [tokens (rest tokens)
         values []]
    (let [[value remaining] (parse-expr tokens stock-value)]
      (case (first remaining)
        "," (recur (rest remaining) (conj values value))
        ")" [(conj values value) (rest remaining)]
        (throw (ex-info "expected comma or closing parenthesis" {}))))))

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
      (let [[values remaining] (parse-argument-list rest-tokens stock-value)
            arity (function-arities token)]
        (when-not (= arity (count values))
          (throw (ex-info "wrong function arity" {:function token
                                                  :expected arity
                                                  :actual (count values)})))
        [(apply-function token values) remaining])

      (= "-" token)
      (let [[value remaining] (parse-factor rest-tokens stock-value)]
        [(- value) remaining])

      (= "pi" token)
      [Math/PI rest-tokens]

      (= "e" token)
      [Math/E rest-tokens]

      (= "?" token)
      [(* 0.9 (rand)) rest-tokens]

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
      "%" (let [[factor tokens''] (parse-power (rest tokens') stock-value)]
            (when (zero? factor)
              (throw (ex-info "modulo by zero" {})))
            (recur tokens'' [(mod value factor) tokens'']))
      [value tokens'])))

(defn- parse-expr
  [tokens stock-value]
  (loop [tokens tokens
         [value tokens'] (parse-term tokens stock-value)]
    (cond
      (#{")" ","} (first tokens'))
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
                (evaluate formula (constantly 1.0))
                true
                (catch Exception _ false))))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T11:27:18.614848-05:00", :module-hash "358348910", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "282585715"} {:id "form/1/declare", :kind "declare", :line 5, :end-line 5, :hash "1128659220"} {:id "def/function-names", :kind "def", :line 7, :end-line 8, :hash "941362385"} {:id "def/token-pattern", :kind "def", :line 10, :end-line 11, :hash "-425025864"} {:id "defn-/tokenize", :kind "defn-", :line 13, :end-line 22, :hash "1599254320"} {:id "defn-/function-token?", :kind "defn-", :line 24, :end-line 26, :hash "-1829932357"} {:id "defn-/stock-token?", :kind "defn-", :line 28, :end-line 31, :hash "-1611176881"} {:id "def/function-implementations", :kind "def", :line 33, :end-line 39, :hash "770257885"} {:id "defn-/apply-function", :kind "defn-", :line 41, :end-line 43, :hash "-472578322"} {:id "defn-/parse-parenthesized", :kind "defn-", :line 45, :end-line 52, :hash "1811886979"} {:id "defn-/parse-factor", :kind "defn-", :line 54, :end-line 75, :hash "276277264"} {:id "defn-/parse-power", :kind "defn-", :line 77, :end-line 83, :hash "1900492906"} {:id "defn-/parse-term", :kind "defn-", :line 85, :end-line 96, :hash "1835556306"} {:id "defn-/parse-expr", :kind "defn-", :line 98, :end-line 118, :hash "-1706946225"} {:id "defn/evaluate", :kind "defn", :line 120, :end-line 123, :hash "-1778425733"} {:id "defn-/formula-stocks", :kind "defn-", :line 125, :end-line 127, :hash "635228488"} {:id "defn/valid-syntax?", :kind "defn", :line 129, :end-line 134, :hash "-1478954409"} {:id "defn/valid-for-stocks?", :kind "defn", :line 136, :end-line 144, :hash "-1112580691"}]}
;; clj-mutate-manifest-end
