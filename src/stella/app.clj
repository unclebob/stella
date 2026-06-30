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
            [stella.thermometer :as thermometer]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]
           [javafx.scene Parent]
           [javafx.scene.control Label]
           [javafx.scene.shape Rectangle]))

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

(defn- upsert-stock-thermometer-fill!
  [^Parent canvas diagram name x y]
  (let [width (max 0 (get (thermometer/stock-thermometer diagram name) :fill-width 0))]
    (if-let [^Rectangle fill (fx-nodes/find-stock-thermometer-fill canvas name x y)]
      (do (.setWidth fill (double width))
          (.setVisible fill true)
          fill)
      (let [fill-id (str "stock-thermometer-fill-" name)
            ^Rectangle fill (Rectangle. (double width) (double thermometer/track-height))]
        (.setId fill fill-id)
        (.setLayoutX fill (+ x thermometer/track-x))
        (.setLayoutY fill (+ y thermometer/track-y))
        (.setStyle fill "-fx-fill: #add8e6;")
        (.setMouseTransparent fill true)
        (.setVisible fill true)
        (.add (.getChildren canvas) fill)
        fill))))

(defn- apply-stock-thermometer-fills!
  [shell]
  (when-let [^Parent canvas (fx-nodes/find-by-id-in-windows "canvas")]
    (let [diagram (:diagram shell)]
      (doseq [{:keys [name x y]} (model/stocks diagram)]
        (upsert-stock-thermometer-fill! canvas diagram name x y)))))

(defn- sync-stock-thermometer-fills!
  [shell]
  (on-fx-thread! #(apply-stock-thermometer-fills! shell)))

(def ^:private stock-value-label-x 24.0)

(defn- sync-stock-value-labels!
  [shell]
  (on-fx-thread!
   (when-let [^Parent canvas (fx-nodes/find-by-id-in-windows "canvas")]
     (let [diagram (:diagram shell)]
       (doseq [{:keys [name x y]} (model/stocks diagram)]
         (when-let [labels (thermometer/stock-canvas-labels diagram name)]
           (when-let [group (fx-nodes/find-stock-group-on-canvas canvas name x y)]
             (doseq [^Label label (.getChildrenUnmodifiable group)]
               (when (and (instance? Label label)
                          (= stock-value-label-x (.getLayoutX label)))
                 (.setText label (:value labels)))))))))))

(defn sync-ui-thermometer-fills!
  "Refreshes stock thermometer fill rectangles on the live canvas."
  []
  (on-fx-thread! #(apply-stock-thermometer-fills! @*state)))

(defn- sync-diagram-after-event!
  [etype shell]
  (when (or (dispatch/diagram-event? etype)
            (contains? diagram-sync-events etype))
    (fx-overlay/sync-diagram-overlay! (:diagram shell)))
  (when (= events/simulation-step etype)
    (sync-simulation-time-display! shell)
    (sync-stock-value-labels! shell))
  (when (or (= events/simulation-step etype)
            (contains? diagram-sync-events etype)
            (dispatch/diagram-event? etype))
    (sync-stock-thermometer-fills! shell)))

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
