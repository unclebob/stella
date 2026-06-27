(ns stella.main
  (:require [cljfx.api :as fx]
            [stella.app :as app]
            [stella.qa.args :as qa-args])
  (:gen-class))

(defn -main
  [& args]
  (qa-args/apply-qa-flag! args)
  (System/setProperty "apple.laf.useScreenMenuBar" "true")
  (System/setProperty "apple.awt.application.name" "Stella")
  (fx/on-fx-thread
   (app/start!)))