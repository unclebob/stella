(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.commands :as cmd]
            [stella.dispatch :as dispatch]
            [stella.fx.effects :as fx-effects]
            [stella.fx.input :as fx-input]
            [stella.ui.root :as root]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (let [event (fx-input/enrich-event event)
         effect (dispatch/event-effect event)]
     (swap! state-atom dispatch/apply-event event)
     (when effect (fx-effects/run-effect effect)))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [shell] (root/root-desc shell)))
   :opts {:fx.opt/map-event-handler dispatch-map-event!}))

(defn start!
  []
  (reset! *state (cmd/default-shell! nil))
  (fx/mount-renderer *state renderer))