(ns stella.ui.root
  (:require [stella.events :as events]
            [stella.ui.canvas :as canvas]
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
   :on-close-request {:event events/quit}
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top (menu/menu-bar-desc shell)
                  :left (palette/palette-desc)
                  :center (canvas/canvas-desc shell)}}})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T17:11:42.532455-05:00", :module-hash "351993905", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 5, :hash "1522927926"} {:id "defn-/qa-dimension", :kind "defn-", :line 7, :end-line 11, :hash "1169114183"} {:id "defn/root-desc", :kind "defn", :line 13, :end-line 27, :hash "44152511"}]}
;; clj-mutate-manifest-end
