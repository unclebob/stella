(ns stella.commands
  (:require [stella.model :as model]
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

(defn apply-edit-converter-on-shell!
  [shell draft]
  (let [converter-name (:converter-name (:edit-converter shell))
        diagram (:diagram shell)
        updated (apply-converter-edit! diagram converter-name draft)]
    (-> shell
        (cond-> (not= updated diagram) (assoc :diagram updated))
        (dissoc :edit-converter))))

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
;; {:version 1, :tested-at "2026-06-27T10:27:11.063071-05:00", :module-hash "273800771", :forms [{:id "form/0/ns", :kind "ns", :line 1, :end-line 2, :hash "273392964"} {:id "defn/default-shell!", :kind "defn", :line 4, :end-line 6, :hash "-2121201791"} {:id "defn/default-diagram!", :kind "defn", :line 8, :end-line 10, :hash "605260130"} {:id "defn/show-about!", :kind "defn", :line 12, :end-line 16, :hash "-1322911364"} {:id "defn/quit!", :kind "defn", :line 18, :end-line 20, :hash "-6014701"} {:id "defn/arm-stock-placement!", :kind "defn", :line 22, :end-line 24, :hash "1528122585"} {:id "defn/place-stock!", :kind "defn", :line 26, :end-line 28, :hash "327264207"} {:id "defn/fixture-stock!", :kind "defn", :line 30, :end-line 32, :hash "649142275"} {:id "defn/move-stock!", :kind "defn", :line 34, :end-line 36, :hash "-1366856280"} {:id "defn/move-converter!", :kind "defn", :line 38, :end-line 40, :hash "1758477189"} {:id "defn/click-select!", :kind "defn", :line 42, :end-line 44, :hash "543148823"} {:id "defn/shift-click-select!", :kind "defn", :line 46, :end-line 48, :hash "-1830221816"} {:id "defn/marquee-select!", :kind "defn", :line 50, :end-line 52, :hash "277313531"} {:id "defn/clear-selection!", :kind "defn", :line 54, :end-line 56, :hash "-1761941661"} {:id "defn/delete-selection!", :kind "defn", :line 58, :end-line 60, :hash "-1936631472"} {:id "defn/fixture-stock-connector!", :kind "defn", :line 62, :end-line 64, :hash "1548556317"} {:id "defn/set-stock-name!", :kind "defn", :line 66, :end-line 68, :hash "980633483"} {:id "defn/set-stock-initial-value!", :kind "defn", :line 70, :end-line 72, :hash "1954449060"} {:id "defn/set-stock-min!", :kind "defn", :line 74, :end-line 76, :hash "-842214619"} {:id "defn/set-stock-max!", :kind "defn", :line 78, :end-line 80, :hash "-1580614357"} {:id "defn/clear-stock-max!", :kind "defn", :line 82, :end-line 84, :hash "1483601368"} {:id "defn/arm-source-placement!", :kind "defn", :line 86, :end-line 88, :hash "-126889149"} {:id "defn/place-source!", :kind "defn", :line 90, :end-line 92, :hash "-346190542"} {:id "defn/arm-sink-placement!", :kind "defn", :line 94, :end-line 96, :hash "-1217049189"} {:id "defn/place-sink!", :kind "defn", :line 98, :end-line 100, :hash "1657306823"} {:id "defn/fixture-source!", :kind "defn", :line 102, :end-line 104, :hash "1604815373"} {:id "defn/fixture-sink!", :kind "defn", :line 106, :end-line 108, :hash "-1292347174"} {:id "defn/arm-flow-placement!", :kind "defn", :line 110, :end-line 112, :hash "-2049350910"} {:id "defn/select-flow-source!", :kind "defn", :line 114, :end-line 116, :hash "233734405"} {:id "defn/connect-flow!", :kind "defn", :line 118, :end-line 120, :hash "736803171"} {:id "defn/fixture-flow!", :kind "defn", :line 122, :end-line 124, :hash "1369753620"} {:id "defn/set-flow-name!", :kind "defn", :line 126, :end-line 128, :hash "-2006740562"} {:id "defn/set-flow-rate!", :kind "defn", :line 130, :end-line 132, :hash "-605461932"} {:id "defn/arm-converter-placement!", :kind "defn", :line 134, :end-line 136, :hash "1417442163"} {:id "defn/place-converter!", :kind "defn", :line 138, :end-line 140, :hash "-872494041"} {:id "defn/fixture-converter!", :kind "defn", :line 142, :end-line 144, :hash "1640813265"} {:id "defn/fixture-connector!", :kind "defn", :line 146, :end-line 148, :hash "663905514"} {:id "defn/set-converter-name!", :kind "defn", :line 150, :end-line 152, :hash "-416090274"} {:id "defn/set-converter-formula!", :kind "defn", :line 154, :end-line 156, :hash "-1811945733"} {:id "defn/arm-connector-placement!", :kind "defn", :line 158, :end-line 160, :hash "-785743789"} {:id "defn/select-connector-origin!", :kind "defn", :line 162, :end-line 164, :hash "-1004406321"} {:id "defn/connect-connector!", :kind "defn", :line 166, :end-line 168, :hash "1425348016"} {:id "defn/arm-stock-placement-on-shell!", :kind "defn", :line 170, :end-line 172, :hash "1559005654"} {:id "defn/place-stock-on-shell!", :kind "defn", :line 174, :end-line 176, :hash "-1939405487"} {:id "defn/arm-source-placement-on-shell!", :kind "defn", :line 178, :end-line 180, :hash "-1886329299"} {:id "defn/place-source-on-shell!", :kind "defn", :line 182, :end-line 184, :hash "-633997888"} {:id "defn/arm-sink-placement-on-shell!", :kind "defn", :line 186, :end-line 188, :hash "1720300578"} {:id "defn/place-sink-on-shell!", :kind "defn", :line 190, :end-line 192, :hash "-499332881"} {:id "defn/arm-flow-placement-on-shell!", :kind "defn", :line 194, :end-line 196, :hash "-226480789"} {:id "defn/arm-converter-placement-on-shell!", :kind "defn", :line 198, :end-line 200, :hash "1418940624"} {:id "defn/place-converter-on-shell!", :kind "defn", :line 202, :end-line 204, :hash "116885808"} {:id "defn/arm-connector-placement-on-shell!", :kind "defn", :line 206, :end-line 208, :hash "1279171735"} {:id "defn-/select-endpoint-on-diagram", :kind "defn-", :line 210, :end-line 219, :hash "1840493823"} {:id "defn/select-endpoint-on-shell!", :kind "defn", :line 221, :end-line 223, :hash "-75615409"}]}
;; clj-mutate-manifest-end
