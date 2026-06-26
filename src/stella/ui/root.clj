(ns stella.ui.root
  (:require [stella.ui.menu :as menu]
            [stella.ui.canvas :as canvas]))

(defn root-desc
  [shell]
  {:fx/type :stage
   :title (:window-title shell)
   :showing (:showing shell)
   :width 1024
   :height 768
   :min-width 640
   :min-height 480
   :on-close-request {:event :stella.app/quit}
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top (menu/menu-bar-desc shell)
                  :center (canvas/canvas-desc)}}})