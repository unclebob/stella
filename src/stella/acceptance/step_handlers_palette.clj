(ns stella.acceptance.step-handlers-palette
  (:require [stella.acceptance.step-support :as support]))

(def palette-handlers
  [{:pattern #"^the palette tool ([A-Za-z0-9]+) should be active$"
    :fn (fn [world [_ tool-label] _]
          (support/assert-palette-tool-active world tool-label))}
   {:pattern #"^the palette tool ([A-Za-z0-9]+) should be inactive$"
    :fn (fn [world [_ tool-label] _]
          (support/assert-palette-tool-inactive world tool-label))}
   {:pattern #"^no palette tool should be active$"
    :fn (fn [world _ _]
          (support/assert-no-palette-tool-active world))}])