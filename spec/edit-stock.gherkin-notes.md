# edit-stock Gherkin Notes

**Task:** `edit-stock`  
**Depends on:** `place-stock`

## Stock model extension

```clojure
{:name "Stock1"
 :initial-value "0"
 :min-value "0"       ; default on place-stock and fixture-stock
 :max-value nil       ; nil = unbounded
 :x 200
 :y 150}
```

Rename updates `:name` only; internal id (`:stock-1`) stays stable.

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `set-stock-name!` | Rename when non-empty and unique; else reject |
| `set-stock-initial-value!` | Update when within min/max; else reject |
| `set-stock-current-value!` | Update current value, clamped to min/max |
| `set-stock-min!` | Update minimum; reject when min > current initial or min > max |
| `set-stock-max!` | Set maximum string; reject when max < current initial or max < min |
| `clear-stock-max!` | Set `:max-value` to `nil` |

Gherkin does not use `select-stock!`; edits target stock by current name.

## Assertions (direct model reads)

| Query | Use |
|---|---|
| `stock-initial-value` | existing |
| `stock-current-value` | current simulation/display value after clamping |
| `stock-min-value` | new |
| `stock-max-value` | new; `nil` means unbounded |
| `stock-named?` | existence by display name |

`Then the stock edit should be rejected` means the prior diagram state for the targeted field is unchanged.

## Canvas rendering

Stock icon (80×50 v1) layout:

| Region | Content | Style |
|---|---|---|
| Center | Name | Prominent / primary label |
| Bottom-left | Minimum value | Small font; width < ¼ icon |
| Bottom-right | Maximum value, or empty when unbounded | Small font; width < ¼ icon |

Initial value is not drawn on the icon. Replaces place-stock stacked name + initial value display.

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | existing `fixture-stock!` |
| `When I set stock <name> name to <new_name>` | `set-stock-name!` |
| `When I set stock <name> initial value to <value>` | `set-stock-initial-value!` |
| `When I set stock <name> current value to <value>` | `set-stock-current-value!` |
| `When I set stock <name> minimum to <min>` | `set-stock-min!` |
| `When I set stock <name> maximum to <max>` | `set-stock-max!` |
| `When I clear stock <name> maximum` | `clear-stock-max!` |
| `Then stock <name> minimum should be <min>` | assert `:min-value` |
| `Then stock <name> maximum should be <max>` | assert `:max-value` |
| `Then stock <name> current value should be <value>` | assert clamped current value |
| `Then stock <name> should have no maximum` | assert `nil` max |
| `Then the stock edit should be rejected` | assert last edit left field unchanged (pair with prior When) |
| `Then stock <name> canvas name should be <text>` | assert renderer name label |
| `Then stock <name> canvas minimum should be <min>` | assert renderer min label |
| `Then stock <name> canvas maximum should be <max>` | assert renderer max label |
| `Then stock <name> should display no maximum on canvas` | assert empty max label |

## UI / QA

- Right-click `:stock "<name>"` on the canvas opens modal dialog titled `Edit Stock`.
- Dialog field labels (exact): `Name`, `Initial value`, `Current value`, `Minimum`, `Maximum`.
- `OK` commits all fields; `Cancel` dismisses without changes.
- Empty `Maximum` means unbounded (`clear-stock-max!`).
- Current value edits clamp to minimum or maximum instead of being rejected.
- Invalid edits keep the dialog open and leave the canvas unchanged.
- QA primitives: `right-click-element!`, `type-into-dialog-field!`, `click-ok-on-dialog!` with title `Edit Stock`.
- Gherkin remains model/command level; gesture and dialog are QA-only.

## Acceptance pipeline

```bash
gherkin-parser features/model/edit-stock.feature build/acceptance/ir/model-edit-stock.json
bb gherkin-ir-dry-checker build/acceptance/ir/model-edit-stock.json build/acceptance/dry/model-edit-stock.json
```
