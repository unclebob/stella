# edit-flow Gherkin Notes

**Task:** `edit-flow`  
**Depends on:** `connect-flow`

## Flow model (unchanged)

```clojure
{:name "Flow1"
 :from {:kind :stock :id "Stock1"}
 :to   {:kind :stock :id "Stock2"}
 :rate "0"}          ; constant rate; editable via edit-flow
```

Rename updates `:name` only; internal id (`:flow-1`) stays stable. Connector `:to` / `:from` refs that use `{:kind :flow :id "Flow1"}` must be updated to the new name.

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `set-flow-name!` | Rename when non-empty and unique among flows; update connector flow refs; else reject |
| `set-flow-rate!` | Update `:rate` when non-empty and numeric; else reject |

Gherkin does not use a select step; edits target flow by current name.

## Assertions (direct model reads)

| Query | Use |
|---|---|
| `flow-rate` | existing |
| `flow-exists?` | existing |
| `connector-from` / `connector-to` | existing; verify rename propagation |

`Then the flow edit should be rejected` means the prior diagram state for the targeted field is unchanged (same pattern as `edit-stock`).

## Canvas rendering

Flow pipe label (existing):

| Label | Content |
|---|---|
| Top | Flow name |
| Bottom | Rate |

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | existing `fixture-stock!` |
| `Given flow <flow> runs from stock <from> to stock <to>` | existing `fixture-flow!` |
| `Given converter <name> at <x> <y>` | existing `fixture-converter!` |
| `When I arm the connector placement tool` | existing |
| `When I select converter <n> as the connector origin` | existing |
| `When I select flow <n> as the connector destination` | existing |
| `When I set flow <name> name to <new_name>` | `set-flow-name!` |
| `When I set flow <name> rate to <rate>` | `set-flow-rate!` |
| `Then the diagram should contain flow <flow>` | existing |
| `Then flow <flow> rate should be <rate>` | existing |
| `Then connector <c> should run from converter <f> to flow <t>` | existing |
| `Then the flow edit should be rejected` | assert last edit left field unchanged |
| `Then flow <name> canvas name should be <text>` | assert renderer name label |
| `Then flow <name> canvas rate should be <rate>` | assert renderer rate label |

## UI / QA

Same interaction model as `edit-stock`: right-click semantic canvas target → modal dialog → `OK` / `Cancel`.

- Right-click `:flow "<name>"` on the canvas opens modal dialog titled `Edit Flow`.
- Dialog field labels (exact): `Name`, `Rate`.
- `OK` commits all fields; `Cancel` dismisses without changes.
- Empty `Rate` in the dialog is rejected (QA procedure); Gherkin covers non-numeric rejection.
- Invalid edits keep the dialog open and leave the canvas unchanged.
- QA primitives: reuse `right-click-element!`, `type-into-dialog-field!`, `click-ok-on-dialog!` with title `Edit Flow`.
- Gherkin remains model/command level; gesture and dialog are QA-only.

## Acceptance pipeline

```bash
gherkin-parser features/model/edit-flow.feature build/acceptance/ir/model-edit-flow.json
```