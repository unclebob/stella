(ns stella.ui.menu
  (:require [stella.events :as events]))

(def ^:private menu-specs
  [{:label "File"
    :items ["New" "Open…" "Save" "Save As…" :separator
            {:text "Quit" :enabled true :on-action {:event events/quit}}]}
   {:label "Edit"
    :items ["Undo" "Redo" :separator "Cut" "Copy" "Paste"]}
   {:label "View"
    :items ["Zoom In" "Zoom Out" "Reset Zoom"]}
   {:label "Help"
    :items [{:text "About Stella"
             :enabled true
             :on-action {:event events/show-about}}]}])

(defn- separator []
  {:fx/type :separator-menu-item})

(defn- menu-item-desc
  [{:keys [text enabled on-action]
    :or {enabled false}}]
  (cond-> {:fx/type :menu-item :text text}
    (not enabled) (assoc :disable true)
    on-action (assoc :on-action on-action)))

(defn- item-desc [spec]
  (cond
    (= :separator spec) (separator)
    (string? spec) (menu-item-desc {:text spec})
    (map? spec) (menu-item-desc spec)))

(defn- menu-desc [{:keys [label items]}]
  {:fx/type :menu
   :text label
   :items (mapv item-desc items)})

(defn menu-bar-desc
  []
  {:fx/type :menu-bar
   :menus (mapv menu-desc menu-specs)})

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T14:51:13.98314-05:00", :module-hash "1542794657", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "-504056843"} {:id "def/menu-specs", :kind "def", :line 4, :end-line 15, :hash "655411168"} {:id "defn-/separator", :kind "defn-", :line 17, :end-line 18, :hash "-417437172"} {:id "defn-/menu-item-desc", :kind "defn-", :line 20, :end-line 25, :hash "1594704375"} {:id "defn-/item-desc", :kind "defn-", :line 27, :end-line 31, :hash "-401100150"} {:id "defn-/menu-desc", :kind "defn-", :line 33, :end-line 36, :hash "1395052324"} {:id "defn/menu-bar-desc", :kind "defn", :line 38, :end-line 41, :hash "-175047454"}]}
;; clj-mutate-manifest-end
