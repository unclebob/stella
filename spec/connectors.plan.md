# Implementation Plan: Connectors

**Task:** `connectors`  
**Status:** Approved  
**Depends on:** `place-stock`, `connect-flow`

## User story

As a modeler, I can place converters on the diagram and connect influence arrows (connectors) between converters, stocks, and flows.

## Locked decisions

| Decision | Choice |
|---|---|
| Converter placement | Palette **Converter**; canvas click; disarm after place |
| Connector placement | Palette **Connector**; two-click origin then destination; disarm after connect |
| Converter display | Circle with name + value (`Converter1` / `0`) |
| Connector display | Thin directed arrow (distinct from flow pipe) |
| Naming | Auto `Converter1`, `Connector1`, … |

## Allowed connectors

```
Converter ──connector──► Flow
Stock     ──connector──► Converter
```

## Rejected

- Flow as connector origin
- Source or Sink as connector endpoint
- Connector involving clouds

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Converters, connectors, connector draft |
| `stella.commands` | Place converter; arm/select/connect connector with validation |
| `stella.ui.palette` | Converter and Connector buttons |
| `stella.ui.canvas` | Circle + thin arrow rendering; typed clicks |
| `stella.acceptance` | Handlers for two feature files |
| `stella.qa.hit-test` | Converter and connector targets |

## Testing

| Layer | Files |
|---|---|
| Gherkin | `place-converter.feature`, `connect-connector.feature` |
| QA | `qa/procedures/connectors.qa.md` |

## Implementation order

1. Model + commands + validation
2. Converter palette + rendering
3. Connector tool + two-click routing
4. Connector arrow rendering
5. Acceptance handlers
6. QA script