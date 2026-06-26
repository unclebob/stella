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

(defn arm-source-placement!
  [diagram]
  (model/arm-source-placement diagram))

(defn place-source!
  [diagram x y]
  (model/place-source diagram x y))

(defn arm-sink-placement!
  [diagram]
  (model/arm-sink-placement diagram))

(defn place-sink!
  [diagram x y]
  (model/place-sink diagram x y))

(defn fixture-source!
  [diagram name x y]
  (model/fixture-source diagram name x y))

(defn fixture-sink!
  [diagram name x y]
  (model/fixture-sink diagram name x y))

(defn arm-flow-placement!
  [diagram]
  (model/arm-flow-placement diagram))

(defn select-flow-source!
  [diagram kind name]
  (model/select-flow-source diagram kind name))

(defn connect-flow!
  [diagram kind name]
  (model/connect-flow diagram kind name))

(defn fixture-flow!
  [diagram flow-name from-stock to-stock]
  (model/fixture-flow diagram flow-name from-stock to-stock))

(defn arm-converter-placement!
  [diagram]
  (model/arm-converter-placement diagram))

(defn place-converter!
  [diagram x y]
  (model/place-converter diagram x y))

(defn fixture-converter!
  [diagram name x y]
  (model/fixture-converter diagram name x y))

(defn arm-connector-placement!
  [diagram]
  (model/arm-connector-placement diagram))

(defn select-connector-origin!
  [diagram kind name]
  (model/select-connector-origin diagram kind name))

(defn connect-connector!
  [diagram kind name]
  (model/connect-connector diagram kind name))

(defn arm-stock-placement-on-shell!
  [shell]
  (update shell :diagram arm-stock-placement!))

(defn place-stock-on-shell!
  [shell x y]
  (update shell :diagram #(place-stock! % x y)))

(defn arm-source-placement-on-shell!
  [shell]
  (update shell :diagram arm-source-placement!))

(defn place-source-on-shell!
  [shell x y]
  (update shell :diagram #(place-source! % x y)))

(defn arm-sink-placement-on-shell!
  [shell]
  (update shell :diagram arm-sink-placement!))

(defn place-sink-on-shell!
  [shell x y]
  (update shell :diagram #(place-sink! % x y)))

(defn arm-flow-placement-on-shell!
  [shell]
  (update shell :diagram arm-flow-placement!))

(defn arm-converter-placement-on-shell!
  [shell]
  (update shell :diagram arm-converter-placement!))

(defn place-converter-on-shell!
  [shell x y]
  (update shell :diagram #(place-converter! % x y)))

(defn arm-connector-placement-on-shell!
  [shell]
  (update shell :diagram arm-connector-placement!))

(defn- select-endpoint-on-diagram
  [diagram kind name]
  (case (:placement-mode diagram)
    :flow (if (:flow-draft diagram)
            (connect-flow! diagram kind name)
            (select-flow-source! diagram kind name))
    :connector (if (:connector-draft diagram)
                 (connect-connector! diagram kind name)
                 (select-connector-origin! diagram kind name))
    diagram))

(defn select-endpoint-on-shell!
  [shell kind name]
  (update shell :diagram #(select-endpoint-on-diagram % kind name)))