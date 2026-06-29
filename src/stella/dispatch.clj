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
    (if (= :idle (placement-mode shell))
      (cmd/click-select-at-on-shell! shell x y)
      shell)))

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
                           shell))
   events/canvas-move (fn [shell event]
                        (if-let [coordinates (:coordinates event)]
                          (cmd/update-canvas-preview-on-shell! shell coordinates)
                          shell))
   events/edit-stock-open (fn [shell event]
                            (if (:stock-name event)
                              (cmd/open-edit-stock-on-shell! shell (:stock-name event))
                              shell))
   events/edit-stock-apply (fn [shell event]
                             (if (:draft event)
                               (cmd/apply-edit-stock-on-shell! shell (:draft event))
                               shell))
   events/edit-stock-cancel (fn [shell _]
                              (cmd/cancel-edit-stock-on-shell! shell))
   events/edit-flow-open (fn [shell event]
                           (if (:flow-name event)
                             (cmd/open-edit-flow-on-shell! shell (:flow-name event))
                             shell))
   events/edit-flow-apply (fn [shell event]
                            (if (:draft event)
                              (cmd/apply-edit-flow-on-shell! shell (:draft event))
                              shell))
   events/edit-flow-cancel (fn [shell _]
                             (cmd/cancel-edit-flow-on-shell! shell))
   events/edit-converter-open (fn [shell event]
                                (if (:converter-name event)
                                  (cmd/open-edit-converter-on-shell! shell (:converter-name event))
                                  shell))
   events/edit-converter-apply (fn [shell event]
                                 (if (:draft event)
                                   (cmd/apply-edit-converter-on-shell! shell (:draft event))
                                   shell))
   events/edit-converter-cancel (fn [shell _]
                                  (cmd/cancel-edit-converter-on-shell! shell))
   events/stock-drag-start (fn [shell event]
                            (let [shell (cmd/start-stock-drag-on-shell! shell event)]
                              (if (:stock-drag shell)
                                shell
                                (cmd/start-converter-drag-on-shell! shell event))))
   events/stock-drag (fn [shell event]
                       (cond
                         (:stock-drag shell) (cmd/drag-stock-on-shell! shell event)
                         (:converter-drag shell) (cmd/drag-converter-on-shell! shell event)
                         :else shell))
   events/stock-drag-end (fn [shell event]
                           (cond
                             (:stock-drag shell) (cmd/end-stock-drag-on-shell! shell event)
                             (:converter-drag shell) (cmd/end-converter-drag-on-shell! shell event)
                             :else shell))
   events/converter-drag-start (fn [shell event]
                                 (cmd/start-converter-drag-on-shell! shell event))
   events/converter-drag (fn [shell event]
                           (cmd/drag-converter-on-shell! shell event))
   events/converter-drag-end (fn [shell event]
                               (cmd/end-converter-drag-on-shell! shell event))
   events/cloud-drag-start (fn [shell event]
                             (cmd/start-cloud-drag-on-shell! shell event))
   events/cloud-drag (fn [shell event]
                       (cmd/drag-cloud-on-shell! shell event))
   events/cloud-drag-end (fn [shell event]
                           (cmd/end-cloud-drag-on-shell! shell event))
   events/connector-control-drag-start (fn [shell event]
                                         (cmd/start-connector-control-drag-on-shell! shell event))
   events/connector-control-drag (fn [shell event]
                                   (cmd/drag-connector-control-on-shell! shell event))
   events/connector-control-drag-end (fn [shell event]
                                       (cmd/end-connector-control-drag-on-shell! shell event))
   events/selection-click (fn [shell event]
                            (if-let [[x y] (:canvas-coordinates event)]
                              (if (:shift-key event)
                                (cmd/shift-click-select-at-on-shell! shell x y)
                                (cmd/click-select-at-on-shell! shell x y))
                              (if (:shift-key event)
                                (cmd/shift-click-select-on-shell! shell
                                                                  (:object-kind event)
                                                                  (:object-name event))
                                (cmd/click-select-on-shell! shell
                                                            (:object-kind event)
                                                            (:object-name event)))))
   events/marquee-drag-start (fn [shell event]
                               (cmd/start-marquee-drag-on-shell! shell event))
   events/marquee-drag (fn [shell event]
                         (cmd/drag-marquee-on-shell! shell event))
   events/marquee-drag-end (fn [shell event]
                             (cmd/end-marquee-drag-on-shell! shell event))
   events/clear-selection (fn [shell event]
                            (if (= :Esc (:key-code event))
                              (cmd/cancel-on-escape-on-shell! shell)
                              shell))
   events/scene-key-pressed (fn [shell event]
                              (cond
                                (= :Esc (:key-code event))
                                (cmd/cancel-on-escape-on-shell! shell)

                                (#{:Delete :Backspace} (:key-code event))
                                (if (and (= :idle (get-in shell [:diagram :placement-mode]))
                                         (not (:edit-stock shell))
                                         (not (:edit-flow shell))
                                         (not (:edit-converter shell)))
                                  (cmd/delete-selection-on-shell! shell)
                                  shell)

                                :else shell))})

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
