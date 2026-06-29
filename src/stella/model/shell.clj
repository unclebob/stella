(ns stella.model.shell)

(defn- menu-item [label disabled]
  {:label label :disabled disabled})

(defn- separator []
  {:separator true})

(defn- file-menu []
  {:label "File"
   :items [(menu-item "New" true)
           (menu-item "Open..." true)
           (menu-item "Save" true)
           (menu-item "Save As..." true)
           (separator)
           (menu-item "Quit" false)]})

(defn- edit-menu []
  {:label "Edit"
   :items [(menu-item "Undo" true)
           (menu-item "Redo" true)
           (separator)
           (menu-item "Cut" true)
           (menu-item "Copy" true)
           (menu-item "Paste" true)]})

(defn- view-menu []
  {:label "View"
   :items [(menu-item "Zoom In" true)
           (menu-item "Zoom Out" true)
           (menu-item "Reset Zoom" true)]})

(defn- help-menu []
  {:label "Help"
   :items [(menu-item "About Stella" false)]})

(defn default-diagram []
  {:stocks {}
   :sources {}
   :sinks {}
   :flows {}
   :converters {}
   :connectors {}
   :selection #{}
   :placement-mode :idle
   :flow-draft nil
   :connector-draft nil
   :next-stock-num 1
   :next-source-num 1
   :next-sink-num 1
   :next-flow-num 1
   :next-converter-num 1
   :next-connector-num 1})

(defn default-shell []
  {:showing true
   :window-title "Stella"
   :about-visible false
   :about-text ""
   :menu-bar [(file-menu)
              (edit-menu)
              (view-menu)
              (help-menu)]
   :diagram (default-diagram)})

(defn top-level-menus
  [shell]
  (mapv :label (:menu-bar shell)))

(defn menu-includes?
  [shell menu-label]
  (some #(= menu-label (:label %)) (:menu-bar shell)))

(defn- menu-items
  [shell]
  (mapcat :items (:menu-bar shell)))

(defn menu-item-disabled?
  [shell item-label]
  (when-let [item (first (filter #(and (= (:label %) item-label) (not (:separator %)))
                                 (menu-items shell)))]
    (:disabled item)))

(defn window-title
  [shell]
  (:window-title shell))

(defn showing?
  [shell]
  (:showing shell))

(defn about-visible?
  [shell]
  (:about-visible shell))

(defn about-text
  [shell]
  (:about-text shell))

(defn diagram-empty?
  [shell]
  (empty? (:stocks (:diagram shell))))
