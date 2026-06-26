(ns stella.ui.canvas-test
  (:require [clojure.test :refer [deftest is]]
            [stella.ui.canvas :as canvas]))

(deftest canvas-description-test
  (let [desc (canvas/canvas-desc)
        pane ((:fx/type desc) (dissoc desc :fx/type))]
    (is (fn? (:fx/type desc)))
    (is (= :pane (:fx/type pane)))
    (is (some? (:style desc)))
    (is (re-find #"background-color" (:style desc)))
    (is (= :always (:vgrow desc)))
    (is (= :always (:hgrow desc)))))