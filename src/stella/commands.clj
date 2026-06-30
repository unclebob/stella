(ns stella.commands
  (:require [clojure.string :as str]
            [stella.model :as model]
            [stella.simulation :as simulation]))

(defn default-shell!
  [_]
  (model/default-shell))

(defn default-diagram!
  [_]
  (model/default-diagram))

(defn show-about!
  [shell]
  (-> shell
      (assoc :about-visible true)
      (assoc :about-text "Stella\nA system dynamics diagram editor.")))

(defn quit!
  [shell]
  (assoc shell :showing false))

(defn clear-canvas-preview-on-shell!
  [shell]
  (dissoc shell :canvas-preview))

(defn update-canvas-preview-on-shell!
  [shell [x y]]
  (let [mode (get-in shell [:diagram :placement-mode])
        {:keys [min-x min-y]} (model/preview-anchor-insets mode)
        preview {:x (max min-x x) :y (max min-y y)}]
    (if (= preview (:canvas-preview shell))
      shell
      (assoc shell :canvas-preview preview))))

(defn- update-diagram-and-clear-preview
  [shell f]
  (-> shell
      (update :diagram f)
      clear-canvas-preview-on-shell!))

(defn arm-stock-placement!
  [diagram]
  (model/arm-stock-placement diagram))

(defn place-stock!
  [diagram x y]
  (model/place-stock diagram x y))

(defn fixture-stock!
  [diagram name x y]
  (model/fixture-stock diagram name x y))

(defn move-stock!
  [diagram name x y]
  (model/move-stock diagram name x y))

(defn move-stock-on-shell!
  [shell name x y]
  (update shell :diagram #(move-stock! % name x y)))

(defn move-source!
  [diagram name x y]
  (model/move-source diagram name x y))

(defn move-sink!
  [diagram name x y]
  (model/move-sink diagram name x y))

(defn move-cloud-on-shell!
  [shell kind name x y]
  (case kind
    :source (update shell :diagram #(move-source! % name x y))
    :sink (update shell :diagram #(move-sink! % name x y))
    shell))

(defn- resolve-stock-drag-name
  [shell {:keys [stock-name canvas-coordinates]}]
  (or stock-name
      (when canvas-coordinates
        (let [[cx cy] canvas-coordinates]
          (model/stock-at-canvas-point (:diagram shell) cx cy)))))

(defn start-stock-drag-on-shell!
  [shell event]
  (let [stock-name (resolve-stock-drag-name shell event)
        scene-coordinates (:scene-coordinates event)]
    (if (and (= :idle (get-in shell [:diagram :placement-mode]))
             stock-name
             scene-coordinates
             (model/stock-exists? (:diagram shell) stock-name))
      (let [[x y] (model/stock-position (:diagram shell) stock-name)
            [sx sy] scene-coordinates]
        (assoc shell :stock-drag {:name stock-name
                                  :start-x x
                                  :start-y y
                                  :press-scene-x sx
                                  :press-scene-y sy}))
      shell)))

(defn- pointer-drag-position
  [drag press-x-key press-y-key [rx ry]]
  [(+ (:start-x drag) (- rx (press-x-key drag)))
   (+ (:start-y drag) (- ry (press-y-key drag)))])

(defn- scene-drag-position
  [drag coordinates]
  (pointer-drag-position drag :press-scene-x :press-scene-y coordinates))

(defn drag-stock-on-shell!
  [shell {:keys [scene-coordinates]}]
  (if-let [drag (:stock-drag shell)]
    (if scene-coordinates
      (let [stock-name (:name drag)
            [new-x new-y] (scene-drag-position drag scene-coordinates)]
        (move-stock-on-shell! shell stock-name new-x new-y))
      shell)
    shell))

(defn end-stock-drag-on-shell!
  [shell event]
  (if (:stock-drag shell)
    (-> shell
        (drag-stock-on-shell! event)
        (dissoc :stock-drag))
    shell))

(defn- resolve-cloud-drag-target
  [shell {:keys [cloud-kind cloud-name canvas-coordinates]}]
  (or (when (and cloud-kind cloud-name)
        {:kind cloud-kind :name cloud-name})
      (when canvas-coordinates
        (let [[cx cy] canvas-coordinates]
          (model/cloud-at-canvas-point (:diagram shell) cx cy)))))

(defn- cloud-exists?
  [diagram kind name]
  (case kind
    :source (model/source-exists? diagram name)
    :sink (model/sink-exists? diagram name)
    false))

(defn- cloud-position
  [diagram kind name]
  (case kind
    :source (model/source-position diagram name)
    :sink (model/sink-position diagram name)
    nil))

(defn start-cloud-drag-on-shell!
  [shell event]
  (let [{:keys [kind name]} (resolve-cloud-drag-target shell event)
        scene-coordinates (:scene-coordinates event)]
    (if (and (= :idle (get-in shell [:diagram :placement-mode]))
             kind
             name
             scene-coordinates
             (cloud-exists? (:diagram shell) kind name))
      (let [[x y] (cloud-position (:diagram shell) kind name)
            [sx sy] scene-coordinates]
        (assoc shell :cloud-drag {:kind kind
                                  :name name
                                  :start-x x
                                  :start-y y
                                  :press-scene-x sx
                                  :press-scene-y sy}))
      shell)))

(defn drag-cloud-on-shell!
  [shell {:keys [scene-coordinates]}]
  (if-let [drag (:cloud-drag shell)]
    (if scene-coordinates
      (let [[new-x new-y] (scene-drag-position drag scene-coordinates)]
        (move-cloud-on-shell! shell (:kind drag) (:name drag) new-x new-y))
      shell)
    shell))

(defn end-cloud-drag-on-shell!
  [shell event]
  (if (:cloud-drag shell)
    (-> shell
        (drag-cloud-on-shell! event)
        (dissoc :cloud-drag))
    shell))

(defn move-converter!
  [diagram name x y]
  (model/move-converter diagram name x y))

(defn move-converter-on-shell!
  [shell name x y]
  (update shell :diagram #(move-converter! % name x y)))

(defn move-connector-handle!
  [diagram connector-name x y]
  (model/move-connector-handle diagram connector-name x y))

(defn move-connector-handle-on-shell!
  [shell connector-name x y]
  (update shell :diagram #(move-connector-handle! % connector-name x y)))

(defn- resolve-converter-drag-name
  [shell {:keys [converter-name canvas-coordinates]}]
  (or converter-name
      (when canvas-coordinates
        (let [[cx cy] canvas-coordinates]
          (model/converter-at-canvas-point (:diagram shell) cx cy)))))

(defn start-converter-drag-on-shell!
  [shell event]
  (let [converter-name (resolve-converter-drag-name shell event)
        scene-coordinates (:scene-coordinates event)]
    (if (and (= :idle (get-in shell [:diagram :placement-mode]))
             converter-name
             scene-coordinates
             (model/converter-exists? (:diagram shell) converter-name))
      (let [[x y] (model/converter-position (:diagram shell) converter-name)
            [sx sy] scene-coordinates]
        (assoc shell :converter-drag {:name converter-name
                                      :start-x x
                                      :start-y y
                                      :press-scene-x sx
                                      :press-scene-y sy}))
      shell)))

(defn drag-converter-on-shell!
  [shell {:keys [scene-coordinates]}]
  (if-let [drag (:converter-drag shell)]
    (if scene-coordinates
      (let [converter-name (:name drag)
            [new-x new-y] (scene-drag-position drag scene-coordinates)]
        (move-converter-on-shell! shell converter-name new-x new-y))
      shell)
    shell))

(defn end-converter-drag-on-shell!
  [shell event]
  (if (:converter-drag shell)
    (-> shell
        (drag-converter-on-shell! event)
        (dissoc :converter-drag))
    shell))

(defn start-connector-control-drag-on-shell!
  [shell {:keys [connector-name canvas-coordinates]}]
  (if (and (= :idle (get-in shell [:diagram :placement-mode]))
           connector-name
           canvas-coordinates
           (model/connector-exists? (:diagram shell) connector-name))
    (if-let [[handle-x handle-y] (model/connector-handle-position (:diagram shell) connector-name)]
      (let [[cx cy] canvas-coordinates]
        (assoc shell :connector-control-drag {:name connector-name
                                              :start-x handle-x
                                              :start-y handle-y
                                              :press-canvas-x cx
                                              :press-canvas-y cy}))
      shell)
    shell))

(defn- canvas-drag-position
  [drag coordinates]
  (pointer-drag-position drag :press-canvas-x :press-canvas-y coordinates))

(defn drag-connector-control-on-shell!
  [shell {:keys [canvas-coordinates]}]
  (if-let [drag (:connector-control-drag shell)]
    (if canvas-coordinates
      (let [[new-x new-y] (canvas-drag-position drag canvas-coordinates)]
        (move-connector-handle-on-shell! shell (:name drag) new-x new-y))
      shell)
    shell))

(defn end-connector-control-drag-on-shell!
  [shell event]
  (if (:connector-control-drag shell)
    (-> shell
        (drag-connector-control-on-shell! event)
        (dissoc :connector-control-drag))
    shell))

(defn click-select!
  [diagram kind name]
  (model/click-select diagram kind name))

(defn click-select-at!
  [diagram x y]
  (model/click-select-at diagram x y))

(defn shift-click-select!
  [diagram kind name]
  (model/shift-click-select diagram kind name))

(defn shift-click-select-at!
  [diagram x y]
  (model/shift-click-select-at diagram x y))

(defn marquee-select!
  [diagram x1 y1 x2 y2]
  (model/marquee-select diagram x1 y1 x2 y2))

(defn clear-selection!
  [diagram]
  (model/clear-selection diagram))

(defn delete-selection!
  [diagram]
  (model/delete-selection diagram))

(defn fixture-stock-connector!
  [diagram connector-name from-stock to-converter]
  (model/fixture-stock-connector diagram connector-name from-stock to-converter))

(defn click-select-on-shell!
  [shell kind name]
  (update shell :diagram #(click-select! % kind name)))

(defn click-select-at-on-shell!
  [shell x y]
  (update shell :diagram #(click-select-at! % x y)))

(defn shift-click-select-on-shell!
  [shell kind name]
  (update shell :diagram #(shift-click-select! % kind name)))

(defn shift-click-select-at-on-shell!
  [shell x y]
  (update shell :diagram #(shift-click-select-at! % x y)))

(defn marquee-select-on-shell!
  [shell x1 y1 x2 y2]
  (update shell :diagram #(marquee-select! % x1 y1 x2 y2)))

(defn clear-selection-on-shell!
  [shell]
  (update shell :diagram clear-selection!))

(defn cancel-on-escape-on-shell!
  [shell]
  (-> shell
      (update-diagram-and-clear-preview model/disarm-placement)
      clear-selection-on-shell!))

(defn delete-selection-on-shell!
  [shell]
  (update shell :diagram delete-selection!))

(defn start-marquee-drag-on-shell!
  [shell {:keys [canvas-coordinates]}]
  (if (and (= :idle (get-in shell [:diagram :placement-mode]))
           canvas-coordinates
           (not (:stock-drag shell))
           (not (:converter-drag shell))
           (not (:cloud-drag shell))
           (not (:connector-control-drag shell)))
    (let [[cx cy] canvas-coordinates
          diagram (:diagram shell)]
      (if (model/object-at-canvas-point diagram cx cy)
        shell
        (assoc shell :marquee-drag {:start-x cx :start-y cy})))
    shell))

(defn drag-marquee-on-shell!
  [shell {:keys [canvas-coordinates]}]
  (if (and (:marquee-drag shell) canvas-coordinates)
    (let [[cx cy] canvas-coordinates]
      (-> shell
          (assoc-in [:marquee-drag :current-x] cx)
          (assoc-in [:marquee-drag :current-y] cy)))
    shell))

(defn end-marquee-drag-on-shell!
  [shell {:keys [canvas-coordinates]}]
  (if-let [drag (:marquee-drag shell)]
    (if canvas-coordinates
      (let [[ex ey] canvas-coordinates]
        (-> shell
            (marquee-select-on-shell! (:start-x drag) (:start-y drag) ex ey)
            (dissoc :marquee-drag)))
      (dissoc shell :marquee-drag))
    shell))

(defn set-stock-name!
  [diagram old-name new-name]
  (model/set-stock-name diagram old-name new-name))

(defn set-stock-initial-value!
  [diagram name value]
  (model/set-stock-initial-value diagram name value))

(defn set-stock-min!
  [diagram name min-value]
  (model/set-stock-min diagram name min-value))

(defn set-stock-max!
  [diagram name max-value]
  (model/set-stock-max diagram name max-value))

(defn clear-stock-max!
  [diagram name]
  (model/clear-stock-max diagram name))

(defn apply-stock-edit!
  [diagram stock-name draft]
  (model/apply-stock-edit diagram stock-name draft))

(defn- stock-edit-draft
  [diagram stock-name]
  (when (model/stock-exists? diagram stock-name)
    (let [stock (first (filter #(= stock-name (:name %)) (model/stocks diagram)))]
      {:stock-name stock-name
       :name (:name stock)
       :initial-value (:initial-value stock)
       :min-value (:min-value stock "0")
       :max-value (or (:max-value stock) "")})))

(defn open-edit-stock-on-shell!
  [shell stock-name]
  (if-let [draft (stock-edit-draft (:diagram shell) stock-name)]
    (assoc shell :edit-stock draft)
    shell))

(defn cancel-edit-stock-on-shell!
  [shell]
  (dissoc shell :edit-stock))

(defn apply-edit-stock-on-shell!
  [shell draft]
  (let [stock-name (:stock-name (:edit-stock shell))
        diagram (:diagram shell)
        updated (apply-stock-edit! diagram stock-name draft)]
    (-> shell
        (cond-> (not= updated diagram) (assoc :diagram updated))
        (dissoc :edit-stock))))

(defn arm-source-placement!
  [diagram]
  (model/arm-source-placement diagram))

(defn place-source!
  [diagram x y]
  (model/place-source diagram x y))

(defn arm-sink-placement!
  [diagram]
  (model/arm-sink-placement diagram))

(defn place-sink!
  [diagram x y]
  (model/place-sink diagram x y))

(defn fixture-source!
  [diagram name x y]
  (model/fixture-source diagram name x y))

(defn fixture-sink!
  [diagram name x y]
  (model/fixture-sink diagram name x y))

(defn arm-flow-placement!
  [diagram]
  (model/arm-flow-placement diagram))

(defn select-flow-source!
  [diagram kind name]
  (model/select-flow-source diagram kind name))

(defn connect-flow!
  [diagram kind name]
  (model/connect-flow diagram kind name))

(defn fixture-flow!
  [diagram flow-name from-stock to-stock]
  (model/fixture-flow diagram flow-name from-stock to-stock))

(defn fixture-flow-from-source!
  [diagram flow-name source-name stock-name]
  (model/fixture-flow-from-source diagram flow-name source-name stock-name))

(defn fixture-flow-to-sink!
  [diagram flow-name stock-name sink-name]
  (model/fixture-flow-to-sink diagram flow-name stock-name sink-name))

(defn step-simulation!
  [diagram]
  (simulation/step diagram))

(defn run-simulation-steps!
  [diagram steps]
  (simulation/run-steps diagram steps))

(defn set-simulation-tick-delay-on-shell!
  [shell delay]
  (model/set-simulation-tick-delay shell delay))

(defn start-simulation-run-on-shell!
  [shell]
  (model/start-simulation-run shell))

(defn stop-simulation-run-on-shell!
  [shell]
  (model/stop-simulation-run shell))

(defn simulation-run-tick-on-shell!
  [shell]
  (if (model/simulation-running? shell)
    (update shell :diagram step-simulation!)
    shell))

(defn click-in-control-panel-on-shell!
  [shell]
  shell)

(defn drag-stock-within-control-panel-on-shell!
  [shell _stock-name]
  shell)

(defn step-simulation-on-shell!
  [shell]
  (update shell :diagram simulation/step))

(defn set-flow-name!
  [diagram old-name new-name]
  (model/set-flow-name diagram old-name new-name))

(defn set-flow-rate!
  [diagram name rate]
  (model/set-flow-rate diagram name rate))

(defn apply-flow-edit!
  [diagram flow-name draft]
  (model/apply-flow-edit diagram flow-name draft))

(defn- flow-edit-draft
  [diagram flow-name]
  (when (model/flow-exists? diagram flow-name)
    (let [flow (first (filter #(= flow-name (:name %)) (model/flows diagram)))]
      {:flow-name flow-name
       :name (:name flow)
       :rate (:rate flow)})))

(defn open-edit-flow-on-shell!
  [shell flow-name]
  (if-let [draft (flow-edit-draft (:diagram shell) flow-name)]
    (assoc shell :edit-flow draft)
    shell))

(defn cancel-edit-flow-on-shell!
  [shell]
  (dissoc shell :edit-flow))

(defn apply-edit-flow-on-shell!
  [shell draft]
  (let [flow-name (:flow-name (:edit-flow shell))
        diagram (:diagram shell)
        updated (apply-flow-edit! diagram flow-name draft)]
    (-> shell
        (cond-> (not= updated diagram) (assoc :diagram updated))
        (dissoc :edit-flow))))

(defn arm-converter-placement!
  [diagram]
  (model/arm-converter-placement diagram))

(defn place-converter!
  [diagram x y]
  (model/place-converter diagram x y))

(defn fixture-converter!
  [diagram name x y]
  (model/fixture-converter diagram name x y))

(defn fixture-connector!
  [diagram connector-name from-converter to-flow]
  (model/fixture-connector diagram connector-name from-converter to-flow))

(defn set-converter-name!
  [diagram old-name new-name]
  (model/set-converter-name diagram old-name new-name))

(defn set-converter-formula!
  [diagram name formula]
  (model/set-converter-formula diagram name formula))

(defn apply-converter-edit!
  [diagram converter-name draft]
  (model/apply-converter-edit diagram converter-name draft))

(defn- converter-edit-draft
  [diagram converter-name]
  (when (model/converter-exists? diagram converter-name)
    (let [conv (first (filter #(= converter-name (:name %)) (model/converters diagram)))]
      {:converter-name converter-name
       :name (:name conv)
       :formula (or (model/converter-connector-formula diagram converter-name) "")})))

(defn open-edit-converter-on-shell!
  [shell converter-name]
  (if-let [draft (converter-edit-draft (:diagram shell) converter-name)]
    (assoc shell :edit-converter draft)
    shell))

(defn cancel-edit-converter-on-shell!
  [shell]
  (dissoc shell :edit-converter))

(defn- converter-formula-edit-rejected?
  [diagram converter-name {:keys [formula]}]
  (let [prior-formula (or (model/converter-connector-formula diagram converter-name) "")
        formula-text (str/trim (str formula))]
    (and (seq formula-text)
         (not= formula-text prior-formula)
         (= diagram (model/set-converter-formula diagram converter-name formula)))))

(defn apply-edit-converter-on-shell!
  [shell draft]
  (let [converter-name (:converter-name (:edit-converter shell))
        diagram (:diagram shell)
        formula-rejected? (converter-formula-edit-rejected? diagram converter-name draft)
        updated (apply-converter-edit! diagram converter-name draft)]
    (cond
      formula-rejected?
      shell

      (= updated diagram)
      (dissoc shell :edit-converter)

      :else
      (-> shell
          (assoc :diagram updated)
          (dissoc :edit-converter)))))

(defn arm-connector-placement!
  [diagram]
  (model/arm-connector-placement diagram))

(defn select-connector-origin!
  [diagram kind name]
  (model/select-connector-origin diagram kind name))

(defn connect-connector!
  [diagram kind name]
  (model/connect-connector diagram kind name))

(defn arm-stock-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-stock-placement!))

(defn place-stock-on-shell!
  [shell x y]
  (update-diagram-and-clear-preview shell #(place-stock! % x y)))

(defn arm-source-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-source-placement!))

(defn place-source-on-shell!
  [shell x y]
  (update-diagram-and-clear-preview shell #(place-source! % x y)))

(defn arm-sink-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-sink-placement!))

(defn place-sink-on-shell!
  [shell x y]
  (update-diagram-and-clear-preview shell #(place-sink! % x y)))

(defn arm-flow-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-flow-placement!))

(defn arm-converter-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-converter-placement!))

(defn place-converter-on-shell!
  [shell x y]
  (update-diagram-and-clear-preview shell #(place-converter! % x y)))

(defn arm-connector-placement-on-shell!
  [shell]
  (update-diagram-and-clear-preview shell arm-connector-placement!))

(defn- select-endpoint-on-diagram
  [diagram kind name]
  (case (:placement-mode diagram)
    :flow (if (:flow-draft diagram)
            (connect-flow! diagram kind name)
            (select-flow-source! diagram kind name))
    :connector (if (:connector-draft diagram)
                 (connect-connector! diagram kind name)
                 (select-connector-origin! diagram kind name))
    diagram))

(defn select-endpoint-on-shell!
  [shell kind name]
  (update-diagram-and-clear-preview shell #(select-endpoint-on-diagram % kind name)))

;; clj-mutate-manifest-begin
;; {:version 1, :tested-at "2026-06-30T08:36:52.581155-05:00", :module-hash "-235385713", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 3, :hash "-1312532329"} {:id "defn/default-shell!", :kind "defn", :line 5, :end-line 7, :hash "-2121201791"} {:id "defn/default-diagram!", :kind "defn", :line 9, :end-line 11, :hash "605260130"} {:id "defn/show-about!", :kind "defn", :line 13, :end-line 17, :hash "-1322911364"} {:id "defn/quit!", :kind "defn", :line 19, :end-line 21, :hash "-6014701"} {:id "defn/clear-canvas-preview-on-shell!", :kind "defn", :line 23, :end-line 25, :hash "1352894957"} {:id "defn/update-canvas-preview-on-shell!", :kind "defn", :line 27, :end-line 34, :hash "119519576"} {:id "defn-/update-diagram-and-clear-preview", :kind "defn-", :line 36, :end-line 40, :hash "2072859270"} {:id "defn/arm-stock-placement!", :kind "defn", :line 42, :end-line 44, :hash "1528122585"} {:id "defn/place-stock!", :kind "defn", :line 46, :end-line 48, :hash "327264207"} {:id "defn/fixture-stock!", :kind "defn", :line 50, :end-line 52, :hash "649142275"} {:id "defn/move-stock!", :kind "defn", :line 54, :end-line 56, :hash "-1366856280"} {:id "defn/move-stock-on-shell!", :kind "defn", :line 58, :end-line 60, :hash "1550770721"} {:id "defn/move-source!", :kind "defn", :line 62, :end-line 64, :hash "715464740"} {:id "defn/move-sink!", :kind "defn", :line 66, :end-line 68, :hash "1232417531"} {:id "defn/move-cloud-on-shell!", :kind "defn", :line 70, :end-line 75, :hash "-1092246664"} {:id "defn-/resolve-stock-drag-name", :kind "defn-", :line 77, :end-line 82, :hash "-170855797"} {:id "defn/start-stock-drag-on-shell!", :kind "defn", :line 84, :end-line 99, :hash "598771222"} {:id "defn-/pointer-drag-position", :kind "defn-", :line 101, :end-line 104, :hash "1014483340"} {:id "defn-/scene-drag-position", :kind "defn-", :line 106, :end-line 108, :hash "906076843"} {:id "defn/drag-stock-on-shell!", :kind "defn", :line 110, :end-line 118, :hash "2087730536"} {:id "defn/end-stock-drag-on-shell!", :kind "defn", :line 120, :end-line 126, :hash "-671293140"} {:id "defn-/resolve-cloud-drag-target", :kind "defn-", :line 128, :end-line 134, :hash "114022716"} {:id "defn-/cloud-exists?", :kind "defn-", :line 136, :end-line 141, :hash "-1384194059"} {:id "defn-/cloud-position", :kind "defn-", :line 143, :end-line 148, :hash "-1241372925"} {:id "defn/start-cloud-drag-on-shell!", :kind "defn", :line 150, :end-line 167, :hash "-1371771074"} {:id "defn/drag-cloud-on-shell!", :kind "defn", :line 169, :end-line 176, :hash "-1082519854"} {:id "defn/end-cloud-drag-on-shell!", :kind "defn", :line 178, :end-line 184, :hash "-1762670321"} {:id "defn/move-converter!", :kind "defn", :line 186, :end-line 188, :hash "1758477189"} {:id "defn/move-converter-on-shell!", :kind "defn", :line 190, :end-line 192, :hash "217392874"} {:id "defn/move-connector-handle!", :kind "defn", :line 194, :end-line 196, :hash "-645830214"} {:id "defn/move-connector-handle-on-shell!", :kind "defn", :line 198, :end-line 200, :hash "-1661797408"} {:id "defn-/resolve-converter-drag-name", :kind "defn-", :line 202, :end-line 207, :hash "-1188273566"} {:id "defn/start-converter-drag-on-shell!", :kind "defn", :line 209, :end-line 224, :hash "-2060442868"} {:id "defn/drag-converter-on-shell!", :kind "defn", :line 226, :end-line 234, :hash "733879177"} {:id "defn/end-converter-drag-on-shell!", :kind "defn", :line 236, :end-line 242, :hash "1511276522"} {:id "defn/start-connector-control-drag-on-shell!", :kind "defn", :line 244, :end-line 258, :hash "-704820084"} {:id "defn-/canvas-drag-position", :kind "defn-", :line 260, :end-line 262, :hash "1744513589"} {:id "defn/drag-connector-control-on-shell!", :kind "defn", :line 264, :end-line 271, :hash "-52830822"} {:id "defn/end-connector-control-drag-on-shell!", :kind "defn", :line 273, :end-line 279, :hash "-662680162"} {:id "defn/click-select!", :kind "defn", :line 281, :end-line 283, :hash "543148823"} {:id "defn/click-select-at!", :kind "defn", :line 285, :end-line 287, :hash "-628062425"} {:id "defn/shift-click-select!", :kind "defn", :line 289, :end-line 291, :hash "-1830221816"} {:id "defn/shift-click-select-at!", :kind "defn", :line 293, :end-line 295, :hash "1743007418"} {:id "defn/marquee-select!", :kind "defn", :line 297, :end-line 299, :hash "277313531"} {:id "defn/clear-selection!", :kind "defn", :line 301, :end-line 303, :hash "-1761941661"} {:id "defn/delete-selection!", :kind "defn", :line 305, :end-line 307, :hash "-1936631472"} {:id "defn/fixture-stock-connector!", :kind "defn", :line 309, :end-line 311, :hash "1548556317"} {:id "defn/click-select-on-shell!", :kind "defn", :line 313, :end-line 315, :hash "-1548056535"} {:id "defn/click-select-at-on-shell!", :kind "defn", :line 317, :end-line 319, :hash "-836895205"} {:id "defn/shift-click-select-on-shell!", :kind "defn", :line 321, :end-line 323, :hash "-1787730092"} {:id "defn/shift-click-select-at-on-shell!", :kind "defn", :line 325, :end-line 327, :hash "1617838024"} {:id "defn/marquee-select-on-shell!", :kind "defn", :line 329, :end-line 331, :hash "1242076553"} {:id "defn/clear-selection-on-shell!", :kind "defn", :line 333, :end-line 335, :hash "1399975763"} {:id "defn/cancel-on-escape-on-shell!", :kind "defn", :line 337, :end-line 341, :hash "-240960220"} {:id "defn/delete-selection-on-shell!", :kind "defn", :line 343, :end-line 345, :hash "1568513828"} {:id "defn/start-marquee-drag-on-shell!", :kind "defn", :line 347, :end-line 360, :hash "826467500"} {:id "defn/drag-marquee-on-shell!", :kind "defn", :line 362, :end-line 369, :hash "-1297876374"} {:id "defn/end-marquee-drag-on-shell!", :kind "defn", :line 371, :end-line 380, :hash "2056537560"} {:id "defn/set-stock-name!", :kind "defn", :line 382, :end-line 384, :hash "980633483"} {:id "defn/set-stock-initial-value!", :kind "defn", :line 386, :end-line 388, :hash "1954449060"} {:id "defn/set-stock-min!", :kind "defn", :line 390, :end-line 392, :hash "-842214619"} {:id "defn/set-stock-max!", :kind "defn", :line 394, :end-line 396, :hash "-1580614357"} {:id "defn/clear-stock-max!", :kind "defn", :line 398, :end-line 400, :hash "1483601368"} {:id "defn/apply-stock-edit!", :kind "defn", :line 402, :end-line 404, :hash "-1903378945"} {:id "defn-/stock-edit-draft", :kind "defn-", :line 406, :end-line 414, :hash "-1438992226"} {:id "defn/open-edit-stock-on-shell!", :kind "defn", :line 416, :end-line 420, :hash "-554871434"} {:id "defn/cancel-edit-stock-on-shell!", :kind "defn", :line 422, :end-line 424, :hash "1634807379"} {:id "defn/apply-edit-stock-on-shell!", :kind "defn", :line 426, :end-line 433, :hash "1721865818"} {:id "defn/arm-source-placement!", :kind "defn", :line 435, :end-line 437, :hash "-126889149"} {:id "defn/place-source!", :kind "defn", :line 439, :end-line 441, :hash "-346190542"} {:id "defn/arm-sink-placement!", :kind "defn", :line 443, :end-line 445, :hash "-1217049189"} {:id "defn/place-sink!", :kind "defn", :line 447, :end-line 449, :hash "1657306823"} {:id "defn/fixture-source!", :kind "defn", :line 451, :end-line 453, :hash "1604815373"} {:id "defn/fixture-sink!", :kind "defn", :line 455, :end-line 457, :hash "-1292347174"} {:id "defn/arm-flow-placement!", :kind "defn", :line 459, :end-line 461, :hash "-2049350910"} {:id "defn/select-flow-source!", :kind "defn", :line 463, :end-line 465, :hash "233734405"} {:id "defn/connect-flow!", :kind "defn", :line 467, :end-line 469, :hash "736803171"} {:id "defn/fixture-flow!", :kind "defn", :line 471, :end-line 473, :hash "1369753620"} {:id "defn/fixture-flow-from-source!", :kind "defn", :line 475, :end-line 477, :hash "-2089993013"} {:id "defn/fixture-flow-to-sink!", :kind "defn", :line 479, :end-line 481, :hash "-1541511383"} {:id "defn/step-simulation!", :kind "defn", :line 483, :end-line 485, :hash "1460263805"} {:id "defn/run-simulation-steps!", :kind "defn", :line 487, :end-line 489, :hash "330539702"} {:id "defn/click-in-control-panel-on-shell!", :kind "defn", :line 491, :end-line 493, :hash "-1987131572"} {:id "defn/drag-stock-within-control-panel-on-shell!", :kind "defn", :line 495, :end-line 497, :hash "-369065730"} {:id "defn/step-simulation-on-shell!", :kind "defn", :line 499, :end-line 501, :hash "-591495089"} {:id "defn/set-flow-name!", :kind "defn", :line 503, :end-line 505, :hash "-2006740562"} {:id "defn/set-flow-rate!", :kind "defn", :line 507, :end-line 509, :hash "-605461932"} {:id "defn/apply-flow-edit!", :kind "defn", :line 511, :end-line 513, :hash "937876152"} {:id "defn-/flow-edit-draft", :kind "defn-", :line 515, :end-line 521, :hash "-864414921"} {:id "defn/open-edit-flow-on-shell!", :kind "defn", :line 523, :end-line 527, :hash "-83688387"} {:id "defn/cancel-edit-flow-on-shell!", :kind "defn", :line 529, :end-line 531, :hash "-1051920277"} {:id "defn/apply-edit-flow-on-shell!", :kind "defn", :line 533, :end-line 540, :hash "-2093693896"} {:id "defn/arm-converter-placement!", :kind "defn", :line 542, :end-line 544, :hash "1417442163"} {:id "defn/place-converter!", :kind "defn", :line 546, :end-line 548, :hash "-872494041"} {:id "defn/fixture-converter!", :kind "defn", :line 550, :end-line 552, :hash "1640813265"} {:id "defn/fixture-connector!", :kind "defn", :line 554, :end-line 556, :hash "663905514"} {:id "defn/set-converter-name!", :kind "defn", :line 558, :end-line 560, :hash "-416090274"} {:id "defn/set-converter-formula!", :kind "defn", :line 562, :end-line 564, :hash "-1811945733"} {:id "defn/apply-converter-edit!", :kind "defn", :line 566, :end-line 568, :hash "73350704"} {:id "defn-/converter-edit-draft", :kind "defn-", :line 570, :end-line 576, :hash "-2122920474"} {:id "defn/open-edit-converter-on-shell!", :kind "defn", :line 578, :end-line 582, :hash "2075538689"} {:id "defn/cancel-edit-converter-on-shell!", :kind "defn", :line 584, :end-line 586, :hash "-282423341"} {:id "defn/apply-edit-converter-on-shell!", :kind "defn", :line 588, :end-line 595, :hash "2076422571"} {:id "defn/arm-connector-placement!", :kind "defn", :line 597, :end-line 599, :hash "-785743789"} {:id "defn/select-connector-origin!", :kind "defn", :line 601, :end-line 603, :hash "-1004406321"} {:id "defn/connect-connector!", :kind "defn", :line 605, :end-line 607, :hash "1425348016"} {:id "defn/arm-stock-placement-on-shell!", :kind "defn", :line 609, :end-line 611, :hash "-712170829"} {:id "defn/place-stock-on-shell!", :kind "defn", :line 613, :end-line 615, :hash "-1411128542"} {:id "defn/arm-source-placement-on-shell!", :kind "defn", :line 617, :end-line 619, :hash "-874772117"} {:id "defn/place-source-on-shell!", :kind "defn", :line 621, :end-line 623, :hash "-1565540298"} {:id "defn/arm-sink-placement-on-shell!", :kind "defn", :line 625, :end-line 627, :hash "1406606977"} {:id "defn/place-sink-on-shell!", :kind "defn", :line 629, :end-line 631, :hash "-103908621"} {:id "defn/arm-flow-placement-on-shell!", :kind "defn", :line 633, :end-line 635, :hash "-1214030821"} {:id "defn/arm-converter-placement-on-shell!", :kind "defn", :line 637, :end-line 639, :hash "-686649389"} {:id "defn/place-converter-on-shell!", :kind "defn", :line 641, :end-line 643, :hash "1844612230"} {:id "defn/arm-connector-placement-on-shell!", :kind "defn", :line 645, :end-line 647, :hash "-1911447954"} {:id "defn-/select-endpoint-on-diagram", :kind "defn-", :line 649, :end-line 658, :hash "1840493823"} {:id "defn/select-endpoint-on-shell!", :kind "defn", :line 660, :end-line 662, :hash "-1810970015"}]}
;; clj-mutate-manifest-end
