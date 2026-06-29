(ns stella.dispatch
  (:require [stella.actions :as actions]
            [stella.commands :as cmd]
            [stella.events :as events]))

(defn event-type
  [event]
  (or (:event event) (:event/type event)))

(defn event->action
  [event]
  (some-> event event-type actions/event->action))

(defn- placement-mode
  [shell]
  (get-in shell [:diagram :placement-mode]))

(def ^:private canvas-place-commands
  {:stock cmd/place-stock-on-shell!
   :source cmd/place-source-on-shell!
   :sink cmd/place-sink-on-shell!
   :converter cmd/place-converter-on-shell!})

(defn- place-on-canvas
  [shell [x y]]
  (if-let [place! (get canvas-place-commands (placement-mode shell))]
    (place! shell x y)
    shell))

(defn- arm-stock [shell _]
  (cmd/arm-stock-placement-on-shell! shell))

(defn- arm-flow [shell _]
  (cmd/arm-flow-placement-on-shell! shell))

(defn- arm-source [shell _]
  (cmd/arm-source-placement-on-shell! shell))

(defn- arm-sink [shell _]
  (cmd/arm-sink-placement-on-shell! shell))

(defn- arm-converter [shell _]
  (cmd/arm-converter-placement-on-shell! shell))

(defn- arm-connector [shell _]
  (cmd/arm-connector-placement-on-shell! shell))

(defn- endpoint-click [shell event]
  (if (and (:endpoint-kind event) (:endpoint-name event))
    (cmd/select-endpoint-on-shell! shell
                                   (:endpoint-kind event)
                                   (:endpoint-name event))
    shell))

(defn- canvas-click [shell event]
  (if-let [[x y] (:coordinates event)]
    (place-on-canvas shell [x y])
    shell))

(defn- open-stock-editor [shell event]
  (if (:stock-name event)
    (cmd/open-edit-stock-on-shell! shell (:stock-name event))
    shell))

(defn- apply-stock-editor [shell event]
  (if (:draft event)
    (cmd/apply-edit-stock-on-shell! shell (:draft event))
    shell))

(defn- open-flow-editor [shell event]
  (if (:flow-name event)
    (cmd/open-edit-flow-on-shell! shell (:flow-name event))
    shell))

(defn- apply-flow-editor [shell event]
  (if (:draft event)
    (cmd/apply-edit-flow-on-shell! shell (:draft event))
    shell))

(defn- open-converter-editor [shell event]
  (if (:converter-name event)
    (cmd/open-edit-converter-on-shell! shell (:converter-name event))
    shell))

(defn- apply-converter-editor [shell event]
  (if (:draft event)
    (cmd/apply-edit-converter-on-shell! shell (:draft event))
    shell))

(defn- start-object-drag [shell event]
  (let [shell (cmd/start-stock-drag-on-shell! shell event)]
    (if (:stock-drag shell)
      shell
      (cmd/start-converter-drag-on-shell! shell event))))

(defn- end-object-drag [shell event]
  (cond
    (:stock-drag shell) (cmd/end-stock-drag-on-shell! shell event)
    (:converter-drag shell) (cmd/end-converter-drag-on-shell! shell event)
    :else shell))

(defn- diagram-shell-updaters
  []
  {events/arm-stock arm-stock
   events/arm-flow arm-flow
   events/arm-source arm-source
   events/arm-sink arm-sink
   events/arm-converter arm-converter
   events/arm-connector arm-connector
   events/endpoint-click endpoint-click
   events/canvas-click canvas-click
   events/edit-stock-open open-stock-editor
   events/edit-stock-apply apply-stock-editor
   events/edit-stock-cancel (fn [shell _] (cmd/cancel-edit-stock-on-shell! shell))
   events/edit-flow-open open-flow-editor
   events/edit-flow-apply apply-flow-editor
   events/edit-flow-cancel (fn [shell _] (cmd/cancel-edit-flow-on-shell! shell))
   events/edit-converter-open open-converter-editor
   events/edit-converter-apply apply-converter-editor
   events/edit-converter-cancel (fn [shell _] (cmd/cancel-edit-converter-on-shell! shell))
   events/stock-drag-start start-object-drag
   events/stock-drag-end end-object-drag
   events/converter-drag-start (fn [shell event] (cmd/start-converter-drag-on-shell! shell event))
   events/converter-drag-end (fn [shell event] (cmd/end-converter-drag-on-shell! shell event))})

(defn diagram-event?
  [event-type]
  (contains? (diagram-shell-updaters) event-type))

(defn apply-action
  [shell action]
  (cond
    (= action :quit) (cmd/quit! shell)
    (= action :show-about) (cmd/show-about! shell)
    :else shell))

(defn apply-event
  [shell event]
  (let [etype (event-type event)]
    (if-let [updater (get (diagram-shell-updaters) etype)]
      (updater shell event)
      (if-let [action (actions/event->action etype)]
        (apply-action shell action)
        shell))))

(defn event-effect
  [event]
  (some-> event event-type actions/event->action actions/action->effect))

(defn process-event
  [event]
  (when-let [action (event->action event)]
    {:action action
     :effect (actions/action->effect action)}))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T18:02:12.099929-05:00", :module-hash "-910800042", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-800377786"} {:id "defn/event-type", :kind "defn", :line 6, :end-line 8, :hash "-694825432"} {:id "defn/event->action", :kind "defn", :line 10, :end-line 12, :hash "-823897444"} {:id "defn-/placement-mode", :kind "defn-", :line 14, :end-line 16, :hash "1856580076"} {:id "def/canvas-place-commands", :kind "def", :line 18, :end-line 22, :hash "-729914663"} {:id "defn-/place-on-canvas", :kind "defn-", :line 24, :end-line 28, :hash "717815547"} {:id "defn-/diagram-shell-updaters", :kind "defn-", :line 30, :end-line 53, :hash "-1766390950"} {:id "defn/diagram-event?", :kind "defn", :line 55, :end-line 57, :hash "-1235679639"} {:id "defn/apply-action", :kind "defn", :line 59, :end-line 64, :hash "-632712785"} {:id "defn/apply-event", :kind "defn", :line 66, :end-line 73, :hash "1926107168"} {:id "defn/event-effect", :kind "defn", :line 75, :end-line 77, :hash "-850104102"} {:id "defn/process-event", :kind "defn", :line 79, :end-line 83, :hash "1193286310"}]}
;; clj-mutate-manifest-end
