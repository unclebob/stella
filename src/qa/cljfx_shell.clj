(ns qa.cljfx-shell
  "Executable QA suites mapped to qa/procedures/*.qa.md.
  Drives the live JavaFX app through stella.qa.ui-driver only."
  (:require [cljfx.api :as fx]
            [stella.qa.ui-driver :as ui])
  (:import [javafx.stage Stage Window]))

(defn- fail! [message]
  (binding [*out* *err*]
    (println "QA FAIL:" message))
  (System/exit 1))

(defn- pass! [suite message]
  (println (str "QA PASS [" suite "]: " message)))

(defn- run-suite! [suite-name thunk]
  (println (str "QA SUITE: " suite-name))
  (try
    (thunk)
    (println (str "QA SUITE PASS: " suite-name))
    (catch Throwable t
      (fail! (str suite-name " - " (.getMessage t))))))

(defn- with-app! [{:keys [hard-exit? width height] :as opts} f]
  (let [done (promise)]
    (fx/on-fx-thread
      (try
        (ui/launch-app! :width width :height height :hard-exit? hard-exit?)
        (if-let [^Stage stage (ui/wait-for-stage 50)]
          (do (f stage) (deliver done :ok))
          (do (fail! "Timed out waiting for Stella window")
              (deliver done :error)))
        (catch Throwable t
          (fail! (.getMessage t))
          (deliver done :error))))
    (case (deref done 120000 :timeout)
      :ok nil
      (fail! "QA app session timed out"))))

(defn- run-shell-launch! []
  (with-app! {}
    (fn [^Stage stage]
      (when-not (= "Stella" (ui/window-title stage))
        (fail! (str "Window title is " (ui/window-title stage))))
      (pass! "shell-launch" "Window title is Stella")
      (when-not (= ["File" "Edit" "View" "Help"] (ui/top-level-menu-labels stage))
        (fail! (str "Unexpected menus: " (ui/top-level-menu-labels stage))))
      (pass! "shell-launch" "Top-level menus are visible")
      (when-not (ui/region-bounds stage :canvas)
        (fail! "Canvas region :canvas is not visible"))
      (pass! "shell-launch" "Diagram canvas region is visible")
      (ui/quit-app! stage)
      (pass! "shell-launch" "File → Quit requested"))))

(defn- run-shell-menus! []
  (with-app! {}
    (fn [^Stage stage]
      (ui/open-menu! stage "File")
      (when-not (ui/menu-item-disabled? stage "File" "New")
        (fail! "New should be disabled"))
      (pass! "shell-menus" "New is disabled")
      (when (ui/menu-item-disabled? stage "File" "Quit")
        (fail! "Quit should be enabled"))
      (pass! "shell-menus" "Quit is enabled")
      (ui/close-menus! stage)
      (ui/open-menu! stage "Help")
      (when (ui/menu-item-disabled? stage "Help" "About Stella")
        (fail! "About Stella should be enabled"))
      (pass! "shell-menus" "About Stella is enabled")
      (ui/close-menus! stage)
      (ui/quit-app! stage)
      (pass! "shell-menus" "Quit requested"))))

(defn- run-shell-about! []
  (with-app! {}
    (fn [^Stage stage]
      (ui/menu-choose! stage "Help" "About Stella")
      (loop [n 20]
        (if (ui/dialog-visible? "diagram editor")
          nil
          (when (pos? n)
            (Thread/sleep 100)
            (recur (dec n)))))
      (when-not (ui/dialog-visible? "diagram editor")
        (fail! "About dialog did not appear"))
      (pass! "shell-about" "About dialog text is visible")
      (when-not (= "About Stella" (ui/frontmost-dialog-title))
        (fail! (str "Unexpected front dialog: " (ui/frontmost-dialog-title))))
      (pass! "shell-about" "About dialog is frontmost")
      (ui/click-ok-on-front-dialog!)
      (Thread/sleep 200)
      (when (ui/dialog-visible? "diagram editor")
        (ui/close-menus! stage))
      (when (ui/dialog-visible? "diagram editor")
        (fail! "About dialog did not dismiss"))
      (pass! "shell-about" "About dialog dismissed")
      (when-not (= "Stella" (ui/window-title stage))
        (fail! "Main window title changed after About"))
      (pass! "shell-about" "Main window remains open")
      (ui/quit-app! stage)
      (pass! "shell-about" "Quit requested"))))

(defn- run-shell-resize! []
  (with-app! {:width 800 :height 600}
    (fn [^Stage stage]
      (when-not (ui/region-bounds stage :canvas)
        (fail! "Canvas region is not visible"))
      (pass! "shell-resize" "Canvas visible before resize")
      (let [w0 (.getWidth stage)
            h0 (.getHeight stage)]
        (ui/resize-window! stage 950 700)
        (Thread/sleep 500)
        (when-not (ui/region-bounds stage :canvas)
          (fail! "Canvas region missing after resize"))
        (when-not (and (> (.getWidth stage) w0) (> (.getHeight stage) h0))
          (fail! (str "Window did not grow: " w0 "x" h0 " -> "
                      (.getWidth stage) "x" (.getHeight stage))))
        (pass! "shell-resize" "Window and canvas region grew after resize")
        (ui/click-in-region! stage :canvas :center)
        (pass! "shell-resize" "Canvas center click succeeded")
        (when-not (= "Stella" (ui/window-title stage))
          (fail! "Main window closed after canvas click"))
        (pass! "shell-resize" "Main window still showing")
        (ui/quit-app! stage)
        (pass! "shell-resize" "Quit requested")))))

(defn- run-shell-quit! []
  (with-app! {}
    (fn [^Stage stage]
      (when-not (.isShowing stage)
        (fail! "Main window is not visible"))
      (pass! "shell-quit" "Main window is visible")
      (ui/menu-choose! stage "File" "Quit")
      (Thread/sleep 200)
      (when (.isShowing stage)
        (fail! "Main window still visible after Quit"))
      (pass! "shell-quit" "File → Quit closes the window")
      (System/exit 0))))

(def ^:private suites
  {"shell-launch" run-shell-launch!
   "shell-menus" run-shell-menus!
   "shell-about" run-shell-about!
   "shell-resize" run-shell-resize!
   "shell-quit" run-shell-quit!})

(defn -main [& args]
  (when-not (first args)
    (fail! "Usage: clojure -M:qa <suite-name>"))
  (let [suite (first args)]
    (if-let [run (get suites suite)]
      (run-suite! suite (fn [] (run)))
      (fail! (str "Unknown suite: " suite)))))