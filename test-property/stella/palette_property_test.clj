(ns stella.palette-property-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :refer [for-all]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(def ^:private tool-labels
  ["Stock" "Flow" "Source" "Sink" "Converter" "Connector"])

(def ^:private arm-shell
  {"Stock" cmd/arm-stock-placement-on-shell!
   "Flow" cmd/arm-flow-placement-on-shell!
   "Source" cmd/arm-source-placement-on-shell!
   "Sink" cmd/arm-sink-placement-on-shell!
   "Converter" cmd/arm-converter-placement-on-shell!
   "Connector" cmd/arm-connector-placement-on-shell!})

(defspec armed-tool-is-active-on-palette
  25
  (for-all [tool (gen/elements tool-labels)]
    (let [shell ((get arm-shell tool) (cmd/default-shell! nil))]
      (model/palette-tool-active? shell tool))))

(defspec inactive-tools-stay-inactive
  25
  (for-all [active (gen/elements tool-labels)
            inactive (gen/elements tool-labels)]
    (let [shell ((get arm-shell active) (cmd/default-shell! nil))]
      (or (= active inactive)
          (not (model/palette-tool-active? shell inactive))))))

(defspec cloud-placement-clears-palette-highlight
  25
  (prop/for-all [_ gen/int]
    (let [after-source (-> (cmd/default-shell! nil)
                           (cmd/arm-source-placement-on-shell!)
                           (cmd/place-source-on-shell! 50 150))
          after-sink (-> (cmd/default-shell! nil)
                         (cmd/arm-sink-placement-on-shell!)
                         (cmd/place-sink-on-shell! 400 150))]
      (and (model/no-palette-tool-active? after-source)
           (model/no-palette-tool-active? after-sink)))))

(defspec escape-disarms-palette-selection
  25
  (prop/for-all [tool (gen/elements tool-labels)]
    (let [shell (-> ((get arm-shell tool) (cmd/default-shell! nil))
                    (cmd/cancel-on-escape-on-shell!))]
      (model/no-palette-tool-active? shell))))