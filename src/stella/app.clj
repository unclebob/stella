(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.actions :as actions]
            [stella.fx.effects :as fx-effects]
            [stella.ui.root :as root]))

(def ^:private initial-state
  {:showing true})

(defonce *state
  (atom initial-state))

(defn- handle-map-event [event]
  (when-let [event-type (or (:event event) (:event/type event))]
    (when-let [action (actions/event->action event-type)]
      (when-let [effect (actions/action->effect action)]
        (fx-effects/run-effect effect)))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [_] (root/root-desc)))
   :opts {:fx.opt/map-event-handler handle-map-event}))

(defn start!
  []
  (fx/mount-renderer *state renderer))