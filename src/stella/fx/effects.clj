(ns stella.fx.effects
  (:require [stella.fx.nodes :as fx-nodes])
  (:import [javafx.animation ScaleTransition]
           [javafx.application Platform]
           [javafx.scene.control Alert Alert$AlertType]
           [javafx.util Duration]))

(defn- about-dialog []
  (doto (Alert. Alert$AlertType/INFORMATION)
    (.setTitle "About Stella")
    (.setHeaderText "Stella")
    (.setContentText "A system dynamics diagram editor.")
    .show))

(defn- platform-exit! []
  (if (= "true" (System/getProperty "stella.qa.soft-exit"))
    (System/exit 0)
    (Platform/runLater #(Platform/exit))))

(defn- run-later-twice
  [f]
  (Platform/runLater #(Platform/runLater f)))

(defn pulse-node!
  [id]
  (try
    (run-later-twice
     (fn []
       (when-let [node (fx-nodes/find-by-id-in-windows id)]
         (doto (ScaleTransition. (Duration/millis 90) node)
           (.setFromX 1.0)
           (.setFromY 1.0)
           (.setToX 1.08)
           (.setToY 1.08)
           (.setAutoReverse true)
           (.setCycleCount 2)
           .play))))
    (catch IllegalStateException _ nil)))

(def ^:private effect-runners
  {:platform-exit platform-exit!
   :about-dialog about-dialog})

(defn run-effect
  "Runs a platform effect keyword on the JavaFX thread, or does nothing when unknown."
  [effect]
  (when-let [run (get effect-runners effect)]
    (run)))
