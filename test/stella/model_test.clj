(ns stella.model-test
  (:require [clojure.test :refer [deftest is]]
            [stella.model :as model]))

(deftest default-shell-test
  (let [shell (model/default-shell)]
    (is (:showing shell))
    (is (= "Stella" (:window-title shell)))
    (is (false? (:about-visible shell)))
    (is (empty? (:diagram-elements shell)))
    (is (= ["File" "Edit" "View" "Help"] (model/top-level-menus shell)))))

(deftest menu-item-queries-test
  (let [shell (model/default-shell)]
    (is (model/menu-includes? shell "File"))
    (is (model/menu-item-disabled? shell "New"))
    (is (model/menu-item-disabled? shell "Open..."))
    (is (not (model/menu-item-disabled? shell "Quit")))
    (is (not (model/menu-item-disabled? shell "About Stella")))))