(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]
           [javafx.scene.control Alert Alert$AlertType]))

(defn app-event-action
  "Maps a CljFX event to an application action keyword, or nil when ignored."
  [event]
  (case (:fx/event-type event)
    :stella.app/quit :quit
    :stella.app/show-about :show-about
    nil))

(defn effect-for-action
  "Maps an application action to an effect keyword executed on the JavaFX thread."
  [action]
  (case action
    :quit :platform-exit
    :show-about :about-dialog
    nil))

(defn- about-dialog []
  (doto (Alert. Alert$AlertType/INFORMATION)
    (.setTitle "About Stella")
    (.setHeaderText "Stella")
    (.setContentText "A system dynamics diagram editor.")
    .showAndWait))

(def ^:private effect-runners
  {:platform-exit #(Platform/exit)
   :about-dialog about-dialog})

(defn- run-app-effect [effect]
  (when-let [run (get effect-runners effect)]
    (run)))

(defn- handle-app-event [event]
  (-> event app-event-action effect-for-action run-app-effect))

(defn start!
  []
  (fx/create-app
   {:desc-fn (fn [_] (root/root-desc))
    :event-handler handle-app-event}))