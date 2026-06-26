(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.actions :as actions]
            [stella.commands :as cmd]
            [stella.fx.effects :as fx-effects]
            [stella.ui.root :as root]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(defn- apply-action!
  [action]
  (case action
    :quit (swap! *state cmd/quit!)
    :show-about (swap! *state cmd/show-about!)
    nil))

(defn- handle-map-event
  [event]
  (when-let [event-type (or (:event event) (:event/type event))]
    (when-let [action (actions/event->action event-type)]
      (apply-action! action)
      (when-let [effect (actions/action->effect action)]
        (fx-effects/run-effect effect)))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [shell] (root/root-desc shell)))
   :opts {:fx.opt/map-event-handler handle-map-event}))

(defn start!
  []
  (fx/mount-renderer *state renderer))