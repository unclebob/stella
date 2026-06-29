(ns stella.ui.palette-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.events :as events]
            [stella.ui.palette :as palette]))

(defn- descendant-types
  [desc]
  (tree-seq map? :children desc))

(defn- label-texts
  [tool]
  (map :text (filter #(= :label (:fx/type %)) (:children tool))))

(defn- background
  [tool]
  (first (:children tool)))

(defn- node-count
  [tool fx-type]
  (count (filter #(= fx-type (:fx/type %)) (:children tool))))

(deftest palette-tools-test
  (let [desc (palette/palette-desc)
        [stock flow source sink converter connector] (:children desc)]
    (is (= :pane (:fx/type desc)))
    (is (= 82 (:pref-width desc)))
    (is (every? #(= :group (:fx/type %)) (:children desc)))
    (is (empty? (filter #(= :button (:fx/type %)) (descendant-types desc))))
    (is (= "palette-Stock" (:id stock)))
    (is (= ["Stock"] (label-texts stock)))
    (is (= {:event events/arm-stock} (:on-mouse-clicked stock)))
    (is (some #(= :rectangle (:fx/type %)) (:children stock)))
    (is (= ["Flow"] (label-texts flow)))
    (is (= {:event events/arm-flow} (:on-mouse-clicked flow)))
    (is (some #(= :polygon (:fx/type %)) (:children flow)))
    (is (some #(= :circle (:fx/type %)) (:children flow)))
    (is (= ["Source"] (label-texts source)))
    (is (= {:event events/arm-source} (:on-mouse-clicked source)))
    (is (= 3 (node-count source :circle)))
    (is (= 0 (node-count source :ellipse)))
    (is (= ["Sink"] (label-texts sink)))
    (is (= {:event events/arm-sink} (:on-mouse-clicked sink)))
    (is (= 3 (node-count sink :circle)))
    (is (= 0 (node-count sink :ellipse)))
    (is (= ["Converter"] (label-texts converter)))
    (is (= {:event events/arm-converter} (:on-mouse-clicked converter)))
    (is (some #(= :circle (:fx/type %)) (:children converter)))
    (is (= ["Connector"] (label-texts connector)))
    (is (= {:event events/arm-connector} (:on-mouse-clicked connector)))
    (is (some #(= :quad-curve (:fx/type %)) (:children connector)))
    (is (= 1 (node-count connector :polygon)))
    (is (= 0 (node-count connector :line)))))

(deftest active-palette-tool-is-highlighted-test
  (let [shell (cmd/arm-flow-placement-on-shell! (cmd/default-shell! nil))
        desc (palette/palette-desc shell)
        [stock flow] (:children desc)]
    (is (re-find #"#2f80ed" (:style (background flow))))
    (is (not (re-find #"#2f80ed" (:style (background stock)))))))
