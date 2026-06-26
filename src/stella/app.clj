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

(def ^:private diagram-events
  #{events/arm-stock events/arm-flow events/arm-source events/arm-sink
    events/arm-converter events/arm-connector events/canvas-click
    events/endpoint-click})

(defn event-type
  [event]
  (or (:event event) (:event/type event)))

(defn event-action
  [event]
  (some-> event event-type actions/event->action))

(defn diagram-event?
  [event-type]
  (contains? diagram-events event-type))

(defn update-shell
  [shell action]
  (cond
    (= action :quit) (cmd/quit! shell)
    (= action :show-about) (cmd/show-about! shell)
    :else shell))

(defn click-coordinates
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getX mouse)) (int (.getY mouse))]))

(defn place-on-canvas
  [shell x y]
  (case (get-in shell [:diagram :placement-mode])
    :stock (cmd/place-stock-on-shell! shell x y)
    :source (cmd/place-source-on-shell! shell x y)
    :sink (cmd/place-sink-on-shell! shell x y)
    :converter (cmd/place-converter-on-shell! shell x y)
    shell))

(defn place-stock-at-coordinates
  [shell [x y]]
  (cmd/place-stock-on-shell! shell x y))

(defn place-from-canvas-click
  [shell event]
  (if-let [[x y] (click-coordinates event)]
    (place-on-canvas shell x y)
    shell))

(defn endpoint-from-event
  [event]
  (when (and (:endpoint-kind event) (:endpoint-name event))
    {:kind (:endpoint-kind event) :name (:endpoint-name event)}))

(defn select-endpoint-from-event
  [shell event]
  (if-let [{:keys [kind name]} (endpoint-from-event event)]
    (cmd/select-endpoint-on-shell! shell kind name)
    shell))

(defn- diagram-shell-updaters
  []
  {events/arm-stock (fn [shell _] (cmd/arm-stock-placement-on-shell! shell))
   events/arm-flow (fn [shell _] (cmd/arm-flow-placement-on-shell! shell))
   events/arm-source (fn [shell _] (cmd/arm-source-placement-on-shell! shell))
   events/arm-sink (fn [shell _] (cmd/arm-sink-placement-on-shell! shell))
   events/arm-converter (fn [shell _] (cmd/arm-converter-placement-on-shell! shell))
   events/arm-connector (fn [shell _] (cmd/arm-connector-placement-on-shell! shell))
   events/endpoint-click select-endpoint-from-event
   events/canvas-click place-from-canvas-click})

(defn update-shell-for-diagram-event
  [shell event]
  (if-let [updater (get (diagram-shell-updaters) (event-type event))]
    (updater shell event)
    shell))

(defn process-app-event
  [event]
  (when-let [action (event-action event)]
    {:action action
     :effect (actions/action->effect action)}))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (let [etype (event-type event)]
     (if (diagram-event? etype)
       (swap! state-atom update-shell-for-diagram-event event)
       (when-let [{:keys [action effect]} (process-app-event event)]
         (swap! state-atom update-shell action)
         (when effect (fx-effects/run-effect effect)))))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [shell] (root/root-desc shell)))
   :opts {:fx.opt/map-event-handler dispatch-map-event!}))

(defn start!
  []
  (reset! *state (cmd/default-shell! nil))
  (fx/mount-renderer *state renderer))