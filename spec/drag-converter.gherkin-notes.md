# drag-converter Gherkin Notes

**Task:** `drag-converter`  
**Depends on:** `connectors`

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `move-converter!` | Set converter `:x` and `:y` when converter exists; else no-op |
| `fixture-connector!` | Given helper for connector Background (from `edit-converter`) |

Gherkin uses `move-converter!` directly. Drag gesture is QA-only.

## Assertions (direct model reads)

| Query | Use |
|---|---|
| `converter-position` | existing |
| `converter-count` | existing |
| `connector-from` / `connector-to` | existing; unchanged ids after move |

## Canvas rendering

Converter `layout-x` / `layout-y` match model `:x` / `:y` after move. Connector geometry recomputes from `endpoint-position` / `endpoint-anchor`.

## Step handler summary

| Step | Type |
|---|---|
| `Given converter <name> at <x> <y>` | existing `fixture-converter!` |
| `Given connector <c> runs from converter <f> to flow <t>` | `fixture-connector!` |
| `When I move converter <name> to <x> <y>` | `move-converter!` |
| `Then converter <name> should be at position <x> <y>` | existing |
| `Then converter <name> canvas position should be <x> <y>` | assert renderer layout |
| `Then the diagram converter count should be <n>` | existing |
| `Then connector <c> should run from converter <f> to flow <t>` | existing |

## UI / QA

- Primary-button drag from `:converter "<name>"` when placement mode is idle.
- Reuse `drag-element!` (from `drag-stock`).
- Drag disabled while a placement tool is armed.

## Acceptance pipeline

```bash
gherkin-parser features/model/drag-converter.feature build/acceptance/ir/model-drag-converter.json
```