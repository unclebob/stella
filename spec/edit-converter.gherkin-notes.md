# edit-converter Gherkin Notes

**Task:** `edit-converter`  
**Depends on:** `connectors`

## Connector model extension

```clojure
{:name "Connector1"
 :from {:kind :converter :id "Converter1"}
 :to   {:kind :flow :id "Flow1"}
 :formula ""}     ; default on create; edited via Edit Converter dialog
```

Formula lives on the **connector**, not the converter. The Edit Converter dialog is the UX entry point; `set-converter-formula!` finds the converter→flow connector whose `:from` matches the converter and updates `:formula`.

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `fixture-connector!` | Given helper: create connector between typed endpoints |
| `set-converter-name!` | Rename when non-empty and unique; update connector converter refs; else reject |
| `set-converter-formula!` | Set `:formula` on converter→flow connector for converter; reject when no such connector or empty formula |

## Assertions (direct model reads)

| Query | Use |
|---|---|
| `connector-formula` | new; `nil` or `""` means unset |
| `connector-from` / `connector-to` | existing |
| `converter-exists?` | existing |

`Then the converter edit should be rejected` — prior field unchanged (same pattern as `edit-stock`).

## Canvas rendering

| Element | Labels |
|---|---|
| Converter circle | Name only (drop value `0` display) |
| Connector arrow | Name + formula (small font) |

## Step handler summary

| Step | Type |
|---|---|
| `Given connector <c> runs from converter <f> to flow <t>` | `fixture-connector!` |
| `When I set converter <name> name to <new_name>` | `set-converter-name!` |
| `When I set converter <name> formula to <formula>` | `set-converter-formula!` |
| `Then connector <c> formula should be <formula>` | assert `:formula` |
| `Then connector <c> should have no formula` | assert empty/nil formula |
| `Then connector <c> canvas formula should be <formula>` | assert renderer formula label |
| `Then converter <name> canvas name should be <text>` | assert renderer name label |
| `Then the converter edit should be rejected` | assert last edit rejected |
| `Then connector <c> should run from converter <f> to flow <t>` | existing |

## UI / QA

- Right-click `:converter "<name>"` opens modal **Edit Converter**.
- Dialog field labels (exact): `Name`, `Formula`.
- `OK` commits; `Cancel` dismisses.
- Invalid edits keep dialog open.
- Gherkin is model/command level; gesture and dialog are QA-only.

## Acceptance pipeline

```bash
gherkin-parser features/model/edit-converter.feature build/acceptance/ir/model-edit-converter.json
```