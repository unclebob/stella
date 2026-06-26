# Implementation Plan: Connect Flow

**Task:** `connect-flow`  
**Status:** Specified (pending handoff approval)  
**Depends on:** `place-stock`

## User story

As a modeler, I can connect two existing stocks with a directed flow and see the flow name and rate on the diagram.

## Locked decisions

| Decision | Choice |
|---|---|
| Connection UX | Two-click: source stock, then destination stock (A) |
| After connect | Disarm — re-click Flow for next connection |
| On-canvas display | Flow name + rate (`Flow1` / `0`) |
| Naming | Auto `Flow1`, `Flow2`, … |
| Default rate | `0` |
| Endpoints | Two existing stocks only (no clouds) |

## Observable behavior

1. Palette shows enabled **Flow** tool (requires two stocks on diagram for meaningful use).
2. Click **Flow** → armed.
3. Click source stock → draft source recorded.
4. Click destination stock → `Flow1` created, rendered, disarmed.
5. Reverse connection requires re-arming (Stock2 → Stock1 creates `Flow2`).
6. Arming alone creates no flow.
7. Same stock as source and destination is rejected.

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Flows map, flow draft, next flow id |
| `stella.commands` | `arm-flow-placement!`, `select-flow-source!`, `connect-flow!`, fixtures |
| `stella.ui.palette` | Flow tool button |
| `stella.ui.canvas` | Render flows; route stock clicks when `:placement-mode :flow` |
| `stella.acceptance` | Step handlers for `features/model/connect-flow.feature` |
| `stella.qa.hit-test` | Register flows by visible name |

## Testing

| Layer | Artifact |
|---|---|
| Unit | model, commands, flow rendering |
| Gherkin | `features/model/connect-flow.feature` |
| QA | `qa/procedures/connect-flow.qa.md` |

## Implementation order

1. Extend model + commands + unit tests
2. Flow palette + two-click interaction on canvas
3. Flow rendering (directed pipe, name, rate)
4. Acceptance step handlers
5. QA procedure script