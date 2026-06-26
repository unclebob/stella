# Implementation Plan: Place Stock

**Task:** `place-stock`  
**Status:** Specified (pending handoff approval)  
**Depends on:** `cljfx-shell`

## User story

As a modeler, I can place stocks on the diagram canvas one at a time and see each stock's name and initial value.

## Locked decisions

| Decision | Choice |
|---|---|
| Initiate placement | Click **Stock** in left palette |
| After each place | Disarm — re-click Stock for next stock |
| On-canvas display | Name + initial value (`Stock1` / `0`) |
| Naming | Auto `Stock1`, `Stock2`, … |
| Default value | `0` |

## Observable behavior

1. Palette shows an enabled **Stock** tool.
2. Clicking **Stock** arms placement mode.
3. Clicking the canvas places one stock at the click position.
4. Stock appears as a rectangle labeled with name and `0`.
5. Placement mode disarms after a successful place.
6. Clicking **Stock** again places `Stock2`, etc.
7. Arming without a canvas click places nothing.

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Diagram map with stocks, placement mode, next id |
| `stella.commands` | `default-diagram!`, `arm-stock-placement!`, `place-stock!` |
| `stella.ui.palette` | Stock tool button |
| `stella.ui.canvas` | Render stocks; handle armed click → `place-stock!` |
| `stella.ui.root` | Add palette to layout (border pane left + center canvas) |
| `stella.acceptance` | Step handlers for `features/model/place-stock.feature` |
| `stella.qa.hit-test` | Register stocks by visible name |

## Layout

```
┌────────────────────────────────────────────┐
│ File  Edit  View  Help                     │
├──────┬─────────────────────────────────────┤
│Stock │                                     │
│ [■]  │      ┌─────────┐                    │
│      │      │ Stock1  │                    │
│      │      │    0    │                    │
│      │      └─────────┘                    │
└──────┴─────────────────────────────────────┘
```

## Testing

| Layer | Artifact |
|---|---|
| Unit | `stella.model`, `stella.commands`, palette/canvas descriptions |
| Gherkin | `features/model/place-stock.feature` |
| QA | `qa/procedures/place-stock.qa.md` |

## Implementation order

1. `stella.model` + unit tests
2. `stella.commands` + unit tests
3. Palette UI + arm action
4. Canvas placement click + stock rendering
5. Acceptance step handlers + `bb accept`
6. QA hit-test + `qa/procedures/place-stock` script