(ns stella.commands
  (:require [stella.model :as model]))

(defn default-shell!
  [_]
  (model/default-shell))

(defn show-about!
  [shell]
  (-> shell
      (assoc :about-visible true)
      (assoc :about-text "Stella\nA system dynamics diagram editor.")))

(defn quit!
  [shell]
  (assoc shell :showing false))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:23:14.830081-05:00", :module-hash "3633131", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "273392964"} {:id "defn/default-shell!", :kind "defn", :line 4, :end-line 6, :hash "-2121201791"} {:id "defn/show-about!", :kind "defn", :line 8, :end-line 12, :hash "-1322911364"} {:id "defn/quit!", :kind "defn", :line 14, :end-line 16, :hash "-6014701"}]}
;; clj-mutate-manifest-end
