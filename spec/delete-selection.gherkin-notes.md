# delete-selection Gherkin Notes

**Task:** `delete-selection`  
**Depends on:** `select-objects`

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `delete-selection!` | When idle: remove all selected objects plus cascade dependents; clear `:selection`; no-op when selection empty |

Reuses selection commands from `select-objects`:

| Command | Effect |
|---|---|
| `click-select!` | existing |
| `shift-click-select!` | existing |

## Cascade implementation notes

Expand delete set before removal:

1. Start from `:selection`.
2. For each stock/source/sink/flow/converter in the set, add connectors and flows that reference it per cascade table.
3. Repeat until stable (handles stock + flow both selected).
4. Remove all entries from their collections.

Endpoint refs use `{:kind … :id …}` name strings.

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | existing |
| `Given flow <flow> runs from stock <from> to stock <to>` | existing |
| `Given converter <name> at <x> <y>` | existing |
| `Given connector <c> runs from converter <f> to flow <t>` | `fixture-connector!` |
| `Given connector <c> runs from stock <f> to converter <t>` | `fixture-connector!` |
| `When I click select <kind> <name>` | `click-select!` |
| `When I shift click select <kind> <name>` | `shift-click-select!` |
| `When I delete the selection` | `delete-selection!` |
| `Then the diagram should not contain stock <name>` | assert missing |
| `Then the diagram should not contain flow <flow>` | existing negative assert |
| `Then the diagram should not contain converter <name>` | assert missing |
| `Then the diagram should not contain connector <name>` | assert missing |
| `Then the diagram stock count should be <n>` | existing |

Add negative existence steps for flow/converter/connector if not present.

## UI / QA

- `press-delete!` and `press-backspace!` both invoke delete when diagram has focus.
- QA verifies both keys in procedure (one scenario each or both in one run).
- Keys ignored while modal dialog (e.g. **Edit Stock**) has focus.
- Gherkin uses `delete-selection!` only.

## Acceptance pipeline

```bash
gherkin-parser features/model/delete-selection.feature build/acceptance/ir/model-delete-selection.json
```