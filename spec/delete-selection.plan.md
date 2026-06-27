# Implementation Plan: Delete Selection

**Task:** `delete-selection`  
**Status:** Approved  
**Depends on:** `select-objects`

## User story

As a modeler, I can delete the current selection with the **Delete** or **Backspace** key.

## Locked decisions

| Decision | Choice |
|---|---|
| Trigger keys | `Delete` and `Backspace` |
| When allowed | `:placement-mode :idle` and no modal dialog has keyboard focus |
| Empty selection | No-op |
| After delete | Remove deleted objects from diagram; clear `:selection` |
| Multi-select | Delete every selected object in one operation |

## Cascade rules

Deleting an object also removes dependent links:

| Deleted kind | Also remove |
|---|---|
| Stock | Flows and connectors that reference the stock |
| Source | Flows that reference the source |
| Sink | Flows that reference the sink |
| Flow | Connectors that reference the flow |
| Converter | Connectors that reference the converter |
| Connector | Connector only |

Objects both selected and cascade-deleted are removed once. Internal auto-name counters (`:next-stock-num`, etc.) are not decremented.

## Observable behavior

1. With objects selected and placement idle, **Delete** or **Backspace** removes them from the canvas.
2. Attached flows and connectors per cascade rules disappear.
3. Selection outline clears after delete.
4. Empty selection or armed placement tool leaves the diagram unchanged.

## Module changes

| Module | Change |
|---|---|
| `stella.model` | `delete-selection`, cascade helpers, `remove-stock`, `remove-flow`, etc. |
| `stella.commands` | `delete-selection!` |
| `stella.dispatch` | Route Delete/Backspace to `delete-selection!` when idle and no modal focused |
| `stella.ui.root` | Scene key handler for Delete and Backspace |
| `stella.acceptance` | Step handlers for `features/model/delete-selection.feature` |
| `stella.qa.ui-driver` | `press-delete!`, `press-backspace!` |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/delete-selection.feature` |
| QA | `qa/procedures/delete-selection.qa.md` |

## Out of scope

- Undo
- Delete without prior selection (click-to-delete)
- Confirmation dialog
- Partial delete inside edit dialogs
- Renumbering auto-names after delete