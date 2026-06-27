# Implementation Plan: Edit Flow

**Task:** `edit-flow`  
**Status:** Approved  
**Depends on:** `connect-flow`

## User story

As a modeler, I can right-click a flow on the canvas and edit its name and constant rate in a dialog.

## Locked decisions

Consistent with `edit-stock`: right-click on canvas opens a modal dialog; `OK` commits, `Cancel` dismisses; invalid edits keep the dialog open.

| Decision | Choice |
|---|---|
| Open editor | Right-click flow label on canvas (same gesture as stocks) |
| Editor UX | Modal dialog (not a side inspector); same pattern as **Edit Stock** |
| Dialog title | `Edit Flow` |
| Editable fields | Name, Rate |
| Commit | `OK` applies all field values |
| Cancel | `Cancel` closes dialog with no model changes |
| Name rules | Non-empty, unique among flows |
| Rate rules | Non-empty numeric string (parseable as a number) |
| Rate meaning | Constant flow rate; no connector required |
| Flow rendering | **Pipe** — thick tubular link between endpoints; visibly distinct from thin connector arrows |
| Canvas display | Name and rate labels on the pipe |
| Flow rename | Updates connector endpoint refs that target the flow by name |
| Rejected edits | Dialog stays open; field values unchanged on canvas |

## Observable behavior

1. Right-clicking a flow opens the **Edit Flow** dialog populated with current name and rate.
2. **OK** with valid values updates the model and flow pipe labels.
3. **Name** rename updates the canvas name label and semantic hit-test identity (`:flow "<name>"`).
4. **Rate** update changes the rate label on the pipe.
5. Invalid edits (duplicate name, non-numeric rate) keep the dialog open and leave the canvas unchanged.
6. **Cancel** closes the dialog without applying edits.
7. Renaming a flow updates any connector whose destination is that flow.

## Model shape

No new keys. Flow remains:

```clojure
{:name "Flow1"
 :from {:kind :stock :id "Stock1"}
 :to   {:kind :stock :id "Stock2"}
 :rate "0"}
```

## Module changes

| Module | Change |
|---|---|
| `stella.model` | `set-flow-name`, `set-flow-rate`, `rename-flow-endpoints` (connectors referencing `:kind :flow`) |
| `stella.commands` | `set-flow-name!`, `set-flow-rate!` |
| `stella.ui.edit-flow-dialog` | Modal dialog with labeled fields and OK/Cancel |
| `stella.ui.canvas` | Render flows as pipes; right-click on flow opens edit dialog |
| `stella.dispatch` | Dialog OK commits through commands; Cancel dismisses |
| `stella.acceptance` | Step handlers for `features/model/edit-flow.feature` |
| `stella.qa.ui-driver` | Reuse `right-click-element!`, `type-into-dialog-field!`, `click-ok-on-dialog!` |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/edit-flow.feature` |
| QA | `qa/procedures/edit-flow.qa.md` |

## Out of scope

- Simulation or connector override of stored rate
- Editing flow endpoints (from/to)
- Delete flow
- Left-click selection or side inspector
- Edit converter value (separate feature)
- Blocking edits when a connector targets the flow