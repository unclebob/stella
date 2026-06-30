(ns stella.acceptance.steps
  (:require [stella.acceptance.step-handlers-connect :as connect-handlers]
            [stella.acceptance.step-handlers-palette :as palette-handlers]
            [stella.acceptance.step-handlers-placement :as placement-handlers]
            [stella.acceptance.step-handlers-selection :as selection-handlers]
            [stella.acceptance.step-handlers-simulation :as simulation-handlers]
            [stella.acceptance.step-handlers-thermometer :as thermometer-handlers]
            [stella.acceptance.step-support :as support]))

(def step-handlers
  (into placement-handlers/placement-handlers
        (into connect-handlers/connect-handlers
              (into selection-handlers/selection-handlers
                    (into simulation-handlers/simulation-handlers
                          (into thermometer-handlers/thermometer-handlers
                                palette-handlers/palette-handlers))))))

(defn dispatch-step
  [world step example]
  (let [{:keys [text]} step]
    (if-let [handler (first (filter #(re-matches (:pattern %) text) step-handlers))]
      ((:fn handler) world (re-matches (:pattern handler) text) example)
      (support/fail! (str "unsupported step: " text)))))