(ns stella.ui.root
  (:require [stella.events :as events]
            [stella.ui.canvas :as canvas]
            [stella.ui.menu :as menu]))

(defn root-desc
  []
  {:fx/type :stage
   :title "Stella"
   :showing true
   :width 1024
   :height 768
   :min-width 640
   :min-height 480
   :on-close-request {:event events/quit}
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top (menu/menu-bar-desc)
                  :center (canvas/canvas-desc)}}})