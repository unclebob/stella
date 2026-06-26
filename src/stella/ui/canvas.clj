(ns stella.ui.canvas)

(defn- canvas-pane [{:keys [style]}]
  {:fx/type :pane
   :style style})

(defn canvas-desc
  []
  {:fx/type canvas-pane
   :style "-fx-background-color: #f5f5f5;"
   :vgrow :always
   :hgrow :always})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:28:30.193594-05:00", :module-hash "1579264761", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-1568917532"} {:id "defn-/canvas-pane", :kind "defn-", :line 3, :end-line 5, :hash "318399270"} {:id "defn/canvas-desc", :kind "defn", :line 7, :end-line 12, :hash "309176702"}]}
;; clj-mutate-manifest-end
