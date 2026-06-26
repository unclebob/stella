# Implementation Plan: Cloud Endpoints

**Task:** `cloud-endpoints`  
**Status:** Specified (pending handoff approval)  
**Depends on:** `place-stock`, `connect-flow`

## User story

As a modeler, I can place sources and sinks on the diagram and connect them to stocks with flows.

## Locked decisions

| Decision | Choice |
|---|---|
| Palette tools | Separate **Source** and **Sink** buttons |
| Placement | Canvas click; disarm after each place (same as stocks) |
| Flow connection | Two-click; extend `connect-flow` for typed endpoints |
| Source role | Flow origin only |
| Sink role | Flow destination only |
| Display | Cloud shape + name (no value) |
| Naming | Auto `Source1`, `Sink1`, … |
| Flow rate | Default `0` |

## Allowed flows (this slice)

```
Source ──flow──► Stock ──flow──► Sink
Stock  ──flow──► Stock          (existing)
```

## Rejected connections

- Sink as flow source
- Source as flow destination
- Source ──► Sink (direct)
- Source ──► Source, Sink ──► Sink

## Module changes

| Module | Change |
|---|---|
| `stella.model` | Sources, sinks, typed flow endpoints |
| `stella.commands` | Place source/sink; validate flow endpoints |
| `stella.ui.palette` | Source and Sink buttons |
| `stella.ui.canvas` | Cloud rendering; typed click routing |
| `stella.acceptance` | Handlers for two new feature files |
| `stella.qa.hit-test` | `[:source ...]` `[:sink ...]` |

## Testing

| Layer | Files |
|---|---|
| Gherkin | `features/model/place-cloud.feature`, `features/model/connect-cloud-flow.feature` |
| QA | `qa/procedures/cloud-endpoints.qa.md` |

## Implementation order

1. Model + commands for sources/sinks + validation rules
2. Palette + placement rendering
3. Generalize flow connection for typed endpoints
4. Acceptance handlers
5. QA script