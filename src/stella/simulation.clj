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
;; {:version 1, :tested-at "2026-06-30T11:22:16.658899-05:00", :module-hash "1857357200", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "333467263"} {:id "def/dt", :kind "def", :line 5, :end-line 5, :hash "-1253396799"} {:id "defn-/numeric-value", :kind "defn-", :line 7, :end-line 9, :hash "-1861362132"} {:id "defn/simulation-time", :kind "defn", :line 11, :end-line 13, :hash "2136292554"} {:id "defn/format-time", :kind "defn", :line 15, :end-line 17, :hash "1872542886"} {:id "defn/stock-numeric-value", :kind "defn", :line 19, :end-line 26, :hash "-1417938307"} {:id "defn/stock-value", :kind "defn", :line 28, :end-line 30, :hash "-705899973"} {:id "defn-/flow-transfer", :kind "defn-", :line 32, :end-line 48, :hash "304929413"} {:id "defn-/apply-flow-deltas", :kind "defn-", :line 50, :end-line 55, :hash "-1356293942"} {:id "defn-/step-values", :kind "defn-", :line 57, :end-line 66, :hash "1005299853"} {:id "defn/step", :kind "defn", :line 68, :end-line 74, :hash "-313211734"} {:id "defn/run-steps", :kind "defn", :line 76, :end-line 78, :hash "2051488783"}]}
;; clj-mutate-manifest-end
