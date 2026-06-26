# cloud-endpoints Gherkin Notes

**Task:** `cloud-endpoints`  
**Depends on:** `place-stock`, `connect-flow`

## Diagram model extension

```clojure
{:sources {"Source1" {:name "Source1" :x 50 :y 150}}
 :sinks   {"Sink1"   {:name "Sink1"   :x 400 :y 150}}
 :flows   {"Flow1"   {:name "Flow1"
                      :from {:kind :source :id "Source1"}
                      :to   {:kind :stock  :id "Stock1"}
                      :rate "0"}}
 :placement-mode :idle   ; :source :sink :flow
 :next-source-num 1
 :next-sink-num 1}
```

Stock-to-stock flows from `connect-flow` use `{:kind :stock}` at both ends.

## Endpoint rules

| Endpoint | Allowed as flow source | Allowed as flow destination |
|---|---|---|
| Source | yes | no |
| Stock | yes | yes |
| Sink | no | yes |

Invalid selections are rejected; no flow created. Draft cleared on rejection for destination errors; source-only rejection leaves tool armed (connect-cloud-03).

## Placement commands

| Command | Effect |
|---|---|
| `arm-source-placement!` | `:placement-mode :source` |
| `place-source!` | Create `SourceN` at position, disarm |
| `arm-sink-placement!` | `:placement-mode :sink` |
| `place-sink!` | Create `SinkN` at position, disarm |

## Flow commands (extend existing)

Generalize `select-flow-source!` and `connect-flow!` to accept typed endpoints:

| Command | Accepts |
|---|---|
| `select-flow-source!` | `:source` or `:stock` id |
| `connect-flow!` | `:stock` or `:sink` id as destination |

Fixture Given helpers:

- `fixture-source!`, `fixture-sink!` for Background setup in connect-cloud-flow.

## Canvas rendering

- **Source / Sink:** cloud shape (not rectangle), name label only (no accumulation value).
- **Flows:** extend routing to cloud boundaries; arrow direction preserved.
- Hit-test: `[:source "Source1"]`, `[:sink "Sink1"]`.

## Step handlers (new)

| Step | Command / assert |
|---|---|
| `When I arm the source placement tool` | `arm-source-placement!` |
| `When I place a source at <x> <y>` | `place-source!` |
| `Then the diagram should contain source <name>` | assert source |
| `Then source <name> should be at position <x> <y>` | assert coords |
| `Then the diagram source count should be <n>` | count sources |
| `Then the source placement tool should be disarmed` | mode idle |
| `When I arm the sink placement tool` | `arm-sink-placement!` |
| `When I place a sink at <x> <y>` | `place-sink!` |
| `Then the diagram should contain sink <name>` | assert sink |
| `Then sink <name> should be at position <x> <y>` | assert coords |
| `Given source <name> at <x> <y>` | fixture |
| `Given sink <name> at <x> <y>` | fixture |
| `When I select source <name> as the flow source` | `select-flow-source!` |
| `When I select sink <name> as the flow destination` | `connect-flow!` |
| `Then flow <flow> should run from source <from> to stock <to>` | assert endpoints |
| `Then flow <flow> should run from stock <from> to sink <to>` | assert endpoints |
| `Then the flow placement tool should be armed` | mode :flow |

## Out of scope

- Cloud-to-cloud flows.
- Source-to-sink direct flow.
- Converters, simulation, edit/delete.