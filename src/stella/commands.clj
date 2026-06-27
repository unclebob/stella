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

(defn move-stock!
  [diagram name x y]
  (model/move-stock diagram name x y))

(defn move-converter!
  [diagram name x y]
  (model/move-converter diagram name x y))

(defn set-stock-name!
  [diagram old-name new-name]
  (model/set-stock-name diagram old-name new-name))

(defn set-stock-initial-value!
  [diagram name value]
  (model/set-stock-initial-value diagram name value))

(defn set-stock-min!
  [diagram name min-value]
  (model/set-stock-min diagram name min-value))

(defn set-stock-max!
  [diagram name max-value]
  (model/set-stock-max diagram name max-value))

(defn clear-stock-max!
  [diagram name]
  (model/clear-stock-max diagram name))

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

(defn set-flow-name!
  [diagram old-name new-name]
  (model/set-flow-name diagram old-name new-name))

(defn set-flow-rate!
  [diagram name rate]
  (model/set-flow-rate diagram name rate))

(defn arm-converter-placement!
  [diagram]
  (model/arm-converter-placement diagram))

(defn place-converter!
  [diagram x y]
  (model/place-converter diagram x y))

(defn fixture-converter!
  [diagram name x y]
  (model/fixture-converter diagram name x y))

(defn fixture-connector!
  [diagram connector-name from-converter to-flow]
  (model/fixture-connector diagram connector-name from-converter to-flow))

(defn set-converter-name!
  [diagram old-name new-name]
  (model/set-converter-name diagram old-name new-name))

(defn set-converter-formula!
  [diagram name formula]
  (model/set-converter-formula diagram name formula))

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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:22:38.401717-05:00", :module-hash "326420829", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "273392964"} {:id "defn/default-shell!", :kind "defn", :line 4, :end-line 6, :hash "-2121201791"} {:id "defn/default-diagram!", :kind "defn", :line 8, :end-line 10, :hash "605260130"} {:id "defn/show-about!", :kind "defn", :line 12, :end-line 16, :hash "-1322911364"} {:id "defn/quit!", :kind "defn", :line 18, :end-line 20, :hash "-6014701"} {:id "defn/arm-stock-placement!", :kind "defn", :line 22, :end-line 24, :hash "1528122585"} {:id "defn/place-stock!", :kind "defn", :line 26, :end-line 28, :hash "327264207"} {:id "defn/fixture-stock!", :kind "defn", :line 30, :end-line 32, :hash "649142275"} {:id "defn/move-stock!", :kind "defn", :line 34, :end-line 36, :hash "-1366856280"} {:id "defn/move-converter!", :kind "defn", :line 38, :end-line 40, :hash "1758477189"} {:id "defn/set-stock-name!", :kind "defn", :line 42, :end-line 44, :hash "980633483"} {:id "defn/set-stock-initial-value!", :kind "defn", :line 46, :end-line 48, :hash "1954449060"} {:id "defn/set-stock-min!", :kind "defn", :line 50, :end-line 52, :hash "-842214619"} {:id "defn/set-stock-max!", :kind "defn", :line 54, :end-line 56, :hash "-1580614357"} {:id "defn/clear-stock-max!", :kind "defn", :line 58, :end-line 60, :hash "1483601368"} {:id "defn/arm-source-placement!", :kind "defn", :line 62, :end-line 64, :hash "-126889149"} {:id "defn/place-source!", :kind "defn", :line 66, :end-line 68, :hash "-346190542"} {:id "defn/arm-sink-placement!", :kind "defn", :line 70, :end-line 72, :hash "-1217049189"} {:id "defn/place-sink!", :kind "defn", :line 74, :end-line 76, :hash "1657306823"} {:id "defn/fixture-source!", :kind "defn", :line 78, :end-line 80, :hash "1604815373"} {:id "defn/fixture-sink!", :kind "defn", :line 82, :end-line 84, :hash "-1292347174"} {:id "defn/arm-flow-placement!", :kind "defn", :line 86, :end-line 88, :hash "-2049350910"} {:id "defn/select-flow-source!", :kind "defn", :line 90, :end-line 92, :hash "233734405"} {:id "defn/connect-flow!", :kind "defn", :line 94, :end-line 96, :hash "736803171"} {:id "defn/fixture-flow!", :kind "defn", :line 98, :end-line 100, :hash "1369753620"} {:id "defn/set-flow-name!", :kind "defn", :line 102, :end-line 104, :hash "-2006740562"} {:id "defn/set-flow-rate!", :kind "defn", :line 106, :end-line 108, :hash "-605461932"} {:id "defn/arm-converter-placement!", :kind "defn", :line 110, :end-line 112, :hash "1417442163"} {:id "defn/place-converter!", :kind "defn", :line 114, :end-line 116, :hash "-872494041"} {:id "defn/fixture-converter!", :kind "defn", :line 118, :end-line 120, :hash "1640813265"} {:id "defn/fixture-connector!", :kind "defn", :line 122, :end-line 124, :hash "663905514"} {:id "defn/set-converter-name!", :kind "defn", :line 126, :end-line 128, :hash "-416090274"} {:id "defn/set-converter-formula!", :kind "defn", :line 130, :end-line 132, :hash "-1811945733"} {:id "defn/arm-connector-placement!", :kind "defn", :line 134, :end-line 136, :hash "-785743789"} {:id "defn/select-connector-origin!", :kind "defn", :line 138, :end-line 140, :hash "-1004406321"} {:id "defn/connect-connector!", :kind "defn", :line 142, :end-line 144, :hash "1425348016"} {:id "defn/arm-stock-placement-on-shell!", :kind "defn", :line 146, :end-line 148, :hash "1559005654"} {:id "defn/place-stock-on-shell!", :kind "defn", :line 150, :end-line 152, :hash "-1939405487"} {:id "defn/arm-source-placement-on-shell!", :kind "defn", :line 154, :end-line 156, :hash "-1886329299"} {:id "defn/place-source-on-shell!", :kind "defn", :line 158, :end-line 160, :hash "-633997888"} {:id "defn/arm-sink-placement-on-shell!", :kind "defn", :line 162, :end-line 164, :hash "1720300578"} {:id "defn/place-sink-on-shell!", :kind "defn", :line 166, :end-line 168, :hash "-499332881"} {:id "defn/arm-flow-placement-on-shell!", :kind "defn", :line 170, :end-line 172, :hash "-226480789"} {:id "defn/arm-converter-placement-on-shell!", :kind "defn", :line 174, :end-line 176, :hash "1418940624"} {:id "defn/place-converter-on-shell!", :kind "defn", :line 178, :end-line 180, :hash "116885808"} {:id "defn/arm-connector-placement-on-shell!", :kind "defn", :line 182, :end-line 184, :hash "1279171735"} {:id "defn-/select-endpoint-on-diagram", :kind "defn-", :line 186, :end-line 195, :hash "1840493823"} {:id "defn/select-endpoint-on-shell!", :kind "defn", :line 197, :end-line 199, :hash "-75615409"}]}
;; clj-mutate-manifest-end
