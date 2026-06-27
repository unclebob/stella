# select-objects Gherkin Notes

**Task:** `select-objects`  
**Depends on:** `place-stock`, `connectors`

## Diagram model extension

```clojure
{:selection #{}   ; set of {:kind :stock|:flow|:converter|:connector|:source|:sink :id "Name"}
```

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `click-select!` | When idle: if selected, remove; if unselected, clear others then add |
| `shift-click-select!` | When idle: toggle object in selection |
| `marquee-select!` | When idle: replace selection with objects intersecting axis-aligned rectangle |
| `clear-selection!` | Set `:selection` to `#{}` |

All selection commands are no-ops when `:placement-mode` is not `:idle`.

## Assertions

| Query | Use |
|---|---|
| `selected?` | new |
| `selection-count` | new |
| `nothing-selected?` | new; `selection-count` zero |

Intersection test for marquee uses each object's canvas bounds (stock rectangle, converter circle, flow pipe bounds, connector arrow bounds, cloud bounds).

## Canvas rendering

Selected objects render an additional grey outline (`#999` stroke) around their icon or link geometry. Marquee drag shows a semi-transparent rectangle overlay on the canvas.

## Step handler summary

| Step | Type |
|---|---|
| `Given a diagram model with stock <name> at <x> <y>` | existing |
| `Given converter <name> at <x> <y>` | existing |
| `When I click select <kind> <name>` | `click-select!` |
| `When I shift click select <kind> <name>` | `shift-click-select!` |
| `When I marquee select from <x1> <y1> to <x2> <y2>` | `marquee-select!` |
| `When I clear the selection` | `clear-selection!` |
| `When I arm the flow placement tool` | existing |
| `Then <kind> <name> should be selected` | assert `selected?` |
| `Then <kind> <name> should not be selected` | assert not selected |
| `Then the selection count should be <n>` | assert count |
| `Then nothing should be selected` | assert empty |

`select-objects-01` Background for converter row: add `Given converter Converter1 at 100 250` in scenario or extend Background only for that outline — converter row needs converter in diagram. Move converter Given into scenario outline Background using a second Background? 

Fix feature file: add converter to Background for simplicity, or split scenarios. I'll add converter to Background in the feature file - always present, doesn't hurt stock-only scenarios.

## UI / QA

- `click-element!` when idle should route through selection (may already click; verify selection outline appears).
- `shift-click-element!` — new QA primitive.
- `marquee-select!` — drag on `:canvas` background from point to point.
- `press-escape!` — keyboard clear.
- Assert grey outline via semantic element or visible style probe.
- Gherkin stays model/command level; marquee and Escape are QA-tested.

## Acceptance pipeline

```bash
gherkin-parser features/model/select-objects.feature build/acceptance/ir/model-select-objects.json
```