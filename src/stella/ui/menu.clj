(ns stella.ui.menu)

(defn- menu-item
  ([text] (menu-item text true nil))
  ([text disabled] (menu-item text disabled nil))
  ([text disabled on-action]
   (cond-> {:fx/type :menu-item :text text}
     disabled (assoc :disable true)
     on-action (assoc :on-action on-action))))

(defn- separator []
  {:fx/type :separator})

(defn menu-bar-desc
  []
  {:fx/type :menu-bar
   :menus [{:fx/type :menu
            :text "File"
            :items [(menu-item "New")
                    (menu-item "Open…")
                    (menu-item "Save")
                    (menu-item "Save As…")
                    (separator)
                    (menu-item "Quit" false {:event :stella.app/quit})]}
           {:fx/type :menu
            :text "Edit"
            :items [(menu-item "Undo")
                    (menu-item "Redo")
                    (separator)
                    (menu-item "Cut")
                    (menu-item "Copy")
                    (menu-item "Paste")]}
           {:fx/type :menu
            :text "View"
            :items [(menu-item "Zoom In")
                    (menu-item "Zoom Out")
                    (menu-item "Reset Zoom")]}
           {:fx/type :menu
            :text "Help"
            :items [(menu-item "About Stella" false {:event :stella.app/show-about})]}]})