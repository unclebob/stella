(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.actions :as actions]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.fx.effects :as fx-effects]
            [stella.ui.root :as root])
  (:import [javafx.scene.input MouseEvent]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(defn- apply-shell-action!
  [action]
  (case action
    :quit (swap! *state cmd/quit!)
    :show-about (swap! *state cmd/show-about!)
    nil))

(defn- handle-map-event
  [event]
  (let [event-type (or (:event event) (:event/type event))]
    (cond
      (= event-type events/arm-stock)
      (swap! *state cmd/arm-stock-placement-on-shell!)

      (= event-type events/canvas-click)
      (when-let [mouse ^MouseEvent (:fx/event event)]
        (swap! *state #(cmd/place-stock-on-shell! % (int (.getX mouse)) (int (.getY mouse)))))

      :else
      (when-let [action (actions/event->action event-type)]
        (apply-shell-action! action)
        (when-let [effect (actions/action->effect action)]
          (fx-effects/run-effect effect))))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [shell] (root/root-desc shell)))
   :opts {:fx.opt/map-event-handler handle-map-event}))

(defn start!
  []
  (fx/mount-renderer *state renderer))