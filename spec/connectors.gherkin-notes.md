# connectors Gherkin Notes

**Task:** `connectors`  
**Depends on:** `place-stock`, `connect-flow`

## Diagram model extension

```clojure
{:converters {"Converter1" {:name "Converter1" :value "0" :x 100 :y 250}}
 :connectors {"Connector1" {:name "Connector1"
                           :from {:kind :converter :id "Converter1"}
                           :to   {:kind :flow :id "Flow1"}}}
 :placement-mode :idle   ; :converter :connector
 :connector-draft nil    ; {:from {:kind :converter :id ...}}
 :next-converter-num 1
 :next-connector-num 1}
```

Connectors are **influence arrows** (cause → effect). They are not material flows.

## Endpoint rules

| Kind | Connector origin | Connector destination |
|---|---|---|
| Converter | yes | yes |
| Stock | yes | yes |
| Flow | no | yes |
| Source | no | no |
| Sink | no | no |

Invalid selections reject the connection; no connector created. Rejecting an invalid origin leaves the tool armed (connect-connector-03).

## Placement commands

| Command | Effect |
|---|---|
| `arm-converter-placement!` | `:placement-mode :converter` |
| `place-converter!` | Create `ConverterN` at position with value `"0"`, disarm |

## Connector commands

| Command | Effect |
|---|---|
| `arm-connector-placement!` | `:placement-mode :connector`, clear draft |
| `select-connector-origin!` | Record typed origin when valid kind |
| `connect-connector!` | When draft has origin: create `ConnectorN` to valid destination, disarm |

## Canvas rendering

- **Converter:** circle with name and value (`Converter1` / `0`).
- **Connector:** thin directed arrow, visually distinct from flow pipes.
- Hit-test: `[:converter "Converter1"]`, `[:connector "Connector1"]` (connector may resolve by name label near arrow).

## Step handlers

| Step | Handler |
|---|---|
| `When I arm the converter placement tool` | `arm-converter-placement!` |
| `When I place a converter at <x> <y>` | `place-converter!` |
| `Then the diagram should contain converter <name>` | assert |
| `Then converter <name> value should be <value>` | assert `:value` |
| `Then the diagram converter count should be <n>` | count |
| `Then the converter placement tool should be disarmed` | mode idle |
| `When I arm the connector placement tool` | `arm-connector-placement!` |
| `When I select converter <n> as the connector origin` | `select-connector-origin!` |
| `When I select stock <n> as the connector origin` | `select-connector-origin!` |
| `When I select flow <n> as the connector origin` | `select-connector-origin!` (rejected) |
| `When I select flow <n> as the connector destination` | `connect-connector!` |
| `When I select converter <n> as the connector destination` | `connect-connector!` |
| `When I select source <n> as the connector destination` | `connect-connector!` (rejected) |
| `Then connector <c> should run from converter <f> to flow <t>` | assert endpoints |
| `Then connector <c> should run from stock <f> to converter <t>` | assert endpoints |
| `Then the diagram connector count should be <n>` | count |
| `Then the connector placement tool should be armed` | mode :connector |
| `Then the connector placement tool should be disarmed` | mode idle |

## Out of scope

- Connector formulas, simulation, edit value.
- Converter → converter (defer).
- Delete converter or connector.