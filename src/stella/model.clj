(ns stella.model)

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

(defn default-shell []
  {:showing true
   :window-title "Stella"
   :about-visible false
   :about-text ""
   :menu-bar [(file-menu)
              (edit-menu)
              (view-menu)
              (help-menu)]
   :diagram-elements []})

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
  (empty? (:diagram-elements shell)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T15:20:13.960877-05:00", :module-hash "1645083099", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-289228109"} {:id "defn-/menu-item", :kind "defn-", :line 3, :end-line 4, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 6, :end-line 7, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 9, :end-line 16, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 18, :end-line 25, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 27, :end-line 31, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 33, :end-line 35, :hash "229400292"} {:id "defn/default-shell", :kind "defn", :line 37, :end-line 46, :hash "358811537"} {:id "defn/top-level-menus", :kind "defn", :line 48, :end-line 50, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 52, :end-line 54, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 56, :end-line 58, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 60, :end-line 64, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 66, :end-line 68, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 70, :end-line 72, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 74, :end-line 76, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 78, :end-line 80, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 82, :end-line 84, :hash "902320488"}]}
;; clj-mutate-manifest-end
