(ns stella.qa.ui-driver
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [stella.app :as app]
            [stella.qa.hit-test :as hit-test])
  (:import [javafx.event ActionEvent]
           [javafx.scene Node Parent]
           [javafx.scene.control Button Menu MenuBar MenuItem]
           [javafx.scene.layout BorderPane]
           [javafx.stage Stage Window]))

(defn launch-app!
  [& {:keys [width height hard-exit?] :as _opts}]
  (when width
    (System/setProperty "stella.qa.width" (str width)))
  (when height
    (System/setProperty "stella.qa.height" (str height)))
  (System/setProperty "stella.qa.soft-exit" (if hard-exit? "false" "true"))
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

(defn click-in-region!
  [^Stage stage region & [_position]]
  (when-let [^Node node (hit-test/region-node stage region)]
    (.requestFocus node)))

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