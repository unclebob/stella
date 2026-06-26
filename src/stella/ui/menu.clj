(ns stella.ui.menu)

(def ^:private menu-specs
  [{:label "File"
    :items ["New" "Open…" "Save" "Save As…" :separator
            {:text "Quit" :enabled true :on-action {:event :stella.app/quit}}]}
   {:label "Edit"
    :items ["Undo" "Redo" :separator "Cut" "Copy" "Paste"]}
   {:label "View"
    :items ["Zoom In" "Zoom Out" "Reset Zoom"]}
   {:label "Help"
    :items [{:text "About Stella"
             :enabled true
             :on-action {:event :stella.app/show-about}}]}])

(defn- separator []
  {:fx/type :separator})

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