(ns stella.ui.palette
  (:require [stella.events :as events]))

(def ^:private button-spacing 36)

(defn- palette-button [index label event]
  {:fx/type :button
   :layout-y (* index button-spacing)
   :text label
   :on-action {:event event}})

(defn palette-desc
  []
  {:fx/type :pane
   :style "-fx-background-color: #e8e8e8;"
   :pref-width 80
   :min-width 80
   :children [(palette-button 0 "Stock" events/arm-stock)
              (palette-button 1 "Flow" events/arm-flow)
              (palette-button 2 "Source" events/arm-source)
              (palette-button 3 "Sink" events/arm-sink)
              (palette-button 4 "Converter" events/arm-converter)
              (palette-button 5 "Connector" events/arm-connector)]})
