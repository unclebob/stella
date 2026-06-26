(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]
           [javafx.scene.control Alert Alert$AlertType]))

(defn- about-dialog []
  (doto (Alert. Alert$AlertType/INFORMATION)
    (.setTitle "About Stella")
    (.setHeaderText "Stella")
    (.setContentText "A system dynamics diagram editor.")
    .showAndWait))

(defn start!
  []
  (fx/create-app
   {:desc-fn (fn [_] (root/root-desc))
    :event-handler (fn [event]
                     (case (:fx/event-type event)
                       :stella.app/quit (Platform/exit)
                       :stella.app/show-about (about-dialog)
                       nil))}))