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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T17:11:32.40237-05:00", :module-hash "290696503", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "1659448743"} {:id "defn/palette-desc", :kind "defn", :line 4, :end-line 26, :hash "-109303517"}]}
;; clj-mutate-manifest-end
