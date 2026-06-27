(ns stella.qa.auto-close
  (:require [cljfx.api :as fx]
            [stella.fx.effects :as fx-effects])
  (:import [java.util.concurrent Executors ScheduledExecutorService TimeUnit]
           [javafx.stage Stage Window]))

(defn configured-seconds
  []
  (when-let [value (System/getProperty "stella.qa.auto-close-seconds")]
    (Integer/parseInt value)))

(defn schedule-if-configured!
  []
  (when-let [seconds (configured-seconds)]
    (let [^ScheduledExecutorService executor (Executors/newSingleThreadScheduledExecutor)
          task (fn []
                 (fx/on-fx-thread
                  (doseq [^Window w (Window/getWindows)]
                    (when (instance? Stage w)
                      (.hide ^Stage w)))
                  (System/setProperty "stella.qa.soft-exit" "true")
                  (fx-effects/run-effect :platform-exit)))]
      (.schedule executor ^Runnable task (long seconds) TimeUnit/SECONDS))))