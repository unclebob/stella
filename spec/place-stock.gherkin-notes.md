# place-stock Gherkin Notes

**Task:** `place-stock`

## Prerequisites

- `cljfx-shell` complete (window, menu bar, blank canvas, palette region).
- Introduce `stella.model` diagram state and `stella.commands` mutation path per `spec/testing-strategy.md`.

## Diagram model

```clojure
{:stocks {}                  ; id -> {:name :initial-value :x :y}
 :placement-mode :idle       ; :stock when armed, :idle after place or cancel
 :next-stock-num 1}           ; generates Stock1, Stock2, ...
```

Shell state remains separate or nested under app state; Gherkin `Given an empty diagram model` fixtures a diagram with no stocks and `:placement-mode :idle`.

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `arm-stock-placement!` | Set `:placement-mode :stock` |
| `place-stock!` | When armed: create next auto-named stock at `<x> <y>` with initial value `"0"`, set `:placement-mode :idle` |
| `default-diagram!` | Empty diagram for Given step |

`place-stock!` is a no-op on stock data when not armed (supports place-stock-03).

CljFX palette **Stock** button calls `arm-stock-placement!`. Canvas click at `(x, y)` calls `place-stock!`.

## Canvas rendering

Each stock renders as a rectangle with two user-visible lines:

- Line 1: name (`Stock1`)
- Line 2: initial value (`0`)

Hit-test index registers `[:stock "Stock1"]` for QA semantic targeting.

## Step handler summary

| Step | Type |
|---|---|
| `Given an empty diagram model` | `default-diagram!` |
| `When I arm the stock placement tool` | `arm-stock-placement!` |
| `When I place a stock at <x> <y>` | `place-stock!` |
| `Then the diagram should contain stock <name>` | assert stock exists |
| `Then stock <name> should be at position <x> <y>` | assert coordinates |
| `Then stock <name> initial value should be <value>` | assert `:initial-value` |
| `Then the diagram stock count should be <count>` | assert `(count stocks)` |
| `Then the stock placement tool should be disarmed` | assert `:placement-mode :idle` |

## UI additions (coder)

- Left palette pane with enabled **Stock** tool button.
- Armed state: optional cursor change on canvas (not required for acceptance).
- Disarm after each successful placement (user decision A).

## Out of scope

- Rename or edit initial value (`edit-stock` follow-up).
- Drag to reposition, delete stock.
- Flows, converters, simulation, save/load.