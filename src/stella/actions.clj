(ns stella.actions
  (:require [stella.events :as events]))

(defn event->action
  "Maps an application event keyword to an action keyword, or nil when ignored."
  [event-type]
  (cond
    (= event-type events/quit) :quit
    (= event-type events/show-about) :show-about
    :else nil))

(defn action->effect
  "Maps an application action to an effect keyword executed on the JavaFX thread."
  [action]
  (cond
    (= action :quit) :platform-exit
    (= action :show-about) :about-dialog
    :else nil))