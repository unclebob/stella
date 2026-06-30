(ns stella.events)

(def quit ::quit)

(def window-close ::window-close)

(def show-about ::show-about)

(def arm-stock ::arm-stock)

(def arm-flow ::arm-flow)

(def arm-source ::arm-source)

(def arm-sink ::arm-sink)

(def arm-converter ::arm-converter)

(def arm-connector ::arm-connector)

(def canvas-click ::canvas-click)

(def canvas-move ::canvas-move)

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

(def stock-drag ::stock-drag)

(def stock-drag-end ::stock-drag-end)

(def converter-drag-start ::converter-drag-start)

(def converter-drag ::converter-drag)

(def converter-drag-end ::converter-drag-end)

(def cloud-drag-start ::cloud-drag-start)

(def cloud-drag ::cloud-drag)

(def cloud-drag-end ::cloud-drag-end)

(def connector-control-drag-start ::connector-control-drag-start)

(def connector-control-drag ::connector-control-drag)

(def connector-control-drag-end ::connector-control-drag-end)

(def selection-click ::selection-click)

(def marquee-drag-start ::marquee-drag-start)

(def marquee-drag ::marquee-drag)

(def marquee-drag-end ::marquee-drag-end)

(def clear-selection ::clear-selection)

(def scene-key-pressed ::scene-key-pressed)

(def simulation-step ::simulation-step)
