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

(defn enrich-event
  "Adds derived fields to platform events before dispatch."
  [event]
  (cond
    (= events/canvas-click (event-type event))
    (assoc event :coordinates (click-coordinates event))

    (and (= events/edit-stock-apply (event-type event))
         (not (:draft event)))
    (assoc event :draft (read-edit-stock-draft))

    :else event))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:38:37.628728-05:00", :module-hash "-366939010", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "157018194"} {:id "defn-/event-type", :kind "defn-", :line 5, :end-line 7, :hash "-876440962"} {:id "defn/click-coordinates", :kind "defn", :line 9, :end-line 13, :hash "601074763"} {:id "defn/enrich-event", :kind "defn", :line 15, :end-line 20, :hash "1062960074"}]}
;; clj-mutate-manifest-end
