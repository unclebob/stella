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

(defn- diagram-shell-updaters
  []
  {events/arm-stock (fn [shell _]
                      (cmd/arm-stock-placement-on-shell! shell))
   events/arm-flow (fn [shell _]
                     (cmd/arm-flow-placement-on-shell! shell))
   events/stock-click (fn [shell event]
                        (if-let [stock-name (:stock-name event)]
                          (cmd/select-flow-stock-on-shell! shell stock-name)
                          shell))
   events/canvas-click (fn [shell event]
                         (if-let [[x y] (:coordinates event)]
                           (cmd/place-stock-on-shell! shell x y)
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