(ns stella.ui.palette
  (:require [stella.events :as events]))

(defn palette-desc
  []
  {:fx/type :vbox
   :style "-fx-padding: 8; -fx-spacing: 8; -fx-background-color: #e8e8e8;"
   :pref-width 80
   :children [{:fx/type :button
               :text "Stock"
               :on-action {:event events/arm-stock}}]})