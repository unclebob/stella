(ns stella.qa.ui-driver
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [stella.app :as app]
            [stella.events :as events]
            [stella.fx.nodes :as fx-nodes]
            [stella.qa.hit-test :as hit-test])
  (:import [javafx.event ActionEvent EventHandler]
           [javafx.geometry Bounds]
           [javafx.scene Node Parent]
           [javafx.scene.control Button Menu MenuBar MenuItem]
           [javafx.scene.input MouseButton MouseEvent]
           [javafx.scene.layout BorderPane VBox]
           [javafx.scene.robot Robot]
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

(defn menu-bar [^Stage stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (let [top (.getTop ^BorderPane root)]
        (when (instance? MenuBar top) top)))))

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

(defn click-ok-on-front-dialog! []
  (when-let [^Stage dialog (first (filter #(and (instance? Stage %)
                                                 (= "About Stella" (.getTitle ^Stage %)))
                                          (Window/getWindows)))]
    (when-let [^Button ok (find-ok-button (.getRoot (.getScene dialog)))]
      (.fire ok))))

(defn- palette-pane [^Stage stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (.getLeft ^BorderPane root))))

(defn- find-button-by-text [^Node node text]
  (fx-nodes/find-child
   node
   #(and (instance? Button %) (= text (.getText ^Button %)))))

(defn- robot-click!
  [screen-x screen-y]
  (let [^Robot robot (Robot.)
        buttons (into-array MouseButton [MouseButton/PRIMARY])]
    (.mouseMove robot screen-x screen-y)
    (Thread/sleep 50)
    (.mousePress robot buttons)
    (.mouseRelease robot buttons)
    (Thread/sleep 100)))



(defn click-palette!
  [^Stage stage label]
  (when-let [palette (palette-pane stage)]
    (when-let [^Button button (find-button-by-text palette label)]
      (if-let [handler (.getOnAction button)]
        (.handle ^javafx.event.EventHandler handler (ActionEvent.))
        (let [{:keys [x y]} (hit-test/node-screen-center button)]
          (robot-click! x y)))
      (Thread/sleep 250))))

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

(defn- synthesize-mouse-click!
  [^Node node screen-x screen-y]
  (let [local (.screenToLocal node screen-x screen-y)
        lx (.getX local)
        ly (.getY local)
        ^MouseEvent event (MouseEvent. node node
                                      MouseEvent/MOUSE_CLICKED
                                      lx ly screen-x screen-y
                                      MouseButton/PRIMARY 1
                                      false false false true false false false false false false
                                      nil)
        ^EventHandler handler (.getOnMouseClicked node)]
    (when handler
      (.handle handler event))))

(defn click-in-region!
  [^Stage stage region & [position]]
  (when-let [^Node node (hit-test/region-node stage region)]
    (when-let [center (hit-test/region-center stage region)]
      (let [{:keys [x y]} (resolve-position position center)]
        (if-let [handler (.getOnMouseClicked node)]
          (synthesize-mouse-click! node x y)
          (robot-click! x y))))))

(defn click-element!
  [^Stage stage kind name]
  (if (= :flow (get-in @app/*state [:diagram :placement-mode]))
    (do (app/dispatch-map-event! {:event events/endpoint-click
                                  :endpoint-kind kind
                                  :endpoint-name name})
        (Thread/sleep 100))
    (when-let [diagram (:diagram @app/*state)]
      (when-let [{:keys [x y width height]} (hit-test/element-bounds stage diagram kind name)]
        (let [cx (+ x (/ width 2.0))
              cy (+ y (/ height 2.0))
              ^Node target (or (hit-test/element-node stage kind name)
                               (hit-test/region-node stage :canvas))]
          (if-let [handler (when target (.getOnMouseClicked target))]
            (synthesize-mouse-click! target cx cy)
            (robot-click! cx cy)))))))

(defn resize-window!
  [^Stage stage target-width target-height]
  (.setWidth stage target-width)
  (.setHeight stage target-height))

(defn quit-app! [^Stage stage]
  (menu-choose! stage "File" "Quit"))

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

(defn element-shows?
  [^Stage stage _kind name text]
  (some #(and (str/includes? % name) (str/includes? % (str text)))
        (visible-text stage)))

(defn- endpoint-bounds
  [stage name]
  (let [diagram (:diagram @app/*state)]
    (or (hit-test/element-bounds stage diagram :stock name)
        (hit-test/element-bounds stage diagram :source name)
        (hit-test/element-bounds stage diagram :sink name))))

(defn flow-directed?
  [^Stage stage from-name to-name]
  (when-let [from (endpoint-bounds stage from-name)]
    (when-let [to (endpoint-bounds stage to-name)]
      (< (:x from) (:x to)))))