(ns stella.qa.hit-test
  (:require [stella.model :as model]))

(defn stock-targets
  "Returns semantic hit-test targets for stocks on the diagram."
  [diagram]
  (into {}
        (for [{:keys [name x y]} (model/stocks diagram)]
          [[:stock name] {:x x :y :y :w 80 :h 50}])))