(ns stella.fx.input
  (:require [stella.events :as events]
            [stella.fx.nodes :as fx-nodes])
  (:import [javafx.scene.control TextField]
           [javafx.scene.input MouseEvent]))

(defn- event-type
  [event]
  (or (:event event) (:event/type event)))

(defn click-coordinates
  "Extracts canvas click coordinates from a CljFX mouse event, or nil when absent."
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getX mouse)) (int (.getY mouse))]))

(defn- scene-coordinates
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getSceneX mouse)) (int (.getSceneY mouse))]))

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

(def ^:private drag-events
  #{events/stock-drag-start events/stock-drag-end
    events/converter-drag-start events/converter-drag-end})

(def ^:private edit-draft-readers
  {events/edit-stock-apply read-edit-stock-draft
   events/edit-flow-apply read-edit-flow-draft
   events/edit-converter-apply read-edit-converter-draft})

(defn- enrich-canvas-click
  [event]
  (assoc event :coordinates (click-coordinates event)))

(defn- enrich-drag-event
  [event]
  (cond-> event
    (not (:scene-coordinates event))
    (assoc :scene-coordinates (scene-coordinates event))
    (and (:from-canvas event) (not (:canvas-coordinates event)))
    (assoc :canvas-coordinates (click-coordinates event))))

(defn- enrich-edit-apply
  [event draft-reader]
  (if (:draft event)
    event
    (assoc event :draft (draft-reader))))

(defn enrich-event
  "Adds derived fields to platform events before dispatch."
  [event]
  (let [etype (event-type event)]
    (cond
      (= events/canvas-click etype) (enrich-canvas-click event)
      (contains? drag-events etype) (enrich-drag-event event)
      (contains? edit-draft-readers etype) (enrich-edit-apply event (get edit-draft-readers etype))
      :else event)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:38:37.628728-05:00", :module-hash "-366939010", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "157018194"} {:id "defn-/event-type", :kind "defn-", :line 5, :end-line 7, :hash "-876440962"} {:id "defn/click-coordinates", :kind "defn", :line 9, :end-line 13, :hash "601074763"} {:id "defn/enrich-event", :kind "defn", :line 15, :end-line 20, :hash "1062960074"}]}
;; clj-mutate-manifest-end
