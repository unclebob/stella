(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.actions :as actions]
            [stella.commands :as cmd]
            [stella.fx.effects :as fx-effects]
            [stella.ui.root :as root]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(defn event-type
  [event]
  (or (:event event) (:event/type event)))

(defn event-action
  [event]
  (some-> event event-type actions/event->action))

(defn update-shell
  [shell action]
  (cond
    (= action :quit) (cmd/quit! shell)
    (= action :show-about) (cmd/show-about! shell)
    :else shell))

(defn process-app-event
  [event]
  (when-let [action (event-action event)]
    {:action action
     :effect (actions/action->effect action)}))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (when-let [{:keys [action effect]} (process-app-event event)]
     (swap! state-atom update-shell action)
     (when effect (fx-effects/run-effect effect)))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [_] (root/root-desc @*state)))
   :opts {:fx.opt/map-event-handler dispatch-map-event!}))

(defn start!
  []
  (reset! *state (cmd/default-shell! nil))
  (fx/mount-renderer *state renderer))