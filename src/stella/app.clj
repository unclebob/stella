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
            [stella.qa.args :as qa-args]
            [stella.qa.auto-close :as qa-auto-close]
            [stella.ui.root :as root])
  (:import [javafx.application Platform]
           [javafx.event ActionEvent]
           [javafx.scene.input KeyEvent MouseEvent MouseButton]))

(defonce *state
  (atom (cmd/default-shell! nil)))

(def ^:private debug-seen-limit 128)

(defonce ^:private *debug-seen-events
  (atom []))

(defn- node-path [^javafx.scene.Node node]
  (when node
    (loop [n node
           path []]
      (if n
        (let [id (.getId n)
              text (when (instance? javafx.scene.control.Labeled n)
                     (.getText ^javafx.scene.control.Labeled n))
              cls (.getSimpleName (.getClass n))
              label (cond
                      id (str cls "#" id)
                      text (str cls "[" text "]")
                      :else cls)]
          (recur (.getParent n) (conj path label)))
        (reverse path)))))

(declare present-text)

(defn- debug-event-key [event raw-event]
  (cond
    (instance? MouseEvent raw-event)
    (let [^MouseEvent me raw-event]
      [(dispatch/event-type event)
       (class raw-event)
       (str (.getEventType me))
       (str (.getButton me))
       (Math/round (.getScreenX me))
       (Math/round (.getScreenY me))])

    (instance? KeyEvent raw-event)
    (let [^KeyEvent ke raw-event]
      [(dispatch/event-type event)
       (class raw-event)
       (str (.getEventType ke))
       (str (.getCode ke))
       (present-text (.getCharacter ke))
       (present-text (.getText ke))])

    (instance? ActionEvent raw-event)
    [(dispatch/event-type event)
     (class raw-event)
     (System/identityHashCode raw-event)]

    :else
    [(dispatch/event-type event) (class raw-event)]))

(defn- first-debug-observation?
  [event raw-event]
  (when raw-event
    (let [event-key (debug-event-key event raw-event)]
      (loop []
        (let [seen @*debug-seen-events]
          (if (some #{event-key} seen)
            false
            (if (compare-and-set! *debug-seen-events
                                  seen
                                  (->> (conj seen event-key)
                                       (take-last debug-seen-limit)
                                       vec))
              true
              (recur))))))))

(defn- mouse-button-name [^MouseEvent event]
  (cond
    (= (.getButton event) MouseButton/PRIMARY) "left"
    (= (.getButton event) MouseButton/SECONDARY) "right"
    (= (.getButton event) MouseButton/MIDDLE) "middle"
    :else (str (.getButton event))))

(defn- mouse-action-name [^MouseEvent event]
  (let [event-type (.getEventType event)]
    (cond
      (= event-type MouseEvent/MOUSE_PRESSED) "down"
      (= event-type MouseEvent/MOUSE_RELEASED) "up"
      (= event-type MouseEvent/MOUSE_CLICKED) "click"
      :else (str event-type))))

(defn- present-text [value]
  (let [text (str value)]
    (if (seq text)
      (pr-str text)
      "\"\"")))

(defn- debug-event-name [event]
  (or (some-> event dispatch/event-type str)
      "raw"))

(defn- diagram-object-target?
  [path]
  (boolean
   (some #(re-find #"^Group#(stock|source|sink|converter|flow|connector)-" %)
         path)))

(defn- canvas-fallback-event?
  [event]
  (contains? #{events/canvas-click
               events/marquee-drag-start
               events/marquee-drag-end}
             (dispatch/event-type event)))

(defn- skip-debug-event?
  [event raw-event]
  (and (instance? MouseEvent raw-event)
       (or (= events/canvas-move (dispatch/event-type event))
           (and (canvas-fallback-event? event)
                (let [target (.getTarget ^MouseEvent raw-event)
                      path (node-path (if (instance? javafx.scene.Node target) target nil))]
                  (diagram-object-target? path))))))

(defn- debug-log-event [event]
  (when (qa-args/debug?)
    (let [raw-event (:fx/event event)
          event-name (debug-event-name event)]
      (cond
        (skip-debug-event? event raw-event)
        nil

        (instance? MouseEvent raw-event)
        (when (first-debug-observation? event raw-event)
        (let [^MouseEvent me raw-event
              target (.getTarget me)
              path (node-path (if (instance? javafx.scene.Node target) target nil))]
          (println (format (str "[DEBUG mouse] event=%s %s %s scene=(%.0f,%.0f) "
                                "screen=(%.0f,%.0f) local=(%.0f,%.0f) target-path: %s")
                           event-name
                           (mouse-button-name me)
                           (mouse-action-name me)
                           (.getSceneX me)
                           (.getSceneY me)
                           (.getScreenX me)
                           (.getScreenY me)
                           (.getX me)
                           (.getY me)
                           (vec path)))))

        (instance? KeyEvent raw-event)
        (when (first-debug-observation? event raw-event)
        (let [^KeyEvent ke raw-event]
          (println (format "[DEBUG key] event=%s %s code=%s char=%s text=%s"
                           event-name
                           (.getEventType ke)
                           (.getCode ke)
                           (present-text (.getCharacter ke))
                           (present-text (.getText ke))))))

        (instance? ActionEvent raw-event)
        (when (first-debug-observation? event raw-event)
        (let [^ActionEvent ae raw-event
              src (.getSource ae)
              text (cond
                     (instance? javafx.scene.control.Button src)
                     (.getText ^javafx.scene.control.Button src)
                     :else (str src))]
          (println (format "[DEBUG action] event=%s from %s"
                           event-name
                           (present-text text)))))

        :else nil))))

(defn- placed-object-animation-id
  [before-shell after-shell event]
  (when (= events/canvas-click (dispatch/event-type event))
    (let [before-diagram (:diagram before-shell)
          after-diagram (:diagram after-shell)]
      (case (:placement-mode before-diagram)
        :stock (when (< (:next-stock-num before-diagram)
                        (:next-stock-num after-diagram))
                 (str "stock-Stock" (dec (:next-stock-num after-diagram))))
        :source (when (< (:next-source-num before-diagram)
                         (:next-source-num after-diagram))
                  (str "source-Source" (dec (:next-source-num after-diagram))))
        :sink (when (< (:next-sink-num before-diagram)
                       (:next-sink-num after-diagram))
                (str "sink-Sink" (dec (:next-sink-num after-diagram))))
        :converter (when (< (:next-converter-num before-diagram)
                            (:next-converter-num after-diagram))
                     (str "converter-Converter" (dec (:next-converter-num after-diagram))))
        nil))))

(defn- dropped-object-animation-id
  [before-shell event]
  (case (dispatch/event-type event)
    events/stock-drag-end
    (when-let [name (get-in before-shell [:stock-drag :name])]
      (str "stock-" name))

    events/converter-drag-end
    (when-let [name (get-in before-shell [:converter-drag :name])]
      (str "converter-" name))

    events/cloud-drag-end
    (when-let [{:keys [kind name]} (:cloud-drag before-shell)]
      (str (clojure.core/name kind) "-" name))

    nil))

(defn- animation-target-id
  [before-shell after-shell event]
  (or (placed-object-animation-id before-shell after-shell event)
      (dropped-object-animation-id before-shell event)))

(defn dispatch-map-event!
  ([event] (dispatch-map-event! event *state))
  ([event state-atom]
   (debug-log-event event)
   (let [event (fx-input/enrich-event event)
         etype (dispatch/event-type event)
         effect (dispatch/event-effect event)
         before-shell @state-atom]
     (let [shell (swap! state-atom dispatch/apply-event event)]
       (when-let [id (animation-target-id before-shell shell event)]
         (fx-effects/pulse-node! id))
       (when (or (and (dispatch/diagram-event? etype)
                      (not= events/canvas-move etype))
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
