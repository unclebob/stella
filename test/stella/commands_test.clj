(ns stella.commands-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(deftest default-shell-command-test
  (is (= (model/default-shell) (cmd/default-shell! nil))))

(deftest show-about-command-test
  (let [shell (cmd/show-about! (model/default-shell))]
    (is (:about-visible shell))
    (is (re-find #"Stella" (:about-text shell)))))

(deftest quit-command-test
  (let [shell (cmd/quit! (model/default-shell))]
    (is (false? (:showing shell)))))