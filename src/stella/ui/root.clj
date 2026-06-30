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
                  :top {:fx/type :v-box
                        :children [(menu/menu-bar-desc shell)
                                   (control-panel/control-panel-desc shell)]}
                  :left (palette/palette-desc shell)
                  :center (canvas/canvas-stack shell)
                  :bottom {:fx/type :label
                           :id "diagram-overlay"
                           :managed false
                           :visible false
                           :text (canvas/diagram-overlay-text (:diagram shell))}}}})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T08:34:58.605248-05:00", :module-hash "1026949042", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 6, :hash "1855058899"} {:id "defn-/qa-dimension", :kind "defn-", :line 8, :end-line 12, :hash "1169114183"} {:id "defn/root-desc", :kind "defn", :line 14, :end-line 36, :hash "-431194019"}]}
;; clj-mutate-manifest-end
