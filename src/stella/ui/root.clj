(ns stella.ui.root
  (:require [stella.events :as events]
            [stella.ui.canvas :as canvas]
            [stella.ui.menu :as menu]
            [stella.ui.palette :as palette]))

(defn root-desc
  [shell]
  {:fx/type :stage
   :title (:window-title shell)
   :showing (:showing shell)
   :width 1024
   :height 768
   :min-width 640
   :min-height 480
   :on-close-request {:event events/quit}
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top (menu/menu-bar-desc shell)
                  :left (palette/palette-desc)
                  :center (canvas/canvas-desc shell)}}})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:41:16.930163-05:00", :module-hash "1329997746", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1522927926"} {:id "defn/root-desc", :kind "defn", :line 7, :end-line 21, :hash "1152760083"}]}
;; clj-mutate-manifest-end
