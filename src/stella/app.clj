(ns stella.app
  (:require [cljfx.api :as fx]
            [stella.commands :as cmd]
            [stella.dispatch :as dispatch]
            [stella.events :as events]
            [stella.fx.edit-converter-dialog :as edit-converter-dialog]
            [stella.fx.edit-flow-dialog :as edit-flow-dialog]
            [stella.fx.edit-stock-dialog :as edit-stock-dialog]
            [stella.fx.effects :as fx-effects]
            [stella.fx.input :as fx-input]
            [stella.fx.nodes :as fx-nodes]
            [stella.fx.overlay :as fx-overlay]
            [stella.model :as model]
            [stella.qa.auto-close :as qa-auto-close]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]
           [javafx.scene.control Label]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(declare dispatch-map-event!)

(def ^:private diagram-sync-events
  #{events/edit-stock-apply
    events/edit-flow-apply
    events/edit-converter-apply})

(defn- on-fx-thread!
  [f]
  (if (Platform/isFxApplicationThread)
    (f)
    (Platform/runLater f)))

(defn- sync-simulation-time-display!
  [shell]
  (on-fx-thread!
   #(when-let [^Label label (fx-nodes/find-by-id-in-windows "simulation-time-display")]
      (.setText label (model/simulation-time-display shell)))))

(defn- sync-diagram-after-event!
  [etype shell]
  (when (or (dispatch/diagram-event? etype)
            (contains? diagram-sync-events etype))
    (fx-overlay/sync-diagram-overlay! (:diagram shell)))
  (when (= events/simulation-step etype)
    (sync-simulation-time-display! shell)))

(defn- show-edit-dialog!
  [state-atom dialog-state show!]
  (on-fx-thread!
   #(show! dialog-state
           (fn [dialog-event]
             (dispatch-map-event! dialog-event state-atom)))))

(defn- show-dialogs-after-event!
  [etype shell state-atom]
  (cond
    (and (= events/edit-stock-open etype) (:edit-stock shell))
    (show-edit-dialog! state-atom (:edit-stock shell) edit-stock-dialog/show!)

    (and (= events/edit-flow-open etype) (:edit-flow shell))
    (show-edit-dialog! state-atom (:edit-flow shell) edit-flow-dialog/show!)

    (and (= events/edit-converter-open etype) (:edit-converter shell))
    (show-edit-dialog! state-atom (:edit-converter shell) edit-converter-dialog/show!)))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (let [event (fx-input/enrich-event event)
         etype (dispatch/event-type event)
         effect (dispatch/event-effect event)]
     (let [shell (swap! state-atom dispatch/apply-event event)]
       (sync-diagram-after-event! etype shell)
       (show-dialogs-after-event! etype shell state-atom)
       (when effect (fx-effects/run-effect effect))))))

(defonce ^:private renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [shell] (root/root-desc shell)))
   :opts {:fx.opt/map-event-handler dispatch-map-event!}))

(defn start!
  []
  (reset! *state (cmd/default-shell! nil))
  (fx/mount-renderer *state renderer)
  (qa-auto-close/schedule-if-configured!))
