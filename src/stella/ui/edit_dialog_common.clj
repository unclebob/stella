(ns stella.ui.edit-dialog-common)

(defn labeled-field [row label field-id text]
  [{:fx/type :label :text label :grid-column 0 :grid-row row}
   {:fx/type :text-field
    :id field-id
    :text (str text)
    :grid-column 1
    :grid-row row
    :pref-width 200}])

(defn edit-overlay-desc
  [{:keys [id title title-id fields cancel-event apply-event]}]
  {:fx/type :vbox
   :id id
   :style "-fx-background-color: white; -fx-padding: 16; -fx-border-color: #666; -fx-border-width: 1;"
   :spacing 12
   :max-width 320
   :children [{:fx/type :label
               :text title
               :id title-id
               :style "-fx-font-size: 14px; -fx-font-weight: bold;"}
              {:fx/type :grid-pane
               :hgap 8
               :vgap 8
               :children (vec (mapcat (fn [[row label field-id text]]
                                        (labeled-field row label field-id text))
                                      fields))}
              {:fx/type :hbox
               :spacing 8
               :children [{:fx/type :button
                            :text "Cancel"
                            :on-action {:event cancel-event}}
                           {:fx/type :button
                            :text "OK"
                            :default-button true
                            :on-action {:event apply-event}}]}]})
