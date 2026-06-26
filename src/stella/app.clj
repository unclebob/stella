(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.actions :as actions]
            [stella.fx.effects :as fx-effects]
            [stella.ui.root :as root]))

(defn- handle-app-event [event]
  (some-> event :fx/event-type
          actions/event->action
          actions/action->effect
          fx-effects/run-effect))

(defn start!
  []
  (fx/create-app
   {:desc-fn (fn [_] (root/root-desc))
    :event-handler handle-app-event}))