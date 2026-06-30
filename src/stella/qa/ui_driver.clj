(ns stella.qa.ui-driver
  "Drives the live CljFX app through synthesized UI events only (no OS Robot)."
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [stella.app :as app]
            [stella.events :as events]
            [stella.model :as model]
            [stella.fx.nodes :as fx-nodes]
            [stella.qa.hit-test :as hit-test]
            [stella.ui.canvas :as canvas])
  (:import [javafx.event ActionEvent EventHandler]
           [javafx.geometry Bounds]
           [javafx.scene Group Node Parent]
           [javafx.scene.control Button Label Menu MenuBar MenuItem TextField]
           [javafx.scene.input ContextMenuEvent KeyCode MouseButton MouseEvent]
           [javafx.scene.layout BorderPane]
           [javafx.scene.shape Rectangle]
           [javafx.stage Stage Window]))

(defn launch-app!
  [& {:keys [width height hard-exit?] :as _opts}]
  (when width
    (System/setProperty "stella.qa.width" (str width)))
  (when height
    (System/setProperty "stella.qa.height" (str height)))
  (System/setProperty "stella.qa.soft-exit" (if hard-exit? "false" "true"))
  (System/setProperty "stella.qa.semantic-targets" "true")
  (fx/on-fx-thread (app/start!)))

(defn wait-for-stage
  [attempts]
  (loop [n attempts]
    (if-let [stage (first (filter #(instance? Stage %) (Window/getWindows)))]
      stage
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn main-stage []
  (first (filter #(instance? Stage %) (Window/getWindows))))

(defn- find-menu-bar
  [^Node node]
  (cond
    (instance? MenuBar node) node
    (instance? Parent node)
    (some find-menu-bar (.getChildrenUnmodifiable ^Parent node))
    :else nil))

(defn menu-bar [^Stage stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (find-menu-bar (.getTop ^BorderPane root)))))

(defn- menu-by-label [^MenuBar bar label]
  (some #(when (= label (.getText ^Menu %)) %)
        (.getMenus bar)))

(defn- menu-item-by-text [^Menu menu label]
  (some #(when (and (instance? MenuItem %)
                    (= label (.getText ^MenuItem %)))
          %)
        (.getItems menu)))

(defn open-menu!
  [^Stage stage menu-label]
  (when-let [^Menu menu (menu-by-label (menu-bar stage) menu-label)]
    (.show menu)
    (Thread/sleep 50)))

(defn menu-choose!
  [^Stage stage menu-label item-label]
  (let [^MenuBar bar (menu-bar stage)
        ^Menu menu (menu-by-label bar menu-label)
        ^MenuItem item (menu-item-by-text menu item-label)]
    (.show menu)
    (Thread/sleep 50)
    (when-let [handler (.getOnAction item)]
      (.handle ^javafx.event.EventHandler handler (ActionEvent.)))))

(defn close-menus! [^Stage stage]
  (doseq [^Menu menu (.getMenus (menu-bar stage))]
    (.hide menu))
  (Thread/sleep 50))

(defn- find-ok-button [^Node node]
  (cond
    (and (instance? Button node) (= "OK" (.getText ^Button node))) node
    (instance? Parent node)
    (some find-ok-button (.getChildrenUnmodifiable ^Parent node))
    :else nil))

(defn- dialog-stage [title]
  (first (filter #(and (instance? Stage %)
                       (= title (.getTitle ^Stage %)))
                 (Window/getWindows))))

(defn click-ok-on-front-dialog! []
  (when-let [^Stage dialog (dialog-stage "About Stella")]
    (when-let [^Button ok (find-ok-button (.getRoot (.getScene dialog)))]
      (.fire ok))))

(defn- find-ok-in-node [^Node node]
  (or (find-ok-button node)
      (when (instance? Parent node)
        (some find-ok-in-node (.getChildrenUnmodifiable ^Parent node)))))

(defn click-ok-on-dialog! [title]
  (when-let [root (some-> (dialog-stage title) .getScene .getRoot)]
    (when-let [^Button ok (find-ok-in-node root)]
      (if-let [handler (.getOnAction ok)]
        (.handle ^EventHandler handler (ActionEvent.))
        (.fire ok))
      (Thread/sleep 250))))

(defn- edit-stock-dialog-open? []
  (or (boolean (dialog-stage "Edit Stock"))
      (boolean (fx-nodes/find-by-id-in-windows "edit-stock-name"))))

(defn- edit-flow-dialog-open? []
  (or (boolean (dialog-stage "Edit Flow"))
      (boolean (fx-nodes/find-by-id-in-windows "edit-flow-name"))))

(defn- edit-converter-dialog-open? []
  (or (boolean (dialog-stage "Edit Converter"))
      (boolean (fx-nodes/find-by-id-in-windows "edit-converter-name"))))

(defn wait-for-dialog! [title & {:keys [attempts] :or {attempts 20}}]
  (loop [n attempts]
    (if (case title
          "Edit Stock" (edit-stock-dialog-open?)
          "Edit Flow" (edit-flow-dialog-open?)
          "Edit Converter" (edit-converter-dialog-open?)
          (boolean (dialog-stage title)))
      true
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn- dialog-field-id [field-label]
  (case field-label
    "Name" #{"edit-stock-name" "edit-flow-name" "edit-converter-name"}
    "Initial value" #{"edit-stock-initial"}
    "Minimum" #{"edit-stock-min"}
    "Maximum" #{"edit-stock-max"}
    "Rate" #{"edit-flow-rate"}
    "Formula" #{"edit-converter-formula"}
    nil))

(defn- active-edit-dialog-stage []
  (or (dialog-stage "Edit Converter")
      (dialog-stage "Edit Flow")
      (dialog-stage "Edit Stock")))

(defn- find-dialog-field [field-label]
  (when-let [ids (dialog-field-id field-label)]
    (if-let [^Stage dialog (active-edit-dialog-stage)]
      (when-let [root (some-> dialog .getScene .getRoot)]
        (some #(fx-nodes/find-by-id root %) ids))
      (some fx-nodes/find-by-id-in-windows ids))))

(defn type-into-dialog-field! [field-label text]
  (when-let [^TextField field (find-dialog-field field-label)]
    (.setText field text)
    (Thread/sleep 100)))

(defn clear-dialog-field! [field-label]
  (type-into-dialog-field! field-label ""))

(defn flow-pipe-thicker-than-connector? [_stage]
  (> canvas/flow-pipe-stroke-width canvas/connector-stroke-width))

(defn- palette-pane [^Stage stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (.getLeft ^BorderPane root))))

(defn- find-button-by-text [^Node node text]
  (fx-nodes/find-child
   node
   #(and (instance? Button %) (= text (.getText ^Button %)))))

(def ^:private palette-arm-events
  {"Stock" events/arm-stock
   "Flow" events/arm-flow
   "Source" events/arm-source
   "Sink" events/arm-sink
   "Converter" events/arm-converter
   "Connector" events/arm-connector})

(defn- mouse-event
  [^Node node event-type lx ly screen-x screen-y button click-count shift-down?]
  (MouseEvent. node node
               event-type
               lx ly screen-x screen-y
               button click-count
               shift-down? false false true false false false false false false
               nil))

(defn- fire-mouse-click!
  ([^Node node screen-x screen-y]
   (fire-mouse-click! node screen-x screen-y false))
  ([^Node node screen-x screen-y shift-down?]
   (let [local (.screenToLocal node screen-x screen-y)
         event (mouse-event node
                            MouseEvent/MOUSE_CLICKED
                            (.getX local) (.getY local)
                            screen-x screen-y
                            MouseButton/PRIMARY
                            1
                            shift-down?)]
     (.fireEvent node event))))

(defn- fire-mouse-drag!
  [^Node node start-screen-x start-screen-y end-screen-x end-screen-y]
  (let [start-local (.screenToLocal node start-screen-x start-screen-y)
        end-local (.screenToLocal node end-screen-x end-screen-y)
        sx0 (.getX start-local)
        sy0 (.getY start-local)
        sx1 (.getX end-local)
        sy1 (.getY end-local)
        pressed (mouse-event node MouseEvent/MOUSE_PRESSED
                             sx0 sy0 start-screen-x start-screen-y
                             MouseButton/PRIMARY 1 false)
        dragged (mouse-event node MouseEvent/MOUSE_DRAGGED
                             sx1 sy1 end-screen-x end-screen-y
                             MouseButton/PRIMARY 1 false)
        released (mouse-event node MouseEvent/MOUSE_RELEASED
                              sx1 sy1 end-screen-x end-screen-y
                              MouseButton/PRIMARY 1 false)]
    (.fireEvent node pressed)
    (.fireEvent node dragged)
    (.fireEvent node released)))

(defn- fire-context-menu!
  [^Node node screen-x screen-y]
  (when-let [^EventHandler handler (.getOnContextMenuRequested node)]
    (let [local (.screenToLocal node screen-x screen-y)
          ^ContextMenuEvent event (ContextMenuEvent.
                                 node node
                                 ContextMenuEvent/CONTEXT_MENU_REQUESTED
                                 (.getX local) (.getY local)
                                 screen-x screen-y
                                 false nil)]
      (.handle handler event))))

(defn- resolve-position
  [position center]
  (cond
    (nil? position) center
    (keyword? position) (case position
                          :center center
                          center)
    (and (vector? position) (= 2 (count position)))
    (let [[dx dy] position]
      {:x (+ (:x center) dx) :y (+ (:y center) dy)})
    :else center))

(defn- palette-tool-target
  [tool-node]
  (or (fx-nodes/find-child tool-node #(instance? Rectangle %))
      tool-node))

(defn click-palette!
  [^Stage stage label]
  (when-let [palette (palette-pane stage)]
    (cond
      (some? (fx-nodes/find-by-id-in-windows (str "palette-" label)))
      (let [tool (fx-nodes/find-by-id-in-windows (str "palette-" label))
            ^Node target (palette-tool-target tool)
            {:keys [x y]} (hit-test/node-screen-center target)]
        (fire-mouse-click! target x y)
        (Thread/sleep 250))

      (some? (find-button-by-text palette label))
      (let [^Button button (find-button-by-text palette label)]
        (.fire button)
        (Thread/sleep 250))

      (get palette-arm-events label)
      (do (app/dispatch-map-event! {:event (get palette-arm-events label)})
          (Thread/sleep 250)))))

(defn- screen-to-canvas-local
  [^Node canvas screen-x screen-y]
  (let [local (.screenToLocal canvas screen-x screen-y)]
    [(int (.getX local)) (int (.getY local))]))

(defn- canvas-local-to-scene
  [^Node canvas canvas-x canvas-y]
  (let [scene (.localToScene canvas (double canvas-x) (double canvas-y))]
    [(int (.getX scene)) (int (.getY scene))]))

(defn- synthesize-stock-drag!
  [^Stage stage element-name _start-screen-x _start-screen-y end-screen-x end-screen-y]
  (when-let [^Node canvas (hit-test/region-node stage :canvas)]
    (when-let [[stock-x stock-y] (model/stock-position (:diagram @app/*state) element-name)]
      (let [press-cx (+ stock-x (/ model/stock-icon-width 2))
            press-cy (+ stock-y (/ model/stock-icon-height 2))
            [release-cx release-cy] (screen-to-canvas-local canvas end-screen-x end-screen-y)
            press-scene (canvas-local-to-scene canvas press-cx press-cy)
            release-scene (canvas-local-to-scene canvas release-cx release-cy)]
        (app/dispatch-map-event! {:event events/stock-drag-start
                                  :stock-name element-name
                                  :from-canvas true
                                  :canvas-coordinates [(int press-cx) (int press-cy)]
                                  :scene-coordinates press-scene})
        (Thread/sleep 50)
        (app/dispatch-map-event! {:event events/stock-drag
                                  :stock-name element-name
                                  :from-canvas true
                                  :scene-coordinates release-scene})
        (Thread/sleep 50)
        (app/dispatch-map-event! {:event events/stock-drag-end
                                  :stock-name element-name
                                  :from-canvas true
                                  :scene-coordinates release-scene})
        (Thread/sleep 200)))))

(defn- synthesize-converter-drag!
  [^Stage stage element-name _start-screen-x _start-screen-y end-screen-x end-screen-y]
  (when-let [^Node canvas (hit-test/region-node stage :canvas)]
    (when-let [[cx cy] (model/converter-position (:diagram @app/*state) element-name)]
      (let [press-cx (+ cx (/ model/converter-icon-size 2))
            press-cy (+ cy (/ model/converter-icon-size 2))
            [release-cx release-cy] (screen-to-canvas-local canvas end-screen-x end-screen-y)
            press-scene (canvas-local-to-scene canvas press-cx press-cy)
            release-scene (canvas-local-to-scene canvas release-cx release-cy)]
        (app/dispatch-map-event! {:event events/converter-drag-start
                                  :converter-name element-name
                                  :scene-coordinates press-scene})
        (Thread/sleep 50)
        (app/dispatch-map-event! {:event events/converter-drag
                                  :converter-name element-name
                                  :scene-coordinates release-scene})
        (Thread/sleep 50)
        (app/dispatch-map-event! {:event events/converter-drag-end
                                  :converter-name element-name
                                  :scene-coordinates release-scene})
        (Thread/sleep 200)))))

(defn drag-element!
  [^Stage stage kind element-name region & [position]]
  (when-let [diagram (:diagram @app/*state)]
    (when-let [bounds (hit-test/element-bounds stage diagram kind element-name)]
      (when-let [region-center (hit-test/region-center stage region)]
        (let [start-x (+ (:x bounds) (/ (:width bounds) 2.0))
              start-y (+ (:y bounds) (/ (:height bounds) 2.0))
              {:keys [x y]} (resolve-position position region-center)]
          (case kind
            :stock (synthesize-stock-drag! stage element-name start-x start-y x y)
            :converter (synthesize-converter-drag! stage element-name start-x start-y x y)
            (when-let [^Node target (or (hit-test/element-node stage kind element-name)
                                         (hit-test/region-node stage :canvas))]
              (fire-mouse-drag! target start-x start-y x y))))))))

(defn click-in-region!
  [^Stage stage region & [position]]
  (when-let [center (hit-test/region-center stage region)]
    (let [{:keys [x y]} (resolve-position position center)]
      (if (= region :canvas)
        (when-let [^Node canvas (hit-test/region-node stage :canvas)]
          (let [[cx cy] (screen-to-canvas-local canvas x y)]
            (app/dispatch-map-event! {:event events/canvas-click
                                      :coordinates [cx cy]})
            (Thread/sleep 200)))
        (when-let [^Node node (hit-test/region-node stage region)]
          (fire-mouse-click! node x y)
          (Thread/sleep 200))))))

(defn drag-in-region!
  [^Stage stage region start-position end-position]
  (when-let [center (hit-test/region-center stage region)]
    (let [start (resolve-position start-position center)
          end (resolve-position end-position center)]
      (when-let [^Node node (hit-test/region-node stage region)]
        (fire-mouse-drag! node (:x start) (:y start) (:x end) (:y end)))
      (Thread/sleep 200))))

(defn click-step-button!
  [_stage]
  (when-not (fx-nodes/find-by-id-in-windows "step-button")
    (throw (ex-info "Step button not visible" {})))
  (let [done (promise)]
    (fx/on-fx-thread
      (try
        (app/dispatch-map-event! {:event events/simulation-step})
        (deliver done :ok)
        (catch Throwable t
          (deliver done t))))
    (let [result (deref done 5000 :timeout)]
      (cond
        (= result :ok) nil
        (= result :timeout) (throw (ex-info "Timed out dispatching simulation step" {}))
        (instance? Throwable result) (throw result)
        :else (throw (ex-info "Unexpected step dispatch result" {:result result})))))
  (Thread/sleep 300))

(defn right-click-element!
  "Opens an element edit dialog via the same events as the context menu."
  [_stage kind name]
  (case kind
    :stock (when-not (:edit-stock @app/*state)
             (app/dispatch-map-event! {:event events/edit-stock-open :stock-name name}))
    :flow (when-not (:edit-flow @app/*state)
            (app/dispatch-map-event! {:event events/edit-flow-open :flow-name name}))
    :converter (when-not (:edit-converter @app/*state)
                 (app/dispatch-map-event! {:event events/edit-converter-open
                                           :converter-name name}))
    (throw (ex-info "Unsupported element kind for edit dialog"
                    {:kind kind :name name})))
  (Thread/sleep 250))

(defn click-element!
  [^Stage stage kind name]
  (let [mode (get-in @app/*state [:diagram :placement-mode])]
    (cond
      (#{:flow :connector} mode)
      (do (app/dispatch-map-event! {:event events/endpoint-click
                                    :endpoint-kind kind
                                    :endpoint-name name})
          (Thread/sleep 100))

      (= :idle mode)
      (do (app/dispatch-map-event! {:event events/selection-click
                                    :object-kind kind
                                    :object-name name})
          (Thread/sleep 100))

      :else
      (when-let [diagram (:diagram @app/*state)]
        (when-let [{:keys [x y width height]} (hit-test/element-bounds stage diagram kind name)]
          (let [cx (+ x (/ width 2.0))
                cy (+ y (/ height 2.0))
                ^Node target (or (hit-test/element-node stage kind name)
                                 (hit-test/region-node stage :canvas))]
            (fire-mouse-click! target cx cy)
            (Thread/sleep 100)))))))

(defn shift-click-element!
  [^Stage stage kind name]
  (do (app/dispatch-map-event! {:event events/selection-click
                                :object-kind kind
                                :object-name name
                                :shift-key true})
      (Thread/sleep 100)))

(defn- synthesize-marquee-select!
  [^Stage stage start-screen-x start-screen-y end-screen-x end-screen-y]
  (when-let [^Node canvas (hit-test/region-node stage :canvas)]
    (let [[start-cx start-cy] (screen-to-canvas-local canvas start-screen-x start-screen-y)
          [end-cx end-cy] (screen-to-canvas-local canvas end-screen-x end-screen-y)]
      (app/dispatch-map-event! {:event events/marquee-drag-start
                                :from-canvas true
                                :canvas-coordinates [(int start-cx) (int start-cy)]})
      (Thread/sleep 50)
      (app/dispatch-map-event! {:event events/marquee-drag-end
                                :from-canvas true
                                :canvas-coordinates [(int end-cx) (int end-cy)]})
      (Thread/sleep 200))))

(defn marquee-select!
  [^Stage stage region start-position end-position]
  (when-let [center (hit-test/region-center stage region)]
    (let [start (resolve-position start-position center)
          end (resolve-position end-position center)]
      (synthesize-marquee-select! stage (:x start) (:y start) (:x end) (:y end)))))

(defn- press-key!
  [_stage key-code]
  (app/dispatch-map-event! {:event events/scene-key-pressed
                            :key-code (keyword (.getName key-code))})
  (Thread/sleep 100))

(defn press-escape!
  [^Stage stage]
  (press-key! stage KeyCode/ESCAPE))

(defn press-delete!
  [^Stage stage]
  (press-key! stage KeyCode/DELETE))

(defn press-backspace!
  [^Stage stage]
  (press-key! stage KeyCode/BACK_SPACE))

(defn element-selected?
  [_stage kind name]
  (model/selected? (:diagram @app/*state) kind name))

(defn nothing-selected?
  [_stage]
  (model/nothing-selected? (:diagram @app/*state)))

(defn resize-window!
  [^Stage stage target-width target-height]
  (.setWidth stage target-width)
  (.setHeight stage target-height))

(defn quit-app! [^Stage stage]
  (menu-choose! stage "File" "Quit")
  (Thread/sleep 250)
  (when (some #(instance? Stage %) (Window/getWindows))
    (System/setProperty "stella.qa.soft-exit" "true")
    (System/exit 0)))

(defn ensure-app-closed!
  "Best-effort shutdown so headed QA runs do not leave JavaFX processes behind."
  [_stage]
  (try
    (close-menus! (main-stage))
    (catch Throwable _))
  (try
    (app/dispatch-map-event! {:event events/window-close})
    (catch Throwable _))
  (Thread/sleep 100)
  (System/exit 0))

(defn top-level-menu-labels [^Stage stage]
  (when-let [^MenuBar bar (menu-bar stage)]
    (mapv #(.getText ^Menu %) (.getMenus bar))))

(defn window-title [^Stage stage]
  (.getTitle stage))

(defn visible-text [^Stage stage]
  (hit-test/visible-text stage))

(defn dialog-visible? [substring]
  (some (fn [^Window w]
          (when (instance? Stage w)
            (some #(str/includes? % substring)
                  (hit-test/visible-text ^Stage w))))
        (Window/getWindows)))

(defn frontmost-dialog-title []
  (some-> (first (filter #(and (instance? Stage %)
                               (not= "Stella" (.getTitle ^Stage %)))
                         (Window/getWindows)))
          (.getTitle)))

(defn region-bounds [^Stage stage region]
  (hit-test/wait-for-region-bounds stage region 30))

(defn region-text-includes?
  [^Stage stage region substring]
  (or (and (= region :control-panel)
           (when-let [^Label label (fx-nodes/find-by-id-in-windows "simulation-time-display")]
             (str/includes? (.getText label) (str substring))))
      (some #(str/includes? % (str substring))
            (or (hit-test/region-visible-text stage region) []))))

(defn wait-for-region-text!
  [^Stage stage region substring & {:keys [attempts] :or {attempts 30}}]
  (loop [n attempts]
    (if (region-text-includes? stage region substring)
      true
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn simulation-time-label-text
  []
  (when-let [^Label label (fx-nodes/find-by-id-in-windows "simulation-time-display")]
    (.getText label)))

(defn- thermometer-fill-node
  [stock-name]
  (or (fx-nodes/find-stock-thermometer-fill-by-name stock-name)
      (when-let [diagram (:diagram @app/*state)]
        (when-let [{:keys [x y]} (first (filter #(= stock-name (:name %)) (model/stocks diagram)))]
          (when-let [^Parent canvas (fx-nodes/find-by-id-in-windows "canvas")]
            (fx-nodes/find-stock-thermometer-fill canvas stock-name x y))))))

(defn sync-thermometer-fills!
  []
  (app/sync-ui-thermometer-fills!)
  (Thread/sleep 150))

(defn thermometer-fill-width
  [stock-name]
  (when-let [^Rectangle rect (thermometer-fill-node stock-name)]
    (.getWidth rect)))

(defn thermometer-fill-light-blue?
  [stock-name]
  (when-let [^Rectangle rect (thermometer-fill-node stock-name)]
    (let [style (str/lower-case (or (.getStyle rect) ""))]
      (or (str/includes? style "#add8e6")
          (str/includes? style "lightblue")
          (str/includes? style "light blue")))))

(defn wait-for-thermometer-fill-width!
  [stock-name min-width max-width & {:keys [attempts] :or {attempts 30}}]
  (app/sync-ui-thermometer-fills!)
  (loop [n attempts]
    (let [width (or (thermometer-fill-width stock-name) 0.0)]
      (if (and (>= width min-width) (<= width max-width))
        width
        (when (pos? n)
          (app/sync-ui-thermometer-fills!)
          (Thread/sleep 100)
          (recur (dec n)))))))

(defn menu-item-disabled? [^Stage stage menu-label item-label]
  (let [^MenuBar bar (menu-bar stage)
        ^Menu menu (menu-by-label bar menu-label)
        ^MenuItem item (menu-item-by-text menu item-label)]
    (.isDisable item)))

(defn element-visible?
  [^Stage stage kind name]
  (or (some #(str/includes? % name) (visible-text stage))
      (boolean (hit-test/element-bounds stage (:diagram @app/*state) kind name))))

(defn wait-for-element!
  [^Stage stage kind name & {:keys [attempts] :or {attempts 20}}]
  (loop [n attempts]
    (if (element-visible? stage kind name)
      true
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn element-bounds
  [^Stage stage kind name]
  (hit-test/element-bounds stage (:diagram @app/*state) kind name))

(defn- bounds-changed?
  [before after]
  (and before after
       (or (> (Math/abs (- (:x after) (:x before))) 5)
           (> (Math/abs (- (:y after) (:y before))) 5))))

(defn wait-for-bounds-change!
  [^Stage stage kind name initial-bounds & {:keys [attempts] :or {attempts 20}}]
  (loop [n attempts]
    (if (bounds-changed? initial-bounds (element-bounds stage kind name))
      true
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn bounds-unchanged?
  [^Stage stage kind name initial-bounds]
  (not (bounds-changed? initial-bounds (element-bounds stage kind name))))

(defn diagram-stock-count
  [_stage]
  (count (:stocks (:diagram @app/*state))))

(defn diagram-converter-count
  [_stage]
  (count (:converters (:diagram @app/*state))))

(defn- element-icon-labels
  [^Stage stage kind element-name]
  (when-let [^Node label (hit-test/element-node stage kind element-name)]
    (let [^Node icon (or (.getParent label) label)]
      (hit-test/label-texts icon))))

(defn element-icon-label-equals?
  [^Stage stage kind element-name label-text]
  (some #(= (str label-text) %) (or (element-icon-labels stage kind element-name) [])))

(defn element-icon-shows?
  [^Stage stage kind element-name text]
  (some #(str/includes? % (str text)) (or (element-icon-labels stage kind element-name) [])))

(defn label-visible?
  [^Stage stage label-text]
  (some #(= (str label-text) %) (visible-text stage)))

(defn step-button-visible?
  [_stage]
  (boolean (fx-nodes/find-by-id-in-windows "step-button")))

(defn wait-for-label!
  [^Stage stage label-text & {:keys [attempts] :or {attempts 30}}]
  (loop [n attempts]
    (if (label-visible? stage label-text)
      true
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn connector-canvas-formula?
  [connector-name formula]
  (let [diagram (:diagram @app/*state)]
    (= formula (:formula (canvas/connector-canvas-labels diagram connector-name)))))

(defn element-shows?
  [^Stage stage _kind name text]
  (some #(and (str/includes? % name) (str/includes? % (str text)))
        (visible-text stage)))

(defn element-not-shows?
  [^Stage stage kind name text]
  (not (element-shows? stage kind name text)))

(defn- endpoint-bounds
  [stage name]
  (let [diagram (:diagram @app/*state)]
    (or (hit-test/element-bounds stage diagram :stock name)
        (hit-test/element-bounds stage diagram :source name)
        (hit-test/element-bounds stage diagram :sink name)
        (hit-test/element-bounds stage diagram :converter name))))

(defn- link-bounds
  [stage kind name]
  (hit-test/element-bounds stage (:diagram @app/*state) kind name))

(defn directed-between?
  [^Stage stage from-kind from-name to-kind to-name]
  (when-let [from (or (link-bounds stage from-kind from-name)
                      (endpoint-bounds stage from-name))]
    (when-let [to (or (link-bounds stage to-kind to-name)
                      (endpoint-bounds stage to-name))]
      (< (:x from) (:x to)))))

(defn flow-directed?
  [^Stage stage from-name to-name]
  (directed-between? stage :stock from-name :stock to-name))

(defn connector-directed?
  [^Stage stage from-kind from-name to-kind to-name]
  (directed-between? stage from-kind from-name to-kind to-name))
