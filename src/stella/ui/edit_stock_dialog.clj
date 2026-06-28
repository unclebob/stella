(ns stella.ui.edit-stock-dialog
  (:require [stella.events :as events]
            [stella.ui.edit-dialog-common :as edit-dialog]))

(defn edit-stock-overlay-desc
  [{:keys [name initial-value min-value max-value]}]
  {:fx/type :v-box
   :id "edit-stock-overlay"
   :style "-fx-background-color: white; -fx-padding: 16; -fx-border-color: #666; -fx-border-width: 1;"
   :spacing 12
   :max-width 320
   :children [{:fx/type :label
               :text "Edit Stock"
               :id "edit-stock-title"
               :style "-fx-font-size: 14px; -fx-font-weight: bold;"}
              {:fx/type :grid-pane
               :hgap 8
               :vgap 8
               :children (vec (mapcat (fn [[row label field-id text]]
                                        (edit-dialog/labeled-field row label field-id text))
                                      [[0 "Name" "edit-stock-name" name]
                                       [1 "Initial value" "edit-stock-initial" initial-value]
                                       [2 "Minimum" "edit-stock-min" min-value]
                                       [3 "Maximum" "edit-stock-max" max-value]]))}
              {:fx/type :h-box
               :spacing 8
               :children [{:fx/type :button
                            :text "Cancel"
                            :on-action {:event events/edit-stock-cancel}}
                           {:fx/type :button
                            :text "OK"
                            :default-button true
                            :on-action {:event events/edit-stock-apply}}]}]})
