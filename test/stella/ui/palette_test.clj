(ns stella.ui.palette-test
  (:require [clojure.test :refer [deftest is]]
            [stella.events :as events]
            [stella.ui.palette :as palette]))

(deftest palette-stock-tool-test
  (let [desc (palette/palette-desc)
        button (first (:children desc))]
    (is (= :vbox (:fx/type desc)))
    (is (= "Stock" (:text button)))
    (is (= {:event events/arm-stock} (:on-action button)))))