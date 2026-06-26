(ns stella.ui.menu
  (:require [stella.model :as model]))

(defn- separator-desc []
  {:fx/type :separator})

(defn- menu-item-desc
  [item]
  (if (:separator item)
    (separator-desc)
    (cond-> {:fx/type :menu-item
             :text (:label item)}
      (:disabled item) (assoc :disable true)
      (= "Quit" (:label item)) (assoc :on-action {:event :stella.app/quit})
      (= "About Stella" (:label item)) (assoc :on-action {:event :stella.app/show-about}))))

(defn- menu-desc
  [menu]
  {:fx/type :menu
   :text (:label menu)
   :items (mapv menu-item-desc (:items menu))})

(defn menu-bar-desc
  [shell]
  {:fx/type :menu-bar
   :menus (mapv menu-desc (:menu-bar shell))})