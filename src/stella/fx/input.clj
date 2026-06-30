(ns stella.fx.input
  (:require [stella.events :as events]
            [stella.fx.nodes :as fx-nodes])
  (:import [javafx.scene Node]
           [javafx.scene.control TextField]
           [javafx.scene.input KeyEvent MouseEvent]))

(defn- event-type
  [event]
  (or (:event event) (:event/type event)))

(defn click-coordinates
  "Extracts canvas click coordinates from a CljFX mouse event, or nil when absent."
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getX mouse)) (int (.getY mouse))]))

(defn- canvas-coordinates
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    (when-let [^Node canvas (fx-nodes/find-by-id-in-windows "canvas")]
      (let [point (.sceneToLocal canvas (.getSceneX mouse) (.getSceneY mouse))]
        [(int (.getX point)) (int (.getY point))]))))

(defn- scene-coordinates
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getSceneX mouse)) (int (.getSceneY mouse))]))

(defn- shift-down?
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    (.isShiftDown mouse)))

(defn- field-text
  [id]
  (when-let [^TextField field (fx-nodes/find-by-id-in-windows id)]
    (.getText field)))

(defn- read-edit-stock-draft
  []
  {:name (field-text "edit-stock-name")
   :initial-value (field-text "edit-stock-initial")
   :min-value (field-text "edit-stock-min")
   :max-value (field-text "edit-stock-max")})

(defn- read-named-field-draft
  [name-id value-key value-id]
  {:name (field-text name-id)
   value-key (field-text value-id)})

(defn- read-edit-flow-draft
  []
  (read-named-field-draft "edit-flow-name" :rate "edit-flow-rate"))

(defn- read-edit-converter-draft
  []
  (read-named-field-draft "edit-converter-name" :formula "edit-converter-formula"))

(defn enrich-event
  "Adds derived fields to platform events before dispatch."
  [event]
  (cond
    (#{events/canvas-click events/canvas-move} (event-type event))
    (assoc event :coordinates (or (:coordinates event)
                                  (canvas-coordinates event)
                                  (click-coordinates event)))

    (#{events/stock-drag-start events/stock-drag events/stock-drag-end
       events/converter-drag-start events/converter-drag events/converter-drag-end
       events/cloud-drag-start events/cloud-drag events/cloud-drag-end
       events/connector-control-drag-start
       events/connector-control-drag
       events/connector-control-drag-end} (event-type event))
    (cond-> event
      (not (:scene-coordinates event))
      (assoc :scene-coordinates (scene-coordinates event))
      (nil? (:canvas-coordinates event))
      (assoc :canvas-coordinates (canvas-coordinates event)))

    (= events/selection-click (event-type event))
    (cond-> event
      (nil? (:shift-key event))
      (assoc :shift-key (boolean (shift-down? event)))
      (nil? (:canvas-coordinates event))
      (assoc :canvas-coordinates (canvas-coordinates event)))

    (#{events/marquee-drag-start events/marquee-drag events/marquee-drag-end} (event-type event))
    (cond-> event
      (and (:from-canvas event) (not (:canvas-coordinates event)))
      (assoc :canvas-coordinates (or (canvas-coordinates event)
                                     (click-coordinates event))))

    (#{events/clear-selection events/scene-key-pressed} (event-type event))
    (if-let [^KeyEvent key (:fx/event event)]
      (assoc event :key-code (keyword (.getName (.getCode key))))
      event)

    (and (= events/edit-stock-apply (event-type event))
         (not (:draft event)))
    (assoc event :draft (read-edit-stock-draft))

    (and (= events/edit-flow-apply (event-type event))
         (not (:draft event)))
    (assoc event :draft (read-edit-flow-draft))

    (and (= events/edit-converter-apply (event-type event))
         (not (:draft event)))
    (assoc event :draft (read-edit-converter-draft))

    :else event))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:38:37.628728-05:00", :module-hash "-366939010", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "157018194"} {:id "defn-/event-type", :kind "defn-", :line 5, :end-line 7, :hash "-876440962"} {:id "defn/click-coordinates", :kind "defn", :line 9, :end-line 13, :hash "601074763"} {:id "defn/enrich-event", :kind "defn", :line 15, :end-line 20, :hash "1062960074"}]}
;; clj-mutate-manifest-end
