# Implementation Plan: Edit Stock

**Task:** `edit-stock`  
**Status:** Approved  
**Depends on:** `place-stock`

## User story

As a modeler, I can right-click a stock on the canvas and edit its name, initial value, minimum, and maximum in a dialog.

## Locked decisions

| Decision | Choice |
|---|---|
| Open editor | Right-click stock icon on canvas |
| Editor UX | Modal dialog (not a side inspector) |
| Dialog title | `Edit Stock` |
| Editable fields | Name, Initial value, Minimum, Maximum |
| Commit | `OK` applies all field values |
| Cancel | `Cancel` closes dialog with no model changes |
| Default minimum | `0` on new stocks |
| Default maximum | Unbounded (empty Maximum field) |
| Name rules | Non-empty, unique among stocks |
| Value rules | Minimum ≤ initial value ≤ maximum when maximum is set |
| Canvas icon layout | Name centered prominently; minimum bottom-left; maximum bottom-right |
| Canvas min/max font | Small font; each bound label uses less than one-quarter of icon width |
| Unbounded maximum | Bottom-right label area left empty on the icon |
| Initial value on icon | Not shown (dialog and simulation only) |
| Rejected edits | Dialog stays open; field values unchanged on canvas |

## Observable behavior

1. Right-clicking a stock opens the **Edit Stock** dialog populated with current field values.
2. **OK** with valid values updates the model and stock icon labels.
3. **Name** rename updates the centered canvas label and semantic hit-test identity.
4. **Minimum** and **Maximum** appear on the icon at bottom-left and bottom-right in small type.
5. Empty **Maximum** in the dialog clears the bottom-right icon label.
6. Invalid edits (duplicate name, out-of-range initial value) keep the dialog open and leave the canvas unchanged.
7. **Cancel** closes the dialog without applying edits.

## Stock icon layout

Stock rectangle remains 80×50 pixels (v1). Inside the icon:

```
┌────────────────────────────┐
│                            │
│          Stock1            │  ← name, prominent, centered
│                            │
│ 0                    100   │  ← min (bottom-left), max (bottom-right), small font
└────────────────────────────┘
```

- Name uses the primary label style and is visually dominant.
- Minimum and maximum each use a small font and occupy less than one-quarter of the icon width.
- Initial value is not rendered on the icon.

This layout replaces the prior place-stock canvas display (name + initial value stacked).

## Model shape

```clojure
{:name "Stock1"
 :initial-value "0"
 :min-value "0"
 :max-value nil          ; nil = unbounded
 :x 200
 :y 150}
```

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Add `:min-value`, `:max-value`; rename and validation helpers |
| `stella.commands` | `set-stock-name!`, `set-stock-initial-value!`, `set-stock-min!`, `set-stock-max!`, `clear-stock-max!`, `apply-stock-edit!` |
| `stella.ui.edit-stock-dialog` | Modal dialog with labeled fields and OK/Cancel |
| `stella.ui.canvas` | Stock icon layout (name center, min/max corners); right-click opens edit dialog |
| `stella.dispatch` | Dialog OK commits through commands; Cancel dismisses |
| `stella.acceptance` | Step handlers for `features/model/edit-stock.feature` |
| `stella.qa.ui-driver` | `right-click-element!`, dialog field entry, `click-ok-on-dialog!` |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/edit-stock.feature` |
| QA | `qa/procedures/edit-stock.qa.md` |

## Follow-up for coder

- Update `qa/procedures/place-stock.qa.md` assertions: icon shows name + minimum `0`, not initial value `0`.

## Out of scope

- Left-click selection or side inspector
- Showing initial or current simulation value on the icon
- Drag to reposition stocks
- Delete stock
- Simulation enforcement of bounds during run
- Units or documentation fields