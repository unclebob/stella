(ns stella.acceptance.steps
  (:require [stella.acceptance.step-handlers-connect :as connect-handlers]
            [stella.acceptance.step-handlers-placement :as placement-handlers]
            [stella.acceptance.step-support :as support]))

(def step-handlers
  (into placement-handlers/placement-handlers connect-handlers/connect-handlers))

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (support/fail! (str "unsupported step: " text)))))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:14:00.859823-05:00", :module-hash "44416234", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1843614707"} {:id "def/step-handlers", :kind "def", :line 6, :end-line 7, :hash "-746413770"} {:id "defn/dispatch-step", :kind "defn", :line 9, :end-line 14, :hash "1916751137"}]}
;; clj-mutate-manifest-end
