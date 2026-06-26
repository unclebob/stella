(ns stella.ui.palette
  (:require [stella.events :as events]))

(defn palette-desc
  []
  {:fx/type :vbox
   :style "-fx-padding: 8; -fx-spacing: 8; -fx-background-color: #e8e8e8;"
   :pref-width 80
   :children [{:fx/type :button
               :text "Stock"
               :on-action {:event events/arm-stock}}
              {:fx/type :button
               :text "Flow"
               :on-action {:event events/arm-flow}}
              {:fx/type :button
               :text "Source"
               :on-action {:event events/arm-source}}
              {:fx/type :button
               :text "Sink"
               :on-action {:event events/arm-sink}}
              {:fx/type :button
               :text "Converter"
               :on-action {:event events/arm-converter}}
              {:fx/type :button
               :text "Connector"
               :on-action {:event events/arm-connector}}]})
