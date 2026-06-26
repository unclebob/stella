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
   :sources {}
   :sinks {}
   :flows {}
   :placement-mode :idle
   :flow-draft nil
   :next-stock-num 1
   :next-source-num 1
   :next-sink-num 1
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

(defn- endpoint-ref
  [kind id]
  {:kind kind :id id})

(defn- source-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:sources diagram))))

(defn- sink-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:sinks diagram))))

(defn source-exists?
  [diagram name]
  (some? (source-entry-by-name diagram name)))

(defn sink-exists?
  [diagram name]
  (some? (sink-entry-by-name diagram name)))

(defn source-position
  [diagram name]
  (when-let [[_ source] (source-entry-by-name diagram name)]
    [(:x source) (:y source)]))

(defn sink-position
  [diagram name]
  (when-let [[_ sink] (sink-entry-by-name diagram name)]
    [(:x sink) (:y sink)]))

(defn endpoint-position
  [diagram {:keys [kind id]}]
  (case kind
    :stock (stock-position diagram id)
    :source (source-position diagram id)
    :sink (sink-position diagram id)
    nil))

(def ^:private endpoint-anchor-offsets
  {[:stock :right] [80.0 25.0]
   [:stock :left] [0.0 25.0]
   [:source :right] [80.0 25.0]
   [:sink :left] [0.0 25.0]})

(defn endpoint-anchor
  [[x y] kind side]
  (let [[dx dy] (get endpoint-anchor-offsets [kind side] [0.0 25.0])]
    [(+ x dx) (+ y dy)]))

(defn source-count
  [diagram]
  (count (:sources diagram)))

(defn sink-count
  [diagram]
  (count (:sinks diagram)))

(defn sources
  [diagram]
  (vals (:sources diagram)))

(defn sinks
  [diagram]
  (vals (:sinks diagram)))

(defn- cloud-fixture
  [diagram prefix collection-key next-key name x y]
  (let [num (num-from-name prefix name)
        id (keyword (str (clojure.string/lower-case prefix) "-" num))]
    (-> diagram
        (assoc-in [collection-key id] {:name name :x x :y y})
        (update next-key #(max % (inc num))))))

(defn fixture-source
  [diagram name x y]
  (cloud-fixture diagram "Source" :sources :next-source-num name x y))

(defn fixture-sink
  [diagram name x y]
  (cloud-fixture diagram "Sink" :sinks :next-sink-num name x y))

(defn arm-source-placement
  [diagram]
  (assoc diagram :placement-mode :source))

(defn arm-sink-placement
  [diagram]
  (assoc diagram :placement-mode :sink))

(defn- cloud-place
  [diagram mode prefix collection-key next-key x y]
  (if (= mode (:placement-mode diagram))
    (let [num (get diagram next-key)
          name (str prefix num)
          id (keyword (str (clojure.string/lower-case prefix) "-" num))]
      (-> diagram
          (assoc-in [collection-key id] {:name name :x x :y y})
          (assoc :placement-mode :idle)
          (update next-key inc)))
    diagram))

(defn place-source
  [diagram x y]
  (cloud-place diagram :source "Source" :sources :next-source-num x y))

(defn place-sink
  [diagram x y]
  (cloud-place diagram :sink "Sink" :sinks :next-sink-num x y))

(defn source-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn sink-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

(defn- flow-entry-by-name
  [diagram name]
  (first (filter #(= name (:name (val %))) (:flows diagram))))

(defn flow-exists?
  [diagram name]
  (some? (flow-entry-by-name diagram name)))

(defn- flow-attribute
  [diagram name attribute]
  (when-let [[_ flow] (flow-entry-by-name diagram name)]
    (get flow attribute)))

(defn flow-from
  [diagram name]
  (flow-attribute diagram name :from))

(defn flow-to
  [diagram name]
  (flow-attribute diagram name :to))

(defn flow-endpoints
  [diagram name]
  (let [from (flow-from diagram name)
        to (flow-to diagram name)]
    (when (and from to)
      [(:id from) (:id to)])))

(defn flow-rate
  [diagram name]
  (flow-attribute diagram name :rate))

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
                               :from (endpoint-ref :stock from-stock)
                               :to (endpoint-ref :stock to-stock)
                               :rate "0"})
        (update :next-flow-num #(max % (inc num))))))

(defn- valid-flow-pair?
  [from to]
  (case [(:kind from) (:kind to)]
    [:source :stock] true
    [:stock :stock] (not= (:id from) (:id to))
    [:stock :sink] true
    false))

(defn- create-flow!
  [diagram from to]
  (let [num (:next-flow-num diagram)
        name (str "Flow" num)
        id (keyword (str "flow-" num))]
    (-> diagram
        (assoc-in [:flows id] {:name name :from from :to to :rate "0"})
        (assoc :placement-mode :idle :flow-draft nil)
        (update :next-flow-num inc))))

(defn arm-flow-placement
  [diagram]
  (-> diagram
      (assoc :placement-mode :flow)
      (assoc :flow-draft nil)))

(defn flow-placement-armed?
  [diagram]
  (= :flow (:placement-mode diagram)))

(defn- draft-from-endpoint
  [diagram kind name]
  (cond
    (and (= kind :source) (source-exists? diagram name))
    (assoc diagram :flow-draft {:from (endpoint-ref :source name)})

    (and (= kind :stock) (stock-exists? diagram name))
    (assoc diagram :flow-draft {:from (endpoint-ref :stock name)})

    :else diagram))

(defn select-flow-source
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram))
           (nil? (:flow-draft diagram))
           (not= kind :sink))
    (draft-from-endpoint diagram kind name)
    diagram))

(defn- clear-flow-draft
  [diagram]
  (assoc diagram :flow-draft nil))

(defn- endpoint-exists?
  [diagram kind name]
  (case kind
    :stock (stock-exists? diagram name)
    :sink (sink-exists? diagram name)
    false))

(defn- try-connect-flow
  [diagram from kind name]
  (let [to (endpoint-ref kind name)]
    (cond
      (= kind :source) (clear-flow-draft diagram)
      (and (#{:stock :sink} kind)
           (endpoint-exists? diagram kind name)
           (valid-flow-pair? from to)) (create-flow! diagram from to)
      :else (clear-flow-draft diagram))))

(defn connect-flow
  [diagram kind name]
  (if (and (= :flow (:placement-mode diagram)) (:flow-draft diagram))
    (try-connect-flow diagram (:from (:flow-draft diagram)) kind name)
    diagram))

(defn flow-placement-disarmed?
  [diagram]
  (placement-disarmed? diagram))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-26T16:01:11.632249-05:00", :module-hash "1372488370", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 1, :hash "-289228109"} {:id "defn-/menu-item", :kind "defn-", :line 3, :end-line 4, :hash "1389753846"} {:id "defn-/separator", :kind "defn-", :line 6, :end-line 7, :hash "-830175276"} {:id "defn-/file-menu", :kind "defn-", :line 9, :end-line 16, :hash "-1661503381"} {:id "defn-/edit-menu", :kind "defn-", :line 18, :end-line 25, :hash "1911650603"} {:id "defn-/view-menu", :kind "defn-", :line 27, :end-line 31, :hash "-460340653"} {:id "defn-/help-menu", :kind "defn-", :line 33, :end-line 35, :hash "229400292"} {:id "defn/default-diagram", :kind "defn", :line 37, :end-line 47, :hash "370287833"} {:id "defn/default-shell", :kind "defn", :line 49, :end-line 58, :hash "-1757765176"} {:id "defn/top-level-menus", :kind "defn", :line 60, :end-line 62, :hash "1987860788"} {:id "defn/menu-includes?", :kind "defn", :line 64, :end-line 66, :hash "-1080604946"} {:id "defn-/menu-items", :kind "defn-", :line 68, :end-line 70, :hash "-1907022954"} {:id "defn/menu-item-disabled?", :kind "defn", :line 72, :end-line 76, :hash "250965749"} {:id "defn/window-title", :kind "defn", :line 78, :end-line 80, :hash "-1791944771"} {:id "defn/showing?", :kind "defn", :line 82, :end-line 84, :hash "-727519168"} {:id "defn/about-visible?", :kind "defn", :line 86, :end-line 88, :hash "1936908001"} {:id "defn/about-text", :kind "defn", :line 90, :end-line 92, :hash "706456501"} {:id "defn/diagram-empty?", :kind "defn", :line 94, :end-line 96, :hash "-256542367"} {:id "defn/stock-count", :kind "defn", :line 98, :end-line 100, :hash "-1679213761"} {:id "defn-/stock-entry-by-name", :kind "defn-", :line 102, :end-line 104, :hash "407514139"} {:id "defn/stock-exists?", :kind "defn", :line 106, :end-line 108, :hash "2041739431"} {:id "defn/stock-position", :kind "defn", :line 110, :end-line 113, :hash "69915530"} {:id "defn/stock-initial-value", :kind "defn", :line 115, :end-line 118, :hash "1758215575"} {:id "defn/placement-disarmed?", :kind "defn", :line 120, :end-line 122, :hash "351569028"} {:id "defn/arm-stock-placement", :kind "defn", :line 124, :end-line 126, :hash "1928911371"} {:id "defn/place-stock", :kind "defn", :line 128, :end-line 139, :hash "994882937"} {:id "defn/stocks", :kind "defn", :line 141, :end-line 143, :hash "-801020159"} {:id "defn-/num-from-name", :kind "defn-", :line 145, :end-line 147, :hash "1864201808"} {:id "defn/fixture-stock", :kind "defn", :line 149, :end-line 155, :hash "1232988035"} {:id "defn-/endpoint-ref", :kind "defn-", :line 157, :end-line 159, :hash "916366387"} {:id "defn-/source-entry-by-name", :kind "defn-", :line 161, :end-line 163, :hash "480100498"} {:id "defn-/sink-entry-by-name", :kind "defn-", :line 165, :end-line 167, :hash "-917150361"} {:id "defn/source-exists?", :kind "defn", :line 169, :end-line 171, :hash "1146108815"} {:id "defn/sink-exists?", :kind "defn", :line 173, :end-line 175, :hash "-2079647085"} {:id "defn/source-position", :kind "defn", :line 177, :end-line 180, :hash "1966756"} {:id "defn/sink-position", :kind "defn", :line 182, :end-line 185, :hash "856729128"} {:id "defn/endpoint-position", :kind "defn", :line 187, :end-line 193, :hash "-333085446"} {:id "def/endpoint-anchor-offsets", :kind "def", :line 195, :end-line 199, :hash "1605408378"} {:id "defn/endpoint-anchor", :kind "defn", :line 201, :end-line 204, :hash "-1604573613"} {:id "defn/source-count", :kind "defn", :line 206, :end-line 208, :hash "913527496"} {:id "defn/sink-count", :kind "defn", :line 210, :end-line 212, :hash "1591552436"} {:id "defn/sources", :kind "defn", :line 214, :end-line 216, :hash "-973921734"} {:id "defn/sinks", :kind "defn", :line 218, :end-line 220, :hash "1773380131"} {:id "defn-/cloud-fixture", :kind "defn-", :line 222, :end-line 228, :hash "594001407"} {:id "defn/fixture-source", :kind "defn", :line 230, :end-line 232, :hash "-206300138"} {:id "defn/fixture-sink", :kind "defn", :line 234, :end-line 236, :hash "171187725"} {:id "defn/arm-source-placement", :kind "defn", :line 238, :end-line 240, :hash "-1296780729"} {:id "defn/arm-sink-placement", :kind "defn", :line 242, :end-line 244, :hash "1869647698"} {:id "defn-/cloud-place", :kind "defn-", :line 246, :end-line 256, :hash "-54147391"} {:id "defn/place-source", :kind "defn", :line 258, :end-line 260, :hash "1943934410"} {:id "defn/place-sink", :kind "defn", :line 262, :end-line 264, :hash "1940814177"} {:id "defn/source-placement-disarmed?", :kind "defn", :line 266, :end-line 268, :hash "1135284315"} {:id "defn/sink-placement-disarmed?", :kind "defn", :line 270, :end-line 272, :hash "-1769136352"} {:id "defn-/flow-entry-by-name", :kind "defn-", :line 274, :end-line 276, :hash "690741346"} {:id "defn/flow-exists?", :kind "defn", :line 278, :end-line 280, :hash "905629296"} {:id "defn-/flow-attribute", :kind "defn-", :line 282, :end-line 285, :hash "1273430238"} {:id "defn/flow-from", :kind "defn", :line 287, :end-line 289, :hash "-751374872"} {:id "defn/flow-to", :kind "defn", :line 291, :end-line 293, :hash "1500420793"} {:id "defn/flow-endpoints", :kind "defn", :line 295, :end-line 300, :hash "1470373818"} {:id "defn/flow-rate", :kind "defn", :line 302, :end-line 304, :hash "-2102829222"} {:id "defn/flow-count", :kind "defn", :line 306, :end-line 308, :hash "-2009265396"} {:id "defn/flows", :kind "defn", :line 310, :end-line 312, :hash "608017970"} {:id "defn/fixture-flow", :kind "defn", :line 314, :end-line 323, :hash "207888075"} {:id "defn-/valid-flow-pair?", :kind "defn-", :line 325, :end-line 331, :hash "-269751023"} {:id "defn-/create-flow!", :kind "defn-", :line 333, :end-line 341, :hash "908913489"} {:id "defn/arm-flow-placement", :kind "defn", :line 343, :end-line 347, :hash "737743437"} {:id "defn/flow-placement-armed?", :kind "defn", :line 349, :end-line 351, :hash "-943631268"} {:id "defn-/draft-from-endpoint", :kind "defn-", :line 353, :end-line 362, :hash "1978981143"} {:id "defn/select-flow-source", :kind "defn", :line 364, :end-line 370, :hash "-323164585"} {:id "defn-/clear-flow-draft", :kind "defn-", :line 372, :end-line 374, :hash "-371891314"} {:id "defn-/endpoint-exists?", :kind "defn-", :line 376, :end-line 381, :hash "-1208179464"} {:id "defn-/try-connect-flow", :kind "defn-", :line 383, :end-line 391, :hash "822432826"} {:id "defn/connect-flow", :kind "defn", :line 393, :end-line 397, :hash "-817203079"} {:id "defn/flow-placement-disarmed?", :kind "defn", :line 399, :end-line 401, :hash "31272842"}]}
;; clj-mutate-manifest-end
