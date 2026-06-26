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

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T14:51:54.32515-05:00", :module-hash "-618001099", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-992278078"} {:id "defn/root-desc", :kind "defn", :line 6, :end-line 19, :hash "722548944"}]}
;; clj-mutate-manifest-end
