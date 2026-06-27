# Implementation Plan: Drag Converter

**Task:** `drag-converter`  
**Status:** Approved  
**Depends on:** `connectors`

## User story

As a modeler, I can drag a placed converter on the canvas to reposition it.

## Locked decisions

Mirror `drag-stock` for converters.

| Decision | Choice |
|---|---|
| Gesture | Primary-button press on converter, drag, release |
| When allowed | Only when `:placement-mode :idle` |
| Position update | Converter `:x` and `:y` set to new top-left on release |
| Grab offset | Preserve offset from press point within the converter circle |
| Right-click | Unchanged — opens **Edit Converter** dialog (`edit-converter`) |
| Armed placement | Drag does not run while a palette placement tool is armed |
| Connected links | Connectors redraw from updated converter anchors; endpoint refs unchanged |
| Converter identity | Name, value, and connector formula unchanged by move |

## Observable behavior

1. With placement idle, pressing on a converter and dragging moves the converter icon.
2. On release, the converter remains at the new position.
3. Converter count does not change.
4. Attached connectors follow the converter visually.
5. Arming any placement tool disables drag-to-move until disarmed or placement completes.

## Model shape

No new keys. `move-converter!` updates `:x` and `:y` on the converter record.

## Module changes

| Module | Change |
|---|---|
| `stella.model` | `move-converter` |
| `stella.commands` | `move-converter!` |
| `stella.events` | Drag press/drag/release events on converter nodes |
| `stella.ui.canvas` | Mouse handlers on converter group; emit drag events when idle |
| `stella.dispatch` | Route drag release to `move-converter!` |
| `stella.acceptance` | Step handlers for `features/model/drag-converter.feature` |
| `stella.qa.ui-driver` | Reuse `drag-element!` for `:converter` targets |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/drag-converter.feature` |
| QA | `qa/procedures/drag-converter.qa.md` |

## Out of scope

- Drag stock, source, sink, or cloud
- Snap to grid
- Delete converter
- Multi-select drag
- Undo