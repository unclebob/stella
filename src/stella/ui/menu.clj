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