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

(defn- run-place-stock! []
  (with-app! {}
    (fn [^Stage stage]
      (when-not (ui/region-bounds stage :canvas)
        (fail! "Canvas region :canvas is not visible"))
      (pass! "place-stock" "Canvas region is visible")
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas :center)
      (when-not (ui/wait-for-element! stage :stock "Stock1")
        (fail! "Stock1 did not appear"))
      (when-not (ui/element-shows? stage :stock "Stock1" "Stock1")
        (fail! "Stock1 name not visible"))
      (when-not (ui/element-shows? stage :stock "Stock1" "0")
        (fail! "Stock1 initial value not visible"))
      (pass! "place-stock" "Stock1 placed with name and value 0")
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas [100 50])
      (when-not (ui/wait-for-element! stage :stock "Stock2")
        (fail! "Stock2 did not appear"))
      (when-not (ui/element-shows? stage :stock "Stock2" "Stock2")
        (fail! "Stock2 name not visible"))
      (when-not (ui/element-shows? stage :stock "Stock2" "0")
        (fail! "Stock2 initial value not visible"))
      (pass! "place-stock" "Stock2 placed with name and value 0")
      (ui/quit-app! stage)
      (pass! "place-stock" "Quit requested"))))

(defn- place-two-stocks! [^Stage stage]
  (ui/click-palette! stage "Stock")
  (ui/click-in-region! stage :canvas :center)
  (when-not (ui/wait-for-element! stage :stock "Stock1")
    (fail! "Stock1 not placed"))
  (ui/click-palette! stage "Stock")
  (ui/click-in-region! stage :canvas [150 50])
  (when-not (ui/wait-for-element! stage :stock "Stock2")
    (fail! "Stock2 not placed")))

(defn- assert-flow! [^Stage stage flow-name from-name to-name & {:keys [suite show-name?]
                                                                 :or {show-name? true}}]
  (when-not (ui/wait-for-element! stage :flow flow-name)
    (fail! (str flow-name " did not appear")))
  (when show-name?
    (when-not (ui/element-shows? stage :flow flow-name flow-name)
      (fail! (str flow-name " name not visible"))))
  (when-not (ui/element-shows? stage :flow flow-name "0")
    (fail! (str flow-name " rate not visible")))
  (when-not (ui/flow-directed? stage from-name to-name)
    (fail! (str flow-name " direction from " from-name " to " to-name " not visible")))
  (when suite
    (pass! suite (str flow-name " connects " from-name " to " to-name
                      (when show-name? " with rate 0")))))

(defn- run-connect-flow! []
  (with-app! {}
    (fn [^Stage stage]
      (place-two-stocks! stage)
      (ui/click-palette! stage "Flow")
      (ui/click-element! stage :stock "Stock1")
      (ui/click-element! stage :stock "Stock2")
      (assert-flow! stage "Flow1" "Stock1" "Stock2" :suite "connect-flow")
      (ui/quit-app! stage)
      (pass! "connect-flow" "Quit requested"))))

(defn- run-cloud-endpoints! []
  (with-app! {}
    (fn [^Stage stage]
      (ui/click-palette! stage "Source")
      (ui/click-in-region! stage :canvas [-150 0])
      (when-not (ui/wait-for-element! stage :source "Source1")
        (fail! "Source1 did not appear"))
      (pass! "cloud-endpoints" "Source1 placed")
      (ui/click-palette! stage "Sink")
      (ui/click-in-region! stage :canvas [150 0])
      (when-not (ui/wait-for-element! stage :sink "Sink1")
        (fail! "Sink1 did not appear"))
      (pass! "cloud-endpoints" "Sink1 placed")
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas :center)
      (when-not (ui/wait-for-element! stage :stock "Stock1")
        (fail! "Stock1 did not appear"))
      (pass! "cloud-endpoints" "Stock1 placed")
      (ui/click-palette! stage "Flow")
      (ui/click-element! stage :source "Source1")
      (ui/click-element! stage :stock "Stock1")
      (assert-flow! stage "Flow1" "Source1" "Stock1"
                    :suite "cloud-endpoints" :show-name? false)
      (ui/click-palette! stage "Flow")
      (ui/click-element! stage :stock "Stock1")
      (ui/click-element! stage :sink "Sink1")
      (assert-flow! stage "Flow2" "Stock1" "Sink1"
                    :suite "cloud-endpoints" :show-name? false)
      (ui/quit-app! stage)
      (pass! "cloud-endpoints" "Quit requested"))))

(defn- run-connectors! []
  (with-app! {}
    (fn [^Stage stage]
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas [-100 0])
      (when-not (ui/wait-for-element! stage :stock "Stock1")
        (fail! "Stock1 not placed"))
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas [100 0])
      (when-not (ui/wait-for-element! stage :stock "Stock2")
        (fail! "Stock2 not placed"))
      (ui/click-palette! stage "Flow")
      (ui/click-element! stage :stock "Stock1")
      (ui/click-element! stage :stock "Stock2")
      (assert-flow! stage "Flow1" "Stock1" "Stock2" :suite "connectors")
      (ui/click-palette! stage "Converter")
      (ui/click-in-region! stage :canvas [0 100])
      (when-not (ui/wait-for-element! stage :converter "Converter1")
        (fail! "Converter1 did not appear"))
      (when-not (ui/element-shows? stage :converter "Converter1" "0")
        (fail! "Converter1 value not visible"))
      (pass! "connectors" "Converter1 placed with value 0")
      (ui/click-palette! stage "Connector")
      (ui/click-element! stage :converter "Converter1")
      (ui/click-element! stage :flow "Flow1")
      (when-not (ui/wait-for-element! stage :connector "Connector1")
        (fail! "Connector1 did not appear"))
      (pass! "connectors" "Connector1 connects Converter1 to Flow1")
      (ui/click-palette! stage "Connector")
      (ui/click-element! stage :stock "Stock1")
      (ui/click-element! stage :converter "Converter1")
      (when-not (ui/wait-for-element! stage :connector "Connector2")
        (fail! "Connector2 did not appear"))
      (when-not (ui/connector-directed? stage :stock "Stock1" :converter "Converter1")
        (fail! "Connector2 direction from Stock1 to Converter1 not visible"))
      (pass! "connectors" "Connector2 connects Stock1 to Converter1")
      (ui/quit-app! stage)
      (pass! "connectors" "Quit requested"))))

(defn- run-edit-stock! []
  (with-app! {}
    (fn [^Stage stage]
      (when-not (ui/region-bounds stage :canvas)
        (fail! "Canvas region :canvas is not visible"))
      (pass! "edit-stock" "Canvas region is visible")
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas :center)
      (when-not (ui/wait-for-element! stage :stock "Stock1")
        (fail! "Stock1 did not appear"))
      (when-not (ui/element-shows? stage :stock "Stock1" "Stock1")
        (fail! "Stock1 name not visible"))
      (when-not (ui/element-shows? stage :stock "Stock1" "0")
        (fail! "Stock1 minimum not visible"))
      (ui/right-click-element! stage :stock "Stock1")
      (when-not (ui/wait-for-dialog! "Edit Stock" :attempts 50)
        (fail! "Edit Stock dialog did not appear"))
      (ui/type-into-dialog-field! "Name" "Cats")
      (ui/click-ok-on-dialog! "Edit Stock")
      (when-not (ui/wait-for-element! stage :stock "Cats")
        (fail! "Cats did not appear after rename"))
      (when-not (ui/element-shows? stage :stock "Cats" "Cats")
        (fail! "Cats name not visible"))
      (when-not (ui/element-shows? stage :stock "Cats" "0")
        (fail! "Cats minimum not visible"))
      (pass! "edit-stock" "Stock renamed to Cats")
      (ui/right-click-element! stage :stock "Cats")
      (when-not (ui/wait-for-dialog! "Edit Stock")
        (fail! "Edit Stock dialog did not reappear"))
      (ui/type-into-dialog-field! "Initial value" "25")
      (ui/type-into-dialog-field! "Minimum" "5")
      (ui/type-into-dialog-field! "Maximum" "100")
      (ui/click-ok-on-dialog! "Edit Stock")
      (when-not (ui/element-shows? stage :stock "Cats" "Cats")
        (fail! "Cats name missing after bounds edit"))
      (when-not (ui/element-shows? stage :stock "Cats" "5")
        (fail! "Cats minimum not visible after edit"))
      (when-not (ui/element-shows? stage :stock "Cats" "100")
        (fail! "Cats maximum not visible after edit"))
      (when-not (ui/element-not-shows? stage :stock "Cats" "25")
        (fail! "Initial value 25 shown on icon"))
      (pass! "edit-stock" "Cats bounds updated; initial value not on icon")
      (ui/click-palette! stage "Stock")
      (ui/click-in-region! stage :canvas [120 40])
      (when-not (ui/wait-for-element! stage :stock "Stock2")
        (fail! "Stock2 did not appear"))
      (ui/right-click-element! stage :stock "Cats")
      (when-not (ui/wait-for-dialog! "Edit Stock")
        (fail! "Edit Stock dialog missing for duplicate rename"))
      (ui/type-into-dialog-field! "Name" "Stock2")
      (ui/click-ok-on-dialog! "Edit Stock")
      (when-not (ui/element-visible? stage :stock "Cats")
        (fail! "Cats disappeared after rejected duplicate rename"))
      (when-not (ui/element-visible? stage :stock "Stock2")
        (fail! "Stock2 missing after rejected duplicate rename"))
      (pass! "edit-stock" "Duplicate rename rejected")
      (ui/quit-app! stage)
      (pass! "edit-stock" "Quit requested"))))

(def ^:private suites
  {"shell-launch" run-shell-launch!
   "shell-menus" run-shell-menus!
   "shell-about" run-shell-about!
   "shell-resize" run-shell-resize!
   "shell-quit" run-shell-quit!
   "place-stock" run-place-stock!
   "connect-flow" run-connect-flow!
   "cloud-endpoints" run-cloud-endpoints!
   "connectors" run-connectors!
   "edit-stock" run-edit-stock!})

(defn -main [& args]
  (when-not (first args)
    (fail! "Usage: clojure -M:qa <suite-name>"))
  (let [suite (first args)]
    (if-let [run (get suites suite)]
      (run-suite! suite (fn [] (run)))
      (fail! (str "Unknown suite: " suite)))))