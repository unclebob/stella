(ns stella.fx.edit-stock-dialog
  (:require [stella.events :as events])
  (:import [javafx.event EventHandler]
           [javafx.geometry Insets]
           [javafx.scene.control Button ButtonType Dialog DialogPane Label TextField]
           [javafx.scene.layout GridPane]))

(defn- labeled-row [^GridPane grid row label ^TextField field]
  (.add grid (Label. label) 0 row)
  (.add grid field 1 row))

(defn- read-draft [^TextField name-field ^TextField initial-field
                   ^TextField min-field ^TextField max-field]
  {:name (.getText name-field)
   :initial-value (.getText initial-field)
   :min-value (.getText min-field)
   :max-value (.getText max-field)})

(defn show!
  [draft on-event!]
  (let [^TextField name-field (doto (TextField.)
                                 (.setId "edit-stock-name")
                                 (.setText (str (:name draft))))
        ^TextField initial-field (doto (TextField.)
                                   (.setId "edit-stock-initial")
                                   (.setText (str (:initial-value draft))))
        ^TextField min-field (doto (TextField.)
                               (.setId "edit-stock-min")
                               (.setText (str (:min-value draft))))
        ^TextField max-field (doto (TextField.)
                               (.setId "edit-stock-max")
                               (.setText (str (:max-value draft))))
        ^GridPane grid (doto (GridPane.)
                          (.setHgap 8)
                          (.setVgap 8)
                          (.setPadding (Insets. 12 12 12 12)))
        ^Dialog dialog (Dialog.)]
    (labeled-row grid 0 "Name" name-field)
    (labeled-row grid 1 "Initial value" initial-field)
    (labeled-row grid 2 "Minimum" min-field)
    (labeled-row grid 3 "Maximum" max-field)
    (.setTitle dialog "Edit Stock")
    (.setHeaderText dialog nil)
    (let [^DialogPane pane (.getDialogPane dialog)]
      (.set (.contentProperty pane) grid)
      (.addAll (.getButtonTypes pane) (into-array ButtonType [ButtonType/CANCEL ButtonType/OK]))
      (when-let [^Button ok-button (.lookupButton pane ButtonType/OK)]
        (.setDefaultButton ok-button true)
        (.setOnAction ok-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-stock-apply
                                      :draft (read-draft name-field initial-field
                                                         min-field max-field)})
                          (.close dialog)))))
      (when-let [^Button cancel-button (.lookupButton pane ButtonType/CANCEL)]
        (.setOnAction cancel-button
                      (proxy [EventHandler] []
                        (handle [_event]
                          (on-event! {:event events/edit-stock-cancel})
                          (.close dialog))))))
    (.show dialog)))