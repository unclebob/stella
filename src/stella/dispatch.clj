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
   events/endpoint-click (fn [shell event]
                           (if (and (:endpoint-kind event) (:endpoint-name event))
                             (cmd/select-flow-endpoint-on-shell! shell
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
;; {:version 1, :tested-at "2026-06-26T15:43:26.892963-05:00", :module-hash "637684051", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-800377786"} {:id "defn/event-type", :kind "defn", :line 6, :end-line 8, :hash "-694825432"} {:id "defn/event->action", :kind "defn", :line 10, :end-line 12, :hash "-823897444"} {:id "defn-/placement-mode", :kind "defn-", :line 14, :end-line 16, :hash "1856580076"} {:id "defn-/place-on-canvas", :kind "defn-", :line 18, :end-line 24, :hash "-1648991308"} {:id "defn-/diagram-shell-updaters", :kind "defn-", :line 26, :end-line 45, :hash "-639996458"} {:id "defn/diagram-event?", :kind "defn", :line 47, :end-line 49, :hash "-1235679639"} {:id "defn/apply-action", :kind "defn", :line 51, :end-line 56, :hash "-632712785"} {:id "defn/apply-event", :kind "defn", :line 58, :end-line 65, :hash "1926107168"} {:id "defn/event-effect", :kind "defn", :line 67, :end-line 69, :hash "-850104102"} {:id "defn/process-event", :kind "defn", :line 71, :end-line 75, :hash "1193286310"}]}
;; clj-mutate-manifest-end
