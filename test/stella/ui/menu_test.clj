(ns stella.ui.menu-test
  (:require [clojure.test :refer [deftest is testing]]
            [stella.events :as events]
            [stella.ui.menu :as menu]))

(defn- menu-texts [menu-bar-desc]
  (mapv :text (:menus menu-bar-desc)))

(defn- menu-by-text [menu-bar-desc label]
  (first (filter #(= label (:text %)) (:menus menu-bar-desc))))

(defn- item-texts [menu]
  (keep (fn [item]
          (case (:fx/type item)
            :menu-item (:text item)
            :separator :separator
            nil))
        (:items menu)))

(defn- menu-item-by-text [menu label]
  (first (filter #(and (= :menu-item (:fx/type %))
                       (= label (:text %)))
                 (:items menu))))

(deftest menu-bar-structure-test
  (let [desc (menu/menu-bar-desc)]
    (is (= :menu-bar (:fx/type desc)))
    (is (= ["File" "Edit" "View" "Help"] (menu-texts desc)))))

(deftest stub-menu-items-disabled-test
  (let [desc (menu/menu-bar-desc)
        file (menu-by-text desc "File")
        edit (menu-by-text desc "Edit")
        view (menu-by-text desc "View")]
    (testing "File stub items"
      (doseq [label ["New" "Open…" "Save" "Save As…"]]
        (is (:disable (menu-item-by-text file label)))))
    (testing "Edit stub items"
      (doseq [label ["Undo" "Redo" "Cut" "Copy" "Paste"]]
        (is (:disable (menu-item-by-text edit label)))))
    (testing "View stub items"
      (doseq [label ["Zoom In" "Zoom Out" "Reset Zoom"]]
        (is (:disable (menu-item-by-text view label)))))))

(deftest enabled-menu-items-test
  (let [desc (menu/menu-bar-desc)
        file (menu-by-text desc "File")
        help (menu-by-text desc "Help")
        quit (menu-item-by-text file "Quit")
        about (menu-item-by-text help "About Stella")]
    (is (not (:disable quit)))
    (is (not (:disable about)))))

(deftest quit-map-event-test
  (let [desc (menu/menu-bar-desc)
        file (menu-by-text desc "File")
        quit (menu-item-by-text file "Quit")]
    (is (= {:event events/quit} (:on-action quit)))))