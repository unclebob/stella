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

(defn- place-on-canvas
  [shell [x y]]
  (case (placement-mode shell)
    :stock (cmd/place-stock-on-shell! shell x y)
    :source (cmd/place-source-on-shell! shell x y)
    :sink (cmd/place-sink-on-shell! shell x y)
    :converter (cmd/place-converter-on-shell! shell x y)
    shell))

(defn- diagram-shell-updaters
  []
  {events/arm-stock (fn [shell _]
                      (cmd/arm-stock-placement-on-shell! shell))
   events/arm-flow (fn [shell _]
                     (cmd/arm-flow-placement-on-shell! shell))
   events/arm-source (fn [shell _]
                       (cmd/arm-source-placement-on-shell! shell))
   events/arm-sink (fn [shell _]
                     (cmd/arm-sink-placement-on-shell! shell))
   events/arm-converter (fn [shell _]
                          (cmd/arm-converter-placement-on-shell! shell))
   events/arm-connector (fn [shell _]
                          (cmd/arm-connector-placement-on-shell! shell))
   events/endpoint-click (fn [shell event]
                           (if (and (:endpoint-kind event) (:endpoint-name event))
                             (cmd/select-endpoint-on-shell! shell
                                                            (:endpoint-kind event)
                                                            (:endpoint-name event))
                             shell))
   events/canvas-click (fn [shell event]
                         (if-let [[x y] (:coordinates event)]
                           (place-on-canvas shell [x y])
                           shell))})

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
;; {:version 1, :tested-at "2026-06-26T15:24:17.91274-05:00", :module-hash "341913461", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-2009338086"} {:id "defn/event-type", :kind "defn", :line 5, :end-line 7, :hash "-694825432"} {:id "defn/event->action", :kind "defn", :line 9, :end-line 11, :hash "-823897444"} {:id "defn/apply-action", :kind "defn", :line 13, :end-line 18, :hash "-632712785"} {:id "defn/process-event", :kind "defn", :line 20, :end-line 24, :hash "1193286310"}]}
;; clj-mutate-manifest-end
