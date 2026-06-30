(ns stella.ui.control-panel
  (:require [stella.events :as events]
            [stella.model :as model]))

(defn control-panel-desc
  [shell]
  {:fx/type :h-box
   :id "control-panel"
   :style "-fx-background-color: #f0f0f0; -fx-padding: 8;"
   :spacing 12
   :children [{:fx/type :button
               :id "step-button"
               :text "Step"
               :on-action {:event events/simulation-step}}
              {:fx/type :slider
               :id "simulation-speed-slider"
               :min 0.0
               :max 1.0
               :value (Double/parseDouble (model/simulation-tick-delay-display shell))}
              {:fx/type :label
               :id "simulation-tick-delay-display"
               :text (model/simulation-tick-delay-display shell)}
              {:fx/type :button
               :id "simulation-run-button"
               :text (model/run-button-label shell)}
              {:fx/type :label
               :id "simulation-time-display"
               :text (model/simulation-time-display shell)}]})
