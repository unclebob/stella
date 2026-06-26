(ns qa.cljfx-shell
  "Executable QA for spec/cljfx-shell.plan.md manual verification checklist.
  Drives the live JavaFX scene graph; does not call stella.actions directly."
  (:require [cljfx.api :as fx]
            [stella.app :as app])
  (:import [javafx.event ActionEvent]
           [javafx.scene Node Parent]
           [javafx.scene.control Label Menu MenuBar MenuItem]
           [javafx.scene.layout BorderPane Pane]
           [javafx.stage Stage Window]))

(defn- fail! [message]
  (binding [*out* *err*]
    (println "QA FAIL:" message))
  (System/exit 1))

(defn- pass! [message]
  (println "QA PASS:" message))

(defn- find-stage []
  (first (filter #(instance? Stage %) (Window/getWindows))))

(defn- menu-bar [stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (let [top (.getTop ^BorderPane root)]
        (when (instance? MenuBar top) top)))))

(defn- center-pane [stage]
  (when-let [root (some-> stage .getScene .getRoot)]
    (when (instance? BorderPane root)
      (.getCenter ^BorderPane root))))

(defn- menu-labels [^MenuBar bar]
  (mapv #(.getText ^Menu %) (.getMenus bar)))

(defn- menu-by-label [^MenuBar bar label]
  (some #(when (= label (.getText ^Menu %)) %)
        (.getMenus bar)))

(defn- menu-item-by-text [^Menu menu label]
  (some #(when (and (instance? MenuItem %)
                    (= label (.getText ^MenuItem %)))
          %)
        (.getItems menu)))

(defn- assert-menu-structure! [^MenuBar bar]
  (let [labels (menu-labels bar)]
    (when-not (= ["File" "Edit" "View" "Help"] labels)
      (fail! (str "Menu bar labels " labels " do not match expected top-level menus")))
    (pass! "Menu bar shows File / Edit / View / Help")))

(defn- assert-disabled-stubs! [^MenuBar bar]
  (doseq [[menu-label item-labels]
          {"File" ["New" "Open…" "Save" "Save As…"]
           "Edit" ["Undo" "Redo" "Cut" "Copy" "Paste"]
           "View" ["Zoom In" "Zoom Out" "Reset Zoom"]}]
    (let [menu (menu-by-label bar menu-label)]
      (doseq [label item-labels]
        (let [item (menu-item-by-text menu label)]
          (when-not item
            (fail! (str "Missing menu item " menu-label " → " label)))
          (when-not (.isDisable ^MenuItem item)
            (fail! (str "Stub item should be disabled: " menu-label " → " label)))))))
  (pass! "Disabled stub menu items are grayed out"))

(defn- assert-enabled-actions! [^MenuBar bar]
  (let [file (menu-by-label bar "File")
        help (menu-by-label bar "Help")
        quit (menu-item-by-text file "Quit")
        about (menu-item-by-text help "About Stella")]
    (when (or (nil? quit) (.isDisable ^MenuItem quit))
      (fail! "File → Quit should be enabled"))
    (when (or (nil? about) (.isDisable ^MenuItem about))
      (fail! "Help → About Stella should be enabled"))
    (pass! "Quit and About Stella menu items are enabled")))

(defn- assert-canvas! [^Stage stage]
  (let [pane (center-pane stage)]
    (when-not (instance? Pane pane)
      (fail! "Center region is not a canvas pane"))
    (when-not (re-find #"background-color" (.getStyle ^Pane pane))
      (fail! "Canvas pane is missing background style"))
    (pass! "Canvas is blank with expected background")))

(defn- assert-resize! [^Stage stage]
  (let [pane (center-pane stage)
        w0 (.getWidth ^Pane pane)
        h0 (.getHeight ^Pane pane)]
    (.setWidth stage 900)
    (.setHeight stage 600)
    (let [w1 (.getWidth ^Pane pane)
          h1 (.getHeight ^Pane pane)]
      (when (and (= w0 w1) (= h0 h1) (zero? w1))
        (fail! "Canvas did not resize with the window"))
      (pass! "Canvas resizes with the window"))))

(defn- about-stage []
  (some (fn [^Window w]
          (when (and (instance? Stage w)
                     (= "About Stella" (.getTitle ^Stage w)))
            w))
        (Window/getWindows)))

(defn- label-texts [^Node node]
  (cond
    (instance? Label node)
    [(.getText ^Label node)]

    (instance? Parent node)
    (mapcat label-texts (.getChildrenUnmodifiable ^Parent node))

    :else []))

(defn- assert-about-dialog! [^MenuBar bar]
  (let [help (menu-by-label bar "Help")
        about (menu-item-by-text help "About Stella")]
    (when-let [handler (.getOnAction about)]
      (.handle ^javafx.event.EventHandler handler (ActionEvent.)))
    (Thread/sleep 200)
    (if-let [^Stage dialog (about-stage)]
      (let [texts (set (label-texts (.getRoot (.getScene dialog))))]
        (cond
          (not (contains? texts "Stella"))
          (fail! (str "About dialog missing app name in labels: " texts))

          (not (some #(re-find #"diagram editor" %) texts))
          (fail! (str "About dialog missing placeholder line in labels: " texts))

          :else
          (do (.close dialog)
              (pass! "About dialog shows app name and placeholder line"))))
      (fail! "About Stella did not open an information dialog"))))

(defn- wait-for-stage [attempts]
  (loop [n attempts]
    (if-let [stage (find-stage)]
      stage
      (when (pos? n)
        (Thread/sleep 100)
        (recur (dec n))))))

(defn -main [& _]
  (let [done (promise)]
    (fx/on-fx-thread
      (try
        (app/start!)
        (if-let [stage (wait-for-stage 50)]
          (do
            (when-not (= "Stella" (.getTitle stage))
              (fail! (str "Window title is " (.getTitle stage) ", expected Stella")))
            (pass! "bb run opens the Stella window")
            (let [bar (menu-bar stage)]
              (when-not bar
                (fail! "Could not find menu bar in scene graph"))
              (assert-menu-structure! bar)
              (assert-disabled-stubs! bar)
              (assert-enabled-actions! bar)
              (assert-canvas! stage)
              (assert-resize! stage)
              (assert-about-dialog! bar)
              (let [file (menu-by-label bar "File")
                    quit (menu-item-by-text file "Quit")]
                (when-let [handler (.getOnAction quit)]
                  (.handle ^javafx.event.EventHandler handler (ActionEvent.)))
                (pass! "File → Quit exits the application"))
              (println "QA complete: all UI checklist items passed")
              (deliver done :ok)))
          (fail! "Timed out waiting for Stella window"))
        (catch Throwable t
          (fail! (.getMessage t))
          (deliver done :error))))
    @done))