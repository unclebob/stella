# drag-stock Gherkin Notes

**Task:** `drag-stock`  
**Depends on:** `place-stock`

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `move-stock!` | Set stock `:x` and `:y` when stock exists; else no-op |

Gherkin uses `move-stock!` directly (model/command level). The drag gesture is QA-only.

## Assertions (direct model reads)

| Query | Use |
|---|---|
| `stock-position` | existing |
| `stock-count` | existing |
| `flow-endpoints` | existing; unchanged ids after move |

## Canvas rendering

Stock `layout-x` / `layout-y` match model `:x` / `:y` after move. Flow and connector geometry recomputes from `endpoint-position` / `endpoint-anchor`.

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | existing `fixture-stock!` |
| `Given flow <flow> runs from stock <from> to stock <to>` | existing `fixture-flow!` |
| `When I move stock <name> to <x> <y>` | `move-stock!` |
| `Then stock <name> should be at position <x> <y>` | assert coordinates |
| `Then stock <name> canvas position should be <x> <y>` | assert renderer layout |
| `Then the diagram stock count should be <n>` | existing |
| `Then flow <flow> should run from stock <from> to stock <to>` | existing |

## UI / QA

- Primary-button drag from `:stock "<name>"` to a canvas position when placement mode is idle.
- Drag while a placement tool is armed must not move stocks (QA verifies Stock1 stays put).
- QA primitive: `drag-element!` from semantic stock target to region `:canvas` at offset or absolute position.
- Gherkin remains model/command level.

## Acceptance pipeline

```bash
gherkin-parser features/model/drag-stock.feature build/acceptance/ir/model-drag-stock.json
```