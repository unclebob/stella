# Implementation Plan: Drag Stock

**Task:** `drag-stock`  
**Status:** Approved  
**Depends on:** `place-stock`

## User story

As a modeler, I can drag a placed stock on the canvas to reposition it.

## Locked decisions

| Decision | Choice |
|---|---|
| Gesture | Primary-button press on stock, drag, release |
| When allowed | Only when `:placement-mode :idle` |
| Position update | Stock `:x` and `:y` set to new top-left on release |
| Grab offset | Preserve offset from press point within the stock icon |
| Right-click | Unchanged — opens **Edit Stock** dialog (`edit-stock`) |
| Armed placement | Drag does not run while a palette placement tool is armed |
| Connected links | Flows and connectors redraw from updated stock anchors; endpoint refs unchanged |
| Stock identity | Name and field values unchanged by move |

## Observable behavior

1. With placement idle, pressing on a stock and dragging moves the stock icon.
2. On release, the stock remains at the new position.
3. Stock count does not change.
4. Attached flows and connectors follow the stock visually.
5. Arming **Stock** (or any placement tool) disables drag-to-move until disarmed or placement completes.

## Model shape

No new keys. `move-stock!` updates `:x` and `:y` on the stock record.

## Module changes

| Module | Change |
|---|---|
| `stella.model` | `move-stock` |
| `stella.commands` | `move-stock!` |
| `stella.events` | Drag press/drag/release events on stock nodes |
| `stella.ui.canvas` | Mouse handlers on stock group; emit drag events when idle |
| `stella.dispatch` | Route drag release to `move-stock!` |
| `stella.acceptance` | Step handlers for `features/model/drag-stock.feature` |
| `stella.qa.ui-driver` | `drag-element!` from semantic stock to canvas position |
| `stella.qa.hit-test` | Read stock bounds after drag |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/drag-stock.feature` |
| QA | `qa/procedures/drag-stock.qa.md` |

## Out of scope

- Drag converter, source, sink, or cloud
- Snap to grid
- Delete stock
- Multi-select or marquee drag
- Undo