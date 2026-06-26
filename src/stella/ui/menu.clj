(ns stella.ui.menu
  (:require [stella.events :as events]
            [stella.model :as model]))

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
      (= "Quit" (:label item)) (assoc :on-action {:event events/quit})
      (= "About Stella" (:label item)) (assoc :on-action {:event events/show-about}))))

(defn- menu-desc
  [menu]
  {:fx/type :menu
   :text (:label menu)
   :items (mapv menu-item-desc (:items menu))})

(defn menu-bar-desc
  [shell]
  {:fx/type :menu-bar
   :menus (mapv menu-desc (:menu-bar shell))})