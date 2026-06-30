(ns stella.qa.auto-close
  (:require [cljfx.api :as fx]
            [stella.fx.effects :as fx-effects]
            [stella.qa.args :as qa-args])
  (:import [java.util.concurrent Executors ScheduledExecutorService ThreadFactory
            TimeUnit]))

(defonce ^:private executor (atom nil))

(defn configured-seconds
  []
  (when-let [value (System/getProperty "stella.qa.auto-close-seconds")]
    (Integer/parseInt value)))

(defn shutdown-executor!
  []
  (when-let [^ScheduledExecutorService svc (deref executor)]
    (.shutdown svc)
    (reset! executor nil)))

(defn- daemon-thread-factory
  []
  (reify ThreadFactory
    (newThread [_ runnable]
      (doto (Thread. runnable "stella-qa-auto-close")
        (.setDaemon true)))))

(defn schedule-if-configured!
  []
  (when-let [seconds (configured-seconds)]
    (shutdown-executor!)
    (let [^ScheduledExecutorService svc (Executors/newScheduledThreadPool
                                         1 (daemon-thread-factory))
          task (fn []
                 (shutdown-executor!)
                 (fx/on-fx-thread
                  (System/setProperty "stella.qa.soft-exit" "true")
                  (fx-effects/run-effect :platform-exit)))]
      (reset! executor svc)
      (.schedule svc ^Runnable task (long seconds) TimeUnit/SECONDS))))

(defn force-exit!
  "Terminate the JVM during headed QA when a suite finishes without exiting."
  []
  (shutdown-executor!)
  (System/setProperty "stella.qa.soft-exit" "true")
  (fx-effects/run-effect :platform-exit))