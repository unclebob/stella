(ns stella.dispatch
  (:require [stella.actions :as actions]
            [stella.commands :as cmd]))

(defn event-type
  [event]
  (or (:event event) (:event/type event)))

(defn event->action
  [event]
  (some-> event event-type actions/event->action))

(defn apply-action
  [shell action]
  (cond
    (= action :quit) (cmd/quit! shell)
    (= action :show-about) (cmd/show-about! shell)
    :else shell))

(defn process-event
  [event]
  (when-let [action (event->action event)]
    {:action action
     :effect (actions/action->effect action)}))