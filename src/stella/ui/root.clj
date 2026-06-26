(ns stella.ui.root
  (:require [stella.events :as events]
            [stella.ui.canvas :as canvas]
            [stella.ui.menu :as menu]))

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
                  :center (canvas/canvas-desc)}}})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:29:46.873938-05:00", :module-hash "-1315811888", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-992278078"} {:id "defn/root-desc", :kind "defn", :line 6, :end-line 19, :hash "-1394993671"}]}
;; clj-mutate-manifest-end
