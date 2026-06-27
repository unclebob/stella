(ns stella.fx.edit-converter-dialog
  (:require [stella.events :as events])
  (:import [javafx.event EventHandler]
           [javafx.geometry Insets]
           [javafx.scene.control Button ButtonType Dialog DialogPane Label TextField]
           [javafx.scene.layout GridPane]))

(defn- labeled-row [^GridPane grid row label ^TextField field]
  (.add grid (Label. label) 0 row)
  (.add grid field 1 row))

(defn- read-draft [^TextField name-field ^TextField formula-field]
  {:name (.getText name-field)
   :formula (.getText formula-field)})

(defn show!
  [draft on-event!]
  (let [^TextField name-field (doto (TextField.)
                                 (.setId "edit-converter-name")
                                 (.setText (str (:name draft))))
        ^TextField formula-field (doto (TextField.)
                                    (.setId "edit-converter-formula")
                                    (.setText (str (:formula draft))))
        ^GridPane grid (doto (GridPane.)
                          (.setHgap 8)
                          (.setVgap 8)
                          (.setPadding (Insets. 12 12 12 12)))
        ^Dialog dialog (Dialog.)]
    (labeled-row grid 0 "Name" name-field)
    (labeled-row grid 1 "Formula" formula-field)
    (.setTitle dialog "Edit Converter")
    (.setHeaderText dialog nil)
    (let [^DialogPane pane (.getDialogPane dialog)]
      (.set (.contentProperty pane) grid)
      (.addAll (.getButtonTypes pane) (into-array ButtonType [ButtonType/CANCEL ButtonType/OK]))
      (when-let [^Button ok-button (.lookupButton pane ButtonType/OK)]
        (.setDefaultButton ok-button true)
        (.setOnAction ok-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-converter-apply
                                      :draft (read-draft name-field formula-field)})
                          (.close dialog)))))
      (when-let [^Button cancel-button (.lookupButton pane ButtonType/CANCEL)]
        (.setOnAction cancel-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-converter-cancel})
                          (.close dialog))))))
    (.show dialog)))