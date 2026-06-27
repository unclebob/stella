(ns stella.ui.edit-dialog-common)

(defn labeled-field [row label field-id text]
  [{:fx/type :label :text label :grid-column 0 :grid-row row}
   {:fx/type :text-field
    :id field-id
    :text (str text)
    :grid-column 1
    :grid-row row
    :pref-width 200}])