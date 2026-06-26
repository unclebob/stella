(ns stella.ui.menu-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.events :as events]
            [stella.model :as model]
            [stella.ui.menu :as menu]))

(defn- menu-texts [menu-bar-desc]
  (mapv :text (:menus menu-bar-desc)))

(defn- menu-by-text [menu-bar-desc label]
  (first (filter #(= label (:text %)) (:menus menu-bar-desc))))

(defn- menu-item-by-text [menu label]
  (first (filter #(and (= :menu-item (:fx/type %))
                       (= label (:text %)))
                 (:items menu))))

(def ^:private stub-labels
  {"File" ["New" "Open..." "Save" "Save As..."]
   "Edit" ["Undo" "Redo" "Cut" "Copy" "Paste"]
   "View" ["Zoom In" "Zoom Out" "Reset Zoom"]})

(def ^:private shell (model/default-shell))

(defspec menu-bar-desc-is-idempotent
  25
  (prop/for-all [_ gen/int]
    (let [desc (menu/menu-bar-desc shell)]
      (= desc (menu/menu-bar-desc shell)))))

(defspec menu-bar-has-four-top-level-menus
  25
  (prop/for-all [_ gen/int]
    (let [desc (menu/menu-bar-desc shell)]
      (and (= :menu-bar (:fx/type desc))
           (= ["File" "Edit" "View" "Help"] (menu-texts desc))))))

(defspec stub-items-stay-disabled
  25
  (prop/for-all [_ gen/int]
    (let [desc (menu/menu-bar-desc shell)]
      (every? (fn [[menu-label item-labels]]
                (let [menu (menu-by-text desc menu-label)]
                  (every? #(-> (menu-item-by-text menu %)
                               :disable)
                          item-labels)))
              stub-labels))))

(deftest enabled-actions-use-core-events
  (let [desc (menu/menu-bar-desc shell)
        file (menu-by-text desc "File")
        help (menu-by-text desc "Help")]
    (is (= {:event events/quit}
           (:on-action (menu-item-by-text file "Quit"))))
    (is (= {:event events/show-about}
           (:on-action (menu-item-by-text help "About Stella"))))))