(ns stella.commands
  (:require [stella.model :as model]))

(defn default-shell!
  [_]
  (model/default-shell))

(defn default-diagram!
  [_]
  (model/default-diagram))

(defn show-about!
  [shell]
  (-> shell
      (assoc :about-visible true)
      (assoc :about-text "Stella\nA system dynamics diagram editor.")))

(defn quit!
  [shell]
  (assoc shell :showing false))

(defn arm-stock-placement!
  [diagram]
  (model/arm-stock-placement diagram))

(defn place-stock!
  [diagram x y]
  (model/place-stock diagram x y))

(defn fixture-stock!
  [diagram name x y]
  (model/fixture-stock diagram name x y))

(defn arm-flow-placement!
  [diagram]
  (model/arm-flow-placement diagram))

(defn select-flow-source!
  [diagram stock-name]
  (model/select-flow-source diagram stock-name))

(defn connect-flow!
  [diagram to-stock]
  (model/connect-flow diagram to-stock))

(defn fixture-flow!
  [diagram flow-name from-stock to-stock]
  (model/fixture-flow diagram flow-name from-stock to-stock))

(defn arm-stock-placement-on-shell!
  [shell]
  (update shell :diagram arm-stock-placement!))

(defn place-stock-on-shell!
  [shell x y]
  (update shell :diagram #(place-stock! % x y)))

(defn arm-flow-placement-on-shell!
  [shell]
  (update shell :diagram arm-flow-placement!))

(defn select-flow-stock-on-shell!
  [shell stock-name]
  (update shell :diagram
          (fn [diagram]
            (if (:flow-draft diagram)
              (connect-flow! diagram stock-name)
              (select-flow-source! diagram stock-name)))))