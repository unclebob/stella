(ns stella.edit-stock-test
  (:require [clojure.test :refer [deftest is]]
            [stella.commands :as cmd]
            [stella.model :as model]))

(defn- diagram-with-stock []
  (cmd/fixture-stock! (cmd/default-diagram! nil) "Stock1" 200 150))

(deftest new-stock-has-default-bounds-test
  (let [diagram (diagram-with-stock)]
    (is (= "0" (model/stock-min-value diagram "Stock1")))
    (is (= "100" (model/stock-max-value diagram "Stock1")))))

(deftest rename-stock-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-name! "Stock1" "Cats"))]
    (is (not (model/stock-exists? diagram "Stock1")))
    (is (model/stock-exists? diagram "Cats"))
    (is (= "0" (model/stock-initial-value diagram "Cats")))))

(deftest reject-duplicate-stock-name-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/fixture-stock! "Stock2" 300 200)
                    (cmd/set-stock-name! "Stock1" "Stock2"))]
    (is (model/stock-exists? diagram "Stock1"))
    (is (model/stock-exists? diagram "Stock2"))))

(deftest set-stock-initial-value-test
  (let [diagram (cmd/set-stock-initial-value! (diagram-with-stock) "Stock1" "25")]
    (is (= "25" (model/stock-initial-value diagram "Stock1")))))

(deftest reject-initial-below-minimum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-min! "Stock1" "10")
                    (cmd/set-stock-initial-value! "Stock1" "5"))]
    (is (= "0" (model/stock-initial-value diagram "Stock1")))))

(deftest reject-initial-above-maximum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-max! "Stock1" "40")
                    (cmd/set-stock-initial-value! "Stock1" "50"))]
    (is (= "0" (model/stock-initial-value diagram "Stock1")))))

(deftest set-stock-bounds-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-min! "Stock1" "0")
                    (cmd/set-stock-max! "Stock1" "100"))]
    (is (= "0" (model/stock-min-value diagram "Stock1")))
    (is (= "100" (model/stock-max-value diagram "Stock1")))))

(deftest clear-stock-maximum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-max! "Stock1" "50")
                    (cmd/clear-stock-max! "Stock1"))]
    (is (nil? (model/stock-max-value diagram "Stock1")))))

(deftest apply-stock-edit-updates-all-fields-test
  (let [diagram (cmd/apply-stock-edit!
                 (diagram-with-stock)
                 "Stock1"
                 {:name "Inventory"
                  :initial-value "25"
                  :min-value "10"
                  :max-value "100"})]
    (is (not (model/stock-exists? diagram "Stock1")))
    (is (model/stock-exists? diagram "Inventory"))
    (is (= "25" (model/stock-initial-value diagram "Inventory")))
    (is (= "10" (model/stock-min-value diagram "Inventory")))
    (is (= "100" (model/stock-max-value diagram "Inventory")))))

(deftest apply-stock-edit-rejects-blocked-rename-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/fixture-stock! "Stock2" 300 200))
        edited (cmd/apply-stock-edit!
                diagram
                "Stock1"
                {:name "Stock2"
                 :initial-value "25"
                 :min-value "10"
                 :max-value "100"})]
    (is (= diagram edited))))

(deftest apply-stock-edit-keeps-valid-prior-fields-after-invalid-minimum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-initial-value! "Stock1" "25")
                    (cmd/set-stock-max! "Stock1" "50"))
        edited (cmd/apply-stock-edit!
                diagram
                "Stock1"
                {:name "Stock1"
                 :initial-value "30"
                 :min-value "60"
                 :max-value "70"})]
    (is (= "Stock1" (:name (first (model/stocks edited)))))
    (is (= "25" (model/stock-initial-value edited "Stock1")))
    (is (= "0" (model/stock-min-value edited "Stock1")))
    (is (= "50" (model/stock-max-value edited "Stock1")))))

(deftest apply-stock-edit-keeps-valid-prior-fields-after-invalid-maximum-test
  (let [diagram (diagram-with-stock)
        edited (cmd/apply-stock-edit!
                diagram
                "Stock1"
                {:name "Stock1"
                 :initial-value "20"
                 :min-value "10"
                 :max-value "5"})]
    (is (= "0" (model/stock-initial-value edited "Stock1")))
    (is (= "10" (model/stock-min-value edited "Stock1")))
    (is (= "100" (model/stock-max-value edited "Stock1")))))

(deftest apply-stock-edit-clears-blank-maximum-test
  (let [diagram (-> (diagram-with-stock)
                    (cmd/set-stock-max! "Stock1" "50"))
        edited (cmd/apply-stock-edit!
                diagram
                "Stock1"
                {:name "Stock1"
                 :initial-value "25"
                 :min-value "0"
                 :max-value ""})]
    (is (= "25" (model/stock-initial-value edited "Stock1")))
    (is (nil? (model/stock-max-value edited "Stock1")))))

(deftest open-edit-stock-on-shell-builds-draft-test
  (let [shell (-> (cmd/default-shell! nil)
                  (assoc :diagram (-> (diagram-with-stock)
                                      (cmd/set-stock-initial-value! "Stock1" "25")
                                      (cmd/set-stock-max! "Stock1" "50")))
                  (cmd/open-edit-stock-on-shell! "Stock1"))]
    (is (= {:stock-name "Stock1"
            :name "Stock1"
            :initial-value "25"
            :min-value "0"
            :max-value "50"}
           (:edit-stock shell)))))
