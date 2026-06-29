(ns stella.ui.edit-flow-dialog
  (:require [stella.events :as events]
            [stella.ui.edit-dialog-common :as edit-dialog]))

(defn edit-flow-overlay-desc
  [{:keys [name rate]}]
  (edit-dialog/edit-overlay-desc
   {:id "edit-flow-overlay"
    :title "Edit Flow"
    :title-id "edit-flow-title"
    :fields [[0 "Name" "edit-flow-name" name]
             [1 "Rate" "edit-flow-rate" rate]]
    :cancel-event events/edit-flow-cancel
    :apply-event events/edit-flow-apply}))
