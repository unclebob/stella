(ns stella.ui.menu
  (:require [stella.events :as events]))

(def ^:private item-events
  {"Quit" events/quit
   "About Stella" events/show-about})

(def ^:private display-labels
  {"Open..." "Open…"
   "Save As..." "Save As…"})

(defn- display-text
  [label]
  (get display-labels label label))

(defn- separator-desc []
  {:fx/type :separator-menu-item})

(defn- menu-item-desc
  [item]
  (if (:separator item)
    (separator-desc)
    (cond-> {:fx/type :menu-item
             :text (display-text (:label item))}
      (:disabled item) (assoc :disable true)
      (get item-events (:label item))
      (assoc :on-action {:event (get item-events (:label item))}))))

(defn- menu-desc
  [menu]
  {:fx/type :menu
   :text (:label menu)
   :items (mapv menu-item-desc (:items menu))})

(defn menu-bar-desc
  [shell]
  {:fx/type :menu-bar
   :menus (mapv menu-desc (:menu-bar shell))})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:40:07.959331-05:00", :module-hash "173298056", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-504056843"} {:id "def/item-events", :kind "def", :line 4, :end-line 6, :hash "-1037627931"} {:id "def/display-labels", :kind "def", :line 8, :end-line 10, :hash "-1800106824"} {:id "defn-/display-text", :kind "defn-", :line 12, :end-line 14, :hash "-1500706207"} {:id "defn-/separator-desc", :kind "defn-", :line 16, :end-line 17, :hash "1614749365"} {:id "defn-/menu-item-desc", :kind "defn-", :line 19, :end-line 27, :hash "641107846"} {:id "defn-/menu-desc", :kind "defn-", :line 29, :end-line 33, :hash "1147155190"} {:id "defn/menu-bar-desc", :kind "defn", :line 35, :end-line 38, :hash "-1384547785"}]}
;; clj-mutate-manifest-end
