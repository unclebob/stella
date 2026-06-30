(ns stella.ui.root
  (:require [stella.events :as events]
            [stella.ui.canvas :as canvas]
            [stella.ui.control-panel :as control-panel]
            [stella.ui.menu :as menu]
            [stella.ui.palette :as palette]))

(defn- qa-dimension [property default]
  (if-let [value (System/getProperty property)]
    (try (Integer/parseInt value)
         (catch Exception _ default))
    default))

(defn root-desc
  [shell]
  {:fx/type :stage
   :title (:window-title shell)
   :showing (:showing shell)
   :width (qa-dimension "stella.qa.width" 1024)
   :height (qa-dimension "stella.qa.height" 768)
   :min-width 640
   :min-height 480
   :on-close-request {:event events/window-close}
   :scene {:fx/type :scene
           :on-key-pressed {:event events/scene-key-pressed}
           :root {:fx/type :border-pane
                  :top {:fx/type :vbox
                        :children [(menu/menu-bar-desc shell)
                                   (control-panel/control-panel-desc shell)]}
                  :left (palette/palette-desc shell)
                  :center (canvas/canvas-stack shell)
                  :bottom {:fx/type :label
                           :id "diagram-overlay"
                           :managed false
                           :visible false
                           :text (canvas/diagram-overlay-text (:diagram shell))}}}})
