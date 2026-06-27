(ns stella.fx.edit-flow-dialog
  (:require [stella.events :as events])
  (:import [javafx.event EventHandler]
           [javafx.geometry Insets]
           [javafx.scene.control Button ButtonType Dialog DialogPane Label TextField]
           [javafx.scene.layout GridPane]))

(defn- labeled-row [^GridPane grid row label ^TextField field]
  (.add grid (Label. label) 0 row)
  (.add grid field 1 row))

(defn- read-draft [^TextField name-field ^TextField rate-field]
  {:name (.getText name-field)
   :rate (.getText rate-field)})

(defn show!
  [draft on-event!]
  (let [^TextField name-field (doto (TextField.)
                                 (.setId "edit-flow-name")
                                 (.setText (str (:name draft))))
        ^TextField rate-field (doto (TextField.)
                               (.setId "edit-flow-rate")
                               (.setText (str (:rate draft))))
        ^GridPane grid (doto (GridPane.)
                          (.setHgap 8)
                          (.setVgap 8)
                          (.setPadding (Insets. 12 12 12 12)))
        ^Dialog dialog (Dialog.)]
    (labeled-row grid 0 "Name" name-field)
    (labeled-row grid 1 "Rate" rate-field)
    (.setTitle dialog "Edit Flow")
    (.setHeaderText dialog nil)
    (let [^DialogPane pane (.getDialogPane dialog)]
      (.set (.contentProperty pane) grid)
      (.addAll (.getButtonTypes pane) (into-array ButtonType [ButtonType/CANCEL ButtonType/OK]))
      (when-let [^Button ok-button (.lookupButton pane ButtonType/OK)]
        (.setDefaultButton ok-button true)
        (.setOnAction ok-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-flow-apply
                                      :draft (read-draft name-field rate-field)})
                          (.close dialog)))))
      (when-let [^Button cancel-button (.lookupButton pane ButtonType/CANCEL)]
        (.setOnAction cancel-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-flow-cancel})
                          (.close dialog))))))
    (.show dialog)))