(ns stella.ui.menu
  (:require [stella.events :as events]))

(def ^:private item-events
  {"Quit" events/quit
   "About Stella" events/show-about})

(defn- separator-desc []
  {:fx/type :separator-menu-item})

(defn- menu-item-desc
  [item]
  (if (:separator item)
    (separator-desc)
    (cond-> {:fx/type :menu-item
             :text (:label item)}
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
;; {:version 1, :tested-at "2026-06-26T15:28:39.016963-05:00", :module-hash "1629758659", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-504056843"} {:id "def/item-events", :kind "def", :line 4, :end-line 6, :hash "-1037627931"} {:id "defn-/separator-desc", :kind "defn-", :line 8, :end-line 9, :hash "1614749365"} {:id "defn-/menu-item-desc", :kind "defn-", :line 11, :end-line 19, :hash "1138121662"} {:id "defn-/menu-desc", :kind "defn-", :line 21, :end-line 25, :hash "1147155190"} {:id "defn/menu-bar-desc", :kind "defn", :line 27, :end-line 30, :hash "-1384547785"}]}
;; clj-mutate-manifest-end
