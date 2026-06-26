# connect-flow Gherkin Notes

**Task:** `connect-flow`  
**Depends on:** `place-stock`

## Diagram model extension

```clojure
{:flows {}                   ; id -> {:name :from-stock :to-stock :rate}
 :placement-mode :idle        ; :flow when armed
 :flow-draft nil               ; {:from "Stock1"} after source selected
 :next-flow-num 1}
```

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `arm-flow-placement!` | Set `:placement-mode :flow`, clear `:flow-draft` |
| `select-flow-source!` | When armed with no draft: set draft `{:from stock-id}` |
| `connect-flow!` | When draft has `:from`: create `FlowN` to destination stock, rate `"0"`, clear draft, set `:placement-mode :idle` |
| `fixture-flow!` | Given helper: create flow between stocks (connect-flow-02 setup) |
| `fixture-diagram-with-stocks!` | Given helper: two stocks at fixed positions (Background) |

`select-flow-source!` and `connect-flow!` are no-ops when not in the correct mode (supports connect-flow-03).

Selecting the same stock as source and destination is an error (reject; no flow created).

CljFX: palette **Flow** → `arm-flow-placement!`. Click stock → `select-flow-source!` or `connect-flow!` depending on draft state.

## Canvas rendering

- Directed pipe from source stock boundary to destination stock boundary.
- User-visible label: flow name (`Flow1`) and rate (`0`).
- Hit-test index: `[:flow "Flow1"]` for QA.

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | fixture stock (Background) |
| `Given flow <flow> runs from stock <from> to stock <to>` | `fixture-flow!` |
| `When I arm the flow placement tool` | `arm-flow-placement!` |
| `When I select stock <name> as the flow source` | `select-flow-source!` |
| `When I select stock <name> as the flow destination` | `connect-flow!` |
| `Then the diagram should contain flow <flow>` | assert flow exists |
| `Then flow <flow> should run from stock <from> to stock <to>` | assert endpoints |
| `Then flow <flow> rate should be <rate>` | assert `:rate` |
| `Then the diagram flow count should be <count>` | assert count |
| `Then the flow placement tool should be disarmed` | assert `:placement-mode :idle` |

## UI additions

- Enabled **Flow** button in palette.
- Two-click connection: source stock, then destination stock.
- Disarm after successful connection (user decision A).

## Out of scope

- Clouds, converters, simulation, edit rate, delete flow.
- Drag-to-connect (deferred).
- Placing flow on canvas without stock endpoints.