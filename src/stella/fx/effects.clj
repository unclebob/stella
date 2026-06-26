(ns stella.fx.effects
  (:import [javafx.application Platform]
           [javafx.scene.control Alert Alert$AlertType]))

(defn- about-dialog []
  (doto (Alert. Alert$AlertType/INFORMATION)
    (.setTitle "About Stella")
    (.setHeaderText "Stella")
    (.setContentText "A system dynamics diagram editor.")
    .show))

(defn- platform-exit! []
  (if (= "true" (System/getProperty "stella.qa.soft-exit"))
    (System/exit 0)
    (Platform/exit)))

(def ^:private effect-runners
  {:platform-exit platform-exit!
   :about-dialog about-dialog})

(defn run-effect
  "Runs a platform effect keyword on the JavaFX thread, or does nothing when unknown."
  [effect]
  (when-let [run (get effect-runners effect)]
    (run)))