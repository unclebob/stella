(ns stella.ui.edit-converter-dialog
  (:require [stella.events :as events]
            [stella.ui.edit-dialog-common :as edit-dialog]))

(defn edit-converter-overlay-desc
  [{:keys [name formula]}]
  {:fx/type :vbox
   :id "edit-converter-overlay"
   :style "-fx-background-color: white; -fx-padding: 16; -fx-border-color: #666; -fx-border-width: 1;"
   :spacing 12
   :max-width 320
   :children [{:fx/type :label
               :text "Edit Converter"
               :id "edit-converter-title"
               :style "-fx-font-size: 14px; -fx-font-weight: bold;"}
              {:fx/type :grid-pane
               :hgap 8
               :vgap 8
               :children (vec (mapcat (fn [[row label field-id text]]
                                        (edit-dialog/labeled-field row label field-id text))
                                      [[0 "Name" "edit-converter-name" name]
                                       [1 "Formula" "edit-converter-formula" formula]]))}
              {:fx/type :hbox
               :spacing 8
               :children [{:fx/type :button
                            :text "Cancel"
                            :on-action {:event events/edit-converter-cancel}}
                           {:fx/type :button
                            :text "OK"
                            :default-button true
                            :on-action {:event events/edit-converter-apply}}]}]})