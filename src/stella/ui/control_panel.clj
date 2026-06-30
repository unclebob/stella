(ns stella.ui.control-panel
  (:require [stella.events :as events]
            [stella.simulation :as simulation]))

(defn control-panel-desc
  [shell]
  {:fx/type :hbox
   :id "control-panel"
   :alignment :center-left
   :style "-fx-background-color: #f0f0f0; -fx-padding: 8;"
   :spacing 12
   :children [{:fx/type :button
               :id "step-button"
               :text "Step"
               :on-action {:event events/simulation-step}}
              {:fx/type :label
               :id "simulation-time-display"
               :text (simulation/shell-time-display shell)}]})