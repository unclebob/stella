(ns stella.actions
  (:require [stella.events :as events]))

(defn event->action
  "Maps an application event keyword to an action keyword, or nil when ignored."
  [event-type]
  (cond
    (= event-type events/quit) :quit
    (= event-type events/window-close) :window-close
    (= event-type events/show-about) :show-about
    :else nil))

(defn action->effect
  "Maps an application action to an effect keyword executed on the JavaFX thread."
  [action]
  (cond
    (or (= action :quit) (= action :window-close)) :platform-exit
    (= action :show-about) :about-dialog
    :else nil))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:39:41.700918-05:00", :module-hash "-211100532", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-2002190841"} {:id "defn/event->action", :kind "defn", :line 4, :end-line 10, :hash "1106798315"} {:id "defn/action->effect", :kind "defn", :line 12, :end-line 18, :hash "772225751"}]}
;; clj-mutate-manifest-end
