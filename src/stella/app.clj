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
            [stella.fx.overlay :as fx-overlay]
            [stella.qa.auto-close :as qa-auto-close]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (let [event (fx-input/enrich-event event)
         etype (dispatch/event-type event)
         effect (dispatch/event-effect event)]
     (let [shell (swap! state-atom dispatch/apply-event event)]
       (when (or (dispatch/diagram-event? etype)
                 (= events/edit-stock-apply etype)
                 (= events/edit-flow-apply etype)
                 (= events/edit-converter-apply etype))
         (fx-overlay/sync-diagram-overlay! (:diagram shell)))
       (when (and (= events/edit-stock-open etype) (:edit-stock shell))
         (let [show! (fn []
                       (edit-stock-dialog/show! (:edit-stock shell)
                                                (fn [dialog-event]
                                                  (dispatch-map-event! dialog-event state-atom))))]
           (if (Platform/isFxApplicationThread)
             (show!)
             (Platform/runLater show!))))
       (when (and (= events/edit-flow-open etype) (:edit-flow shell))
         (let [show! (fn []
                       (edit-flow-dialog/show! (:edit-flow shell)
                                               (fn [dialog-event]
                                                 (dispatch-map-event! dialog-event state-atom))))]
           (if (Platform/isFxApplicationThread)
             (show!)
             (Platform/runLater show!))))
       (when (and (= events/edit-converter-open etype) (:edit-converter shell))
         (let [show! (fn []
                       (edit-converter-dialog/show! (:edit-converter shell)
                                                      (fn [dialog-event]
                                                        (dispatch-map-event! dialog-event state-atom))))]
           (if (Platform/isFxApplicationThread)
             (show!)
             (Platform/runLater show!))))
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