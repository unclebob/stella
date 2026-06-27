# Implementation Plan: Edit Converter

**Task:** `edit-converter`  
**Status:** Approved  
**Depends on:** `connectors`

## User story

As a modeler, I can right-click a converter on the canvas and edit its name and the formula on its converter→flow connector.

## Locked decisions

Consistent with `edit-stock` and `edit-flow`: right-click on canvas opens a modal dialog; `OK` commits, `Cancel` dismisses; invalid edits keep the dialog open.

| Decision | Choice |
|---|---|
| Open editor | Right-click converter circle on canvas |
| Editor UX | Modal dialog; same pattern as **Edit Stock** / **Edit Flow** |
| Dialog title | `Edit Converter` |
| Editable fields | Name, Formula |
| Name target | Converter `:name` |
| Formula target | `:formula` on the **converter→flow** connector for this converter |
| No converter→flow link | Formula edit rejected; name edit still allowed |
| Formula default | Empty string on connector create |
| Formula use | Stored only; simulation does not evaluate it yet |
| Name rules | Non-empty, unique among converters |
| Formula rules | Any non-empty string when a converter→flow connector exists |
| Converter rename | Updates connector endpoint refs that reference the converter by name |
| Canvas display | Converter circle shows **name** only; connector arrow shows **name** and **formula** |
| Rejected edits | Dialog stays open; canvas unchanged |

## Observable behavior

1. Right-clicking a converter opens **Edit Converter** with current converter name and formula from its converter→flow connector (empty when unset).
2. **OK** with valid values updates the converter name and connector formula labels.
3. **Name** rename updates the converter circle label and connector origin refs.
4. **Formula** is persisted on the connector record, not the converter.
5. Setting formula when the converter has no converter→flow connector is rejected.
6. **Cancel** closes without changes.

## Model shape

Converter (unchanged keys; `:value` remains but is not shown on canvas after this feature):

```clojure
{:name "Converter1" :value "0" :x 100 :y 250}
```

Connector extension:

```clojure
{:name "Connector1"
 :from {:kind :converter :id "Converter1"}
 :to   {:kind :flow :id "Flow1"}
 :formula ""}          ; user-editable; not used in simulation yet
```

## Module changes

| Module | Change |
|---|---|
| `stella.model` | `:formula` on connectors; `set-converter-name`, `set-connector-formula-for-converter!`, `rename-converter-endpoints` |
| `stella.commands` | `set-converter-name!`, `set-converter-formula!` (writes connector `:formula`) |
| `stella.ui.edit-converter-dialog` | Modal dialog with Name and Formula |
| `stella.ui.canvas` | Converter shows name only; connector shows name + formula; right-click converter opens dialog |
| `stella.dispatch` | Dialog OK/Cancel wiring |
| `stella.acceptance` | Step handlers + `fixture-connector!` Given helper |
| `stella.qa.ui-driver` | Reuse right-click and dialog primitives |

## Testing

| Layer | Artifact |
|---|---|
| Gherkin | `features/model/edit-converter.feature` |
| QA | `qa/procedures/edit-converter.qa.md` |

## Follow-up for coder

- Update `qa/procedures/connectors.qa.md`: connector label may show formula; converter circle shows name only (not value `0`).

## Out of scope

- Formula evaluation or simulation
- Editing stock→converter connectors
- Editing connector name independently (follows auto `ConnectorN` unless renamed via separate future feature)
- Delete converter or connector
- Formula syntax validation beyond non-empty