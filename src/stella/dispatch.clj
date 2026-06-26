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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:24:17.91274-05:00", :module-hash "341913461", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-2009338086"} {:id "defn/event-type", :kind "defn", :line 5, :end-line 7, :hash "-694825432"} {:id "defn/event->action", :kind "defn", :line 9, :end-line 11, :hash "-823897444"} {:id "defn/apply-action", :kind "defn", :line 13, :end-line 18, :hash "-632712785"} {:id "defn/process-event", :kind "defn", :line 20, :end-line 24, :hash "1193286310"}]}
;; clj-mutate-manifest-end
