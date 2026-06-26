(ns stella.ui.canvas-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.model :as model]
            [stella.ui.canvas :as canvas]))

(deftest canvas-description-test
  (let [shell (model/default-shell)
        desc (canvas/canvas-desc shell)
        pane ((:fx/type desc) (dissoc desc :fx/type))]
    (is (fn? (:fx/type desc)))
    (is (= :pane (:fx/type pane)))
    (is (some? (:style desc)))
    (is (re-find #"background-color" (:style desc)))
    (is (= :always (:vgrow desc)))
    (is (= :always (:hgrow desc)))
    (is (= {:event events/canvas-click} (:on-mouse-clicked desc)))))

(deftest canvas-renders-stocks-test
  (let [shell (-> (cmd/default-shell! nil)
                  (cmd/arm-stock-placement-on-shell!)
                  (cmd/place-stock-on-shell! 200 150))
        desc (canvas/canvas-desc shell)
        stocks (:children desc)]
    (is (= 1 (count stocks)))
    (let [group (first stocks)]
      (is (= :group (:fx/type group)))
      (is (= 200 (:layout-x group)))
      (is (= 150 (:layout-y group))))))