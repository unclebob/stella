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
