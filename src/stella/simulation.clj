(ns stella.simulation
  (:require [stella.model :as model]
            [stella.numbers :as numbers]))

(def ^:private dt 0.1)

(defn- numeric-value
  [value]
  (numbers/parse-number value))

(defn- round-time
  [time]
  (/ (Math/round (* time 10)) 10.0))

(defn simulation-time
  [diagram]
  (round-time (get-in diagram [:simulation :time] 0.0)))

(defn format-time
  [time]
  (if (zero? time) "0" (model/format-display-number time)))

(defn- stock-value-number
  [diagram name]
  (let [raw (or (get-in diagram [:simulation :stock-values name])
                (model/stock-initial-value diagram name)
                "0")]
    (if (number? raw)
      (double raw)
      (numeric-value raw))))

(defn stock-value
  [diagram name]
  (model/format-display-number (stock-value-number diagram name)))

(defn- flow-transfer
  [diagram {:keys [from to rate]}]
  (let [amount (* (numeric-value (or rate "0")) dt)]
    (cond
      (zero? amount) {}

      (and (= :source (:kind from)) (= :stock (:kind to)))
      {(:id to) amount}

      (and (= :stock (:kind from)) (= :stock (:kind to)))
      {(:id from) (- amount)
       (:id to) amount}

      (and (= :stock (:kind from)) (= :sink (:kind to)))
      {(:id from) (- amount)}

      :else {})))

(defn- apply-flow-deltas
  [values deltas]
  (reduce-kv (fn [m stock-name delta]
               (update m stock-name (fnil + 0.0) delta))
             values
             deltas))

(defn- step-values
  [diagram]
  (let [values (into {}
                     (map (fn [{:keys [name]}]
                            [name (stock-value-number diagram name)]))
                     (model/stocks diagram))
        deltas (apply merge-with +
                      {}
                      (map #(flow-transfer diagram %) (model/flows diagram)))]
    (apply-flow-deltas values deltas)))

(defn step
  [diagram]
  (let [new-values (step-values diagram)
        new-time (round-time (+ (simulation-time diagram) dt))]
    (-> diagram
        (assoc :simulation {:time new-time :stock-values new-values})
        model/refresh-converter-rates)))

(defn run-steps
  [diagram n]
  (reduce (fn [d _] (step d)) diagram (range n)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T08:38:42.111754-05:00", :module-hash "1223959051", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "605290451"} {:id "def/dt", :kind "def", :line 4, :end-line 4, :hash "-1253396799"} {:id "defn-/numeric-value", :kind "defn-", :line 6, :end-line 8, :hash "-1365632356"} {:id "defn-/round-time", :kind "defn-", :line 10, :end-line 12, :hash "-1399213088"} {:id "defn/simulation-time", :kind "defn", :line 14, :end-line 16, :hash "-404964475"} {:id "defn/format-time", :kind "defn", :line 18, :end-line 20, :hash "1872542886"} {:id "defn-/stock-value-number", :kind "defn-", :line 22, :end-line 27, :hash "459068448"} {:id "defn/stock-value", :kind "defn", :line 29, :end-line 31, :hash "-1587147778"} {:id "defn-/flow-transfer", :kind "defn-", :line 33, :end-line 49, :hash "304929413"} {:id "defn-/apply-flow-deltas", :kind "defn-", :line 51, :end-line 56, :hash "-1356293942"} {:id "defn-/step-values", :kind "defn-", :line 58, :end-line 67, :hash "-143627686"} {:id "defn/step", :kind "defn", :line 69, :end-line 74, :hash "-1729194076"} {:id "defn/run-steps", :kind "defn", :line 76, :end-line 78, :hash "2051488783"}]}
;; clj-mutate-manifest-end
