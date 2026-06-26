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

(defn select-flow-endpoint-on-shell!
  [shell kind name]
  (update shell :diagram
          (fn [diagram]
            (if (:flow-draft diagram)
              (connect-flow! diagram kind name)
              (select-flow-source! diagram kind name)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:42:20.18797-05:00", :module-hash "502776803", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "273392964"} {:id "defn/default-shell!", :kind "defn", :line 4, :end-line 6, :hash "-2121201791"} {:id "defn/default-diagram!", :kind "defn", :line 8, :end-line 10, :hash "605260130"} {:id "defn/show-about!", :kind "defn", :line 12, :end-line 16, :hash "-1322911364"} {:id "defn/quit!", :kind "defn", :line 18, :end-line 20, :hash "-6014701"} {:id "defn/arm-stock-placement!", :kind "defn", :line 22, :end-line 24, :hash "1528122585"} {:id "defn/place-stock!", :kind "defn", :line 26, :end-line 28, :hash "327264207"} {:id "defn/fixture-stock!", :kind "defn", :line 30, :end-line 32, :hash "649142275"} {:id "defn/arm-source-placement!", :kind "defn", :line 34, :end-line 36, :hash "-126889149"} {:id "defn/place-source!", :kind "defn", :line 38, :end-line 40, :hash "-346190542"} {:id "defn/arm-sink-placement!", :kind "defn", :line 42, :end-line 44, :hash "-1217049189"} {:id "defn/place-sink!", :kind "defn", :line 46, :end-line 48, :hash "1657306823"} {:id "defn/fixture-source!", :kind "defn", :line 50, :end-line 52, :hash "1604815373"} {:id "defn/fixture-sink!", :kind "defn", :line 54, :end-line 56, :hash "-1292347174"} {:id "defn/arm-flow-placement!", :kind "defn", :line 58, :end-line 60, :hash "-2049350910"} {:id "defn/select-flow-source!", :kind "defn", :line 62, :end-line 64, :hash "233734405"} {:id "defn/connect-flow!", :kind "defn", :line 66, :end-line 68, :hash "736803171"} {:id "defn/fixture-flow!", :kind "defn", :line 70, :end-line 72, :hash "1369753620"} {:id "defn/arm-stock-placement-on-shell!", :kind "defn", :line 74, :end-line 76, :hash "1559005654"} {:id "defn/place-stock-on-shell!", :kind "defn", :line 78, :end-line 80, :hash "-1939405487"} {:id "defn/arm-source-placement-on-shell!", :kind "defn", :line 82, :end-line 84, :hash "-1886329299"} {:id "defn/place-source-on-shell!", :kind "defn", :line 86, :end-line 88, :hash "-633997888"} {:id "defn/arm-sink-placement-on-shell!", :kind "defn", :line 90, :end-line 92, :hash "1720300578"} {:id "defn/place-sink-on-shell!", :kind "defn", :line 94, :end-line 96, :hash "-499332881"} {:id "defn/arm-flow-placement-on-shell!", :kind "defn", :line 98, :end-line 100, :hash "-226480789"} {:id "defn/select-flow-endpoint-on-shell!", :kind "defn", :line 102, :end-line 108, :hash "588213657"}]}
;; clj-mutate-manifest-end
