# Implementation Plan: Select Objects

**Task:** `select-objects`  
**Status:** Approved  
**Depends on:** `place-stock`, `connectors`

## User story

As a modeler, I can select diagram objects to highlight them for inspection or later actions.

## Locked decisions

| Decision | Choice |
|---|---|
| Selectable kinds | Stock, flow, converter, connector, source, sink |
| When allowed | Only when `:placement-mode :idle` |
| Plain click unselected | Select object; clear all other selections first |
| Plain click selected | Deselect that object (toggle off) |
| Shift+click | Toggle object in selection without clearing others |
| Marquee | Primary-button drag on canvas background draws a rectangle; on release, select all objects whose bounds intersect the rectangle (replaces selection) |
| Marquee vs move | Marquee starts on canvas background; object drag-to-move (`drag-stock`, `drag-converter`) starts on object press |
| Highlight | Grey outline around each selected object |
| Escape | Clears all selections |
| Placement tools armed | Selection gestures are ignored; endpoint clicks keep placement behavior |
| Right-click | Unchanged — edit dialogs (`edit-stock`, `edit-flow`, `edit-converter`) |
| Selection storage | Diagram `:selection` set of `{:kind … :id …}` refs |

## Observable behavior

1. Clicking an unselected object selects it and shows a grey outline.
2. Clicking a selected object removes it from the selection and removes its outline.
3. Shift+click toggles membership without affecting other selected objects.
4. Dragging a selection rectangle on the canvas selects every intersecting object and deselects non-intersecting objects.
5. **Esc** clears every selection.
6. While a palette placement tool is armed, clicks do not change selection state.

## Model shape

```clojure
{:selection #{{:kind :stock :id "Stock1"}
              {:kind :flow :id "Flow1"}}}
```

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Selection set; `click-select`, `shift-click-select`, `marquee-select`, `clear-selection`, `selected?`, `selection-count` |
| `stella.commands` | `click-select!`, `shift-click-select!`, `marquee-select!`, `clear-selection!` |
| `stella.events` | Selection click, marquee drag, escape key |
| `stella.ui.canvas` | Grey selection outline on selected nodes; marquee overlay while dragging |
| `stella.dispatch` | Route selection events when idle; escape clears selection |
| `stella.ui.root` | Scene key handler for Escape |
| `stella.acceptance` | Step handlers for `features/model/select-objects.feature` |
| `stella.qa.ui-driver` | `shift-click-element!`, `marquee-select!`, `press-escape!`, selection outline assertions |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/select-objects.feature` |
| QA | `qa/procedures/select-objects.qa.md` |

## Out of scope

- Delete, copy, or paste selected objects
- Keyboard nudge of selection
- Select-all shortcut
- Side inspector or property panel
- Shift+marquee additive selection
- Click empty canvas to clear selection