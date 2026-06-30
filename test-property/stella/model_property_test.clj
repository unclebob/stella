(ns stella.model-property-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.model :as model]))

(def ^:private stub-labels
  ["New" "Open..." "Save" "Save As..."
   "Undo" "Redo" "Cut" "Copy" "Paste"
   "Zoom In" "Zoom Out" "Reset Zoom"])

(def ^:private enabled-labels
  ["Quit" "About Stella"])

(defspec default-shell-is-idempotent
  25
  (prop/for-all [_ gen/int]
    (= (model/default-shell) (model/default-shell))))

(defspec default-shell-has-four-menus
  25
  (prop/for-all [_ gen/int]
    (= ["File" "Edit" "View" "Help"]
       (model/top-level-menus (model/default-shell)))))

(defspec stub-items-are-disabled-in-model
  25
  (prop/for-all [_ gen/int]
    (let [shell (model/default-shell)]
      (every? #(model/menu-item-disabled? shell %) stub-labels))))

(defspec enabled-items-are-not-disabled
  25
  (prop/for-all [_ gen/int]
    (let [shell (model/default-shell)]
      (every? #(not (model/menu-item-disabled? shell %)) enabled-labels))))

(deftest default-shell-shows-and-empty-canvas
  (let [shell (model/default-shell)]
    (is (model/showing? shell))
    (is (model/diagram-empty? shell))
    (is (model/control-panel-visible? shell))
    (is (model/step-button-visible? shell))
    (is (= "0" (model/simulation-time-display shell)))))