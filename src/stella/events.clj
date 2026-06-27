(ns stella.events)

(def quit ::quit)

(def show-about ::show-about)

(def arm-stock ::arm-stock)

(def arm-flow ::arm-flow)

(def arm-source ::arm-source)

(def arm-sink ::arm-sink)

(def arm-converter ::arm-converter)

(def arm-connector ::arm-connector)

(def canvas-click ::canvas-click)

(def endpoint-click ::endpoint-click)

(def edit-stock-open ::edit-stock-open)

(def edit-stock-apply ::edit-stock-apply)

(def edit-stock-cancel ::edit-stock-cancel)

(def edit-flow-open ::edit-flow-open)

(def edit-flow-apply ::edit-flow-apply)

(def edit-flow-cancel ::edit-flow-cancel)

(def edit-converter-open ::edit-converter-open)

(def edit-converter-apply ::edit-converter-apply)

(def edit-converter-cancel ::edit-converter-cancel)

(def stock-drag-start ::stock-drag-start)

(def stock-drag-end ::stock-drag-end)

(def converter-drag-start ::converter-drag-start)

(def converter-drag-end ::converter-drag-end)

(def selection-click ::selection-click)

(def marquee-drag-start ::marquee-drag-start)

(def marquee-drag-end ::marquee-drag-end)

(def clear-selection ::clear-selection)

(def scene-key-pressed ::scene-key-pressed)
