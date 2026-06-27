(ns stella.acceptance.step-handlers-selection
  (:require [stella.acceptance.step-support :as support]
            [stella.commands :as cmd]
            [stella.model :as model]))

(def selection-handlers
  [{:pattern #"^I click select <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (support/selection-kind (support/require-value example kind-param))
                name (support/require-value example name-param)]
            (update world :diagram #(cmd/click-select! % kind name))))}
   {:pattern #"^I click select (stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+)$"
    :fn (fn [world [_ kind name] _]
          (update world :diagram #(cmd/click-select! % (support/selection-kind kind) name)))}
   {:pattern #"^I shift click select <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (support/selection-kind (support/require-value example kind-param))
                name (support/require-value example name-param)]
            (update world :diagram #(cmd/shift-click-select! % kind name))))}
   {:pattern #"^I shift click select (stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+)$"
    :fn (fn [world [_ kind name] _]
          (update world :diagram #(cmd/shift-click-select! % (support/selection-kind kind) name)))}
   {:pattern #"^I marquee select from <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> to <([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ x1-param y1-param x2-param y2-param] example]
          (let [x1 (support/parse-int (support/require-value example x1-param) x1-param)
                y1 (support/parse-int (support/require-value example y1-param) y1-param)
                x2 (support/parse-int (support/require-value example x2-param) x2-param)
                y2 (support/parse-int (support/require-value example y2-param) y2-param)]
            (update world :diagram #(cmd/marquee-select! % x1 y1 x2 y2))))}
   {:pattern #"^I marquee select from (\d+) (\d+) to (\d+) (\d+)$"
    :fn (fn [world [_ x1-str y1-str x2-str y2-str] _]
          (update world :diagram #(cmd/marquee-select! %
                                                       (support/parse-int x1-str "x1")
                                                       (support/parse-int y1-str "y1")
                                                       (support/parse-int x2-str "x2")
                                                       (support/parse-int y2-str "y2"))))}
   {:pattern #"^I clear the selection$"
    :fn (fn [world _ _]
          (update world :diagram cmd/clear-selection!))}
   {:pattern #"^I delete the selection$"
    :fn (fn [world _ _]
          (update world :diagram cmd/delete-selection!))}
   {:pattern #"^<([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> should be selected$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (support/selection-kind (support/require-value example kind-param))
                name (support/require-value example name-param)]
            (when-not (model/selected? (support/diagram-from world) kind name)
              (support/fail! (str kind " " name " not selected")))
            world))}
   {:pattern #"^(stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+) should be selected$"
    :fn (fn [world [_ kind name] _]
          (when-not (model/selected? (support/diagram-from world) (support/selection-kind kind) name)
            (support/fail! (str kind " " name " not selected")))
          world)}
   {:pattern #"^<([A-Za-z0-9_]+)> <([A-Za-z0-9_]+)> should not be selected$"
    :fn (fn [world [_ kind-param name-param] example]
          (let [kind (support/selection-kind (support/require-value example kind-param))
                name (support/require-value example name-param)]
            (when (model/selected? (support/diagram-from world) kind name)
              (support/fail! (str kind " " name " should not be selected")))
            world))}
   {:pattern #"^(stock|converter|flow|connector|source|sink) ([A-Za-z0-9]+) should not be selected$"
    :fn (fn [world [_ kind name] _]
          (when (model/selected? (support/diagram-from world) (support/selection-kind kind) name)
            (support/fail! (str kind " " name " should not be selected")))
          world)}
   {:pattern #"^the selection count should be <([A-Za-z0-9_]+)>$"
    :fn (fn [world [_ count-param] example]
          (let [count (support/parse-int (support/require-value example count-param) count-param)
                actual (model/selection-count (support/diagram-from world))]
            (when-not (= count actual)
              (support/fail! (str "selection count " actual " expected " count)))
            world))}
   {:pattern #"^the selection count should be (\d+)$"
    :fn (fn [world [_ count-str] _]
          (let [count (support/parse-int count-str "count")
                actual (model/selection-count (support/diagram-from world))]
            (when-not (= count actual)
              (support/fail! (str "selection count " actual " expected " count)))
            world))}
   {:pattern #"^nothing should be selected$"
    :fn (fn [world _ _]
          (when-not (model/nothing-selected? (support/diagram-from world))
            (support/fail! (str "selection count " (model/selection-count (support/diagram-from world)) " expected 0")))
          world)}])

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-27T10:27:26.162075-05:00", :module-hash "-1021746358", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 4, :hash "-1671017090"} {:id "def/selection-handlers", :kind "def", :line 6, :end-line 85, :hash "-829439538"}]}
;; clj-mutate-manifest-end
