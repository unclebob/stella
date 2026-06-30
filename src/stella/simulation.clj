(ns stella.simulation
  (:require [stella.model :as model]))

(def ^:private dt 0.1)

(defn- numeric-value
  [value]
  (Double/parseDouble (str value)))

(defn- format-number
  [n]
  (let [rounded (/ (Math/round (* n 10)) 10.0)]
    (if (= rounded (double (long rounded)))
      (str (long rounded))
      (str rounded))))

(defn- round-time
  [time]
  (/ (Math/round (* time 10)) 10.0))

(defn simulation-time
  [diagram]
  (round-time (get-in diagram [:simulation :time] 0.0)))

(defn format-time
  [time]
  (if (zero? time) "0" (format-number time)))

(defn- stock-value-number
  [diagram name]
  (numeric-value
   (or (get-in diagram [:simulation :stock-values name])
       (model/stock-initial-value diagram name)
       "0")))

(defn stock-value
  [diagram name]
  (format-number (stock-value-number diagram name)))

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
        formatted (into {} (map (fn [[k v]] [k (format-number v)]) new-values))
        new-time (round-time (+ (simulation-time diagram) dt))]
    (assoc diagram :simulation {:time new-time :stock-values formatted})))

(defn run-steps
  [diagram n]
  (reduce (fn [d _] (step d)) diagram (range n)))