(ns stella.ui.edit-converter-dialog
  (:require [stella.events :as events]
            [stella.ui.edit-dialog-common :as edit-dialog]))

(defn edit-converter-overlay-desc
  [{:keys [name formula]}]
  (edit-dialog/edit-overlay-desc
   {:id "edit-converter-overlay"
    :title "Edit Converter"
    :title-id "edit-converter-title"
    :fields [[0 "Name" "edit-converter-name" name]
             [1 "Formula" "edit-converter-formula" formula]]
    :cancel-event events/edit-converter-cancel
    :apply-event events/edit-converter-apply}))
