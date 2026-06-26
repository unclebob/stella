(ns stella.main
  (:require [cljfx.api :as fx]
            [stella.app :as app])
  (:gen-class))

(defn -main
  [& _]
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (System/setProperty "apple.awt.application.name" "Stella")
  (fx/on-fx-thread
   (app/start!)))