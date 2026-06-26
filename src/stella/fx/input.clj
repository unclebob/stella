(ns stella.fx.input
  (:require [stella.events :as events])
  (:import [javafx.scene.input MouseEvent]))

(defn- event-type
  [event]
  (or (:event event) (:event/type event)))

(defn click-coordinates
  "Extracts canvas click coordinates from a CljFX mouse event, or nil when absent."
  [event]
  (when-let [^MouseEvent mouse (:fx/event event)]
    [(int (.getX mouse)) (int (.getY mouse))]))

(defn enrich-event
  "Adds platform-neutral coordinates to canvas click events."
  [event]
  (if (= events/canvas-click (event-type event))
    (assoc event :coordinates (click-coordinates event))
    event))