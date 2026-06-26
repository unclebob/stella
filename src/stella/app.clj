(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.commands :as cmd]
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
  (let [state (atom (cmd/default-shell! nil))]
    (fx/create-app
     {:desc-fn (fn [_] (root/root-desc @state))
      :event-handler (fn [event]
                       (case (:fx/event-type event)
                         :stella.app/quit (do (swap! state cmd/quit!)
                                              (Platform/exit))
                         :stella.app/show-about (do (swap! state cmd/show-about!)
                                                    (about-dialog))
                         nil))})))