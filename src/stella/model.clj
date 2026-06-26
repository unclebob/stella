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

(defn default-diagram []
  {:stocks {}
   :flows {}
   :placement-mode :idle
   :flow-draft nil
   :next-stock-num 1
   :next-flow-num 1})

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
  (some #(when (and (= (:label %) item-label) (not (:separator %)))
           (:disabled %))
        (menu-items shell)))

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

(defn stock-count
  [diagram]
  (count (:stocks diagram)))

(defn- stock-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:stocks diagram))))

(defn stock-exists?
  [diagram name]
  (some? (stock-entry-by-name diagram name)))

(defn stock-position
  [diagram name]
  (when-let [[_ stock] (stock-entry-by-name diagram name)]
    [(:x stock) (:y stock)]))

(defn stock-initial-value
  [diagram name]
  (when-let [[_ stock] (stock-entry-by-name diagram name)]
    (:initial-value stock)))

(defn placement-disarmed?
  [diagram]
  (= :idle (:placement-mode diagram)))

(defn arm-stock-placement
  [diagram]
  (assoc diagram :placement-mode :stock))

(defn place-stock
  [diagram x y]
  (if (= :stock (:placement-mode diagram))
    (let [num (:next-stock-num diagram)
          name (str "Stock" num)
          id (keyword (str "stock-" num))
          stock {:name name :initial-value "0" :x x :y y}]
      (-> diagram
          (assoc-in [:stocks id] stock)
          (assoc :placement-mode :idle)
          (update :next-stock-num inc)))
    diagram))

(defn stocks
  [diagram]
  (vals (:stocks diagram)))

(defn- num-from-name
  [prefix name]
  (Integer/parseInt (subs name (count prefix))))

(defn fixture-stock
  [diagram name x y]
  (let [num (num-from-name "Stock" name)
        id (keyword (str "stock-" num))]
    (-> diagram
        (assoc-in [:stocks id] {:name name :initial-value "0" :x x :y y})
        (update :next-stock-num #(max % (inc num))))))

(defn- flow-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:flows diagram))))

(defn flow-exists?
  [diagram name]
  (some? (flow-entry-by-name diagram name)))

(defn flow-endpoints
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    [(:from-stock flow) (:to-stock flow)]))

(defn flow-rate
  [diagram name]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (:rate flow)))

(defn flow-count
  [diagram]
  (count (:flows diagram)))

(defn flows
  [diagram]
  (vals (:flows diagram)))

(defn fixture-flow
  [diagram flow-name from-stock to-stock]
  (let [num (num-from-name "Flow" flow-name)
        id (keyword (str "flow-" num))]
    (-> diagram
        (assoc-in [:flows id] {:name flow-name
                               :from-stock from-stock
                               :to-stock to-stock
                               :rate "0"})
        (update :next-flow-num #(max % (inc num))))))

(defn arm-flow-placement
  [diagram]
  (-> diagram
      (assoc :placement-mode :flow)
      (assoc :flow-draft nil)))

(defn select-flow-source
  [diagram stock-name]
  (if (and (= :flow (:placement-mode diagram))
           (nil? (:flow-draft diagram))
           (stock-exists? diagram stock-name))
    (assoc diagram :flow-draft {:from stock-name})
    diagram))

(defn connect-flow
  [diagram to-stock]
  (if (and (= :flow (:placement-mode diagram))
           (:flow-draft diagram)
           (stock-exists? diagram to-stock))
    (let [{:keys [from]} (:flow-draft diagram)]
      (if (= from to-stock)
        diagram
        (let [num (:next-flow-num diagram)
              name (str "Flow" num)
              id (keyword (str "flow-" num))]
          (-> diagram
              (assoc-in [:flows id] {:name name
                                     :from-stock from
                                     :to-stock to-stock
                                     :rate "0"})
              (assoc :placement-mode :idle
                     :flow-draft nil)
              (update :next-flow-num inc)))))
    diagram))

(defn flow-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))