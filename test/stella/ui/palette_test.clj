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

(defn- node-by
  [tool pred]
  (first (filter pred (:children tool))))

(deftest palette-tools-test
  (let [desc (palette/palette-desc)
        [stock flow source sink converter connector] (:children desc)
        source-arrow (node-by source #(and (= :line (:fx/type %))
                                           (= 42 (:start-x %))
                                           (= 24 (:start-y %))))
        sink-arrow (node-by sink #(and (= :line (:fx/type %))
                                       (= 42 (:start-x %))
                                       (= 1 (:start-y %))))]
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
    (is (= [42 24 42 3]
           (mapv source-arrow [:start-x :start-y :end-x :end-y])))
    (is (= ["Sink"] (label-texts sink)))
    (is (= {:event events/arm-sink} (:on-mouse-clicked sink)))
    (is (= 3 (node-count sink :circle)))
    (is (= 0 (node-count sink :ellipse)))
    (is (= [42 1 42 22]
           (mapv sink-arrow [:start-x :start-y :end-x :end-y])))
    (is (= ["Converter"] (label-texts converter)))
    (is (= {:event events/arm-converter} (:on-mouse-clicked converter)))
    (is (some #(= :circle (:fx/type %)) (:children converter)))
    (is (= ["Connector"] (label-texts connector)))
    (is (= {:event events/arm-connector} (:on-mouse-clicked connector)))
    (is (some #(= :quad-curve (:fx/type %)) (:children connector)))
    (is (= 0 (node-count connector :polygon)))
    (is (= 2 (node-count connector :line)))))

(deftest active-palette-tool-is-highlighted-test
  (let [shell (cmd/arm-flow-placement-on-shell! (cmd/default-shell! nil))
        desc (palette/palette-desc shell)
        [stock flow] (:children desc)]
    (is (re-find #"#2f80ed" (:style (background flow))))
    (is (not (re-find #"#2f80ed" (:style (background stock)))))))

(deftest palette-tool-stays-active-after-placing-test
  (let [shell (-> (cmd/default-shell! nil)
                  (cmd/arm-stock-placement-on-shell!)
                  (cmd/place-stock-on-shell! 200 150))
        desc (palette/palette-desc shell)
        [stock] (:children desc)]
    (is (re-find #"#2f80ed" (:style (background stock))))))

(deftest palette-switches-active-tool-when-another-is-clicked-test
  (let [shell (cmd/arm-flow-placement-on-shell!
                (cmd/arm-stock-placement-on-shell! (cmd/default-shell! nil)))
        desc (palette/palette-desc shell)
        [stock flow] (:children desc)]
    (is (re-find #"#2f80ed" (:style (background flow))))
    (is (not (re-find #"#2f80ed" (:style (background stock)))))))
