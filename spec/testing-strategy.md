# Stella Testing Strategy

**Status:** Approved  
**Date:** 2026-06-26

## Purpose

Stella uses two complementary test layers with two distinct APIs:

| Layer | Tests | Driver | Audience |
|---|---|---|---|
| **Gherkin / Acceptance** | Model and simulation behavior | Model Test API | Coder, hardender (mutation) |
| **QA / E2E** | User-visible workflows | UI Driver (robot) | QA |

Gherkin describes **internal operations** at the object level. QA describes **user interactions and responses** in terms of mouse clicks, drags, double-clicks, keystrokes, and menu choices.

## Architecture

```
Gherkin features
  → step handlers
  → Model Test API
      → writes: stella.commands (shared with UI)
      → reads:  stella.model (assertions only)

QA procedures
  → QA scripts
  → UI Driver
      → user events → CljFX app
      → observations → visible screen state
```

### Invariants

1. Every state mutation — from Gherkin or from UI — flows through `stella.commands`.
2. Gherkin `Then` steps may read `stella.model` directly for assertions.
3. QA may not call `stella.commands`, `stella.model`, or any project API.
4. QA drives the running app only through user-interface events and reads only user-visible state.
5. Gherkin acceptance runs headless; QA runs against a headed app (TestFX robot).

## Shared Write Path: `stella.commands`

`stella.commands` is the sole mutation surface. CljFX event handlers and Gherkin step handlers both call it.

```clojure
(ns stella.commands)

(defn create-stock!    [model id name initial-value] ...)
(defn create-flow!     [model id from-stock to-stock] ...)
(defn connect!         [model source-id target-id] ...)
(defn set-parameter!   [model id value] ...)
(defn place-on-canvas! [model id x y] ...)
(defn run-simulation!  [model opts] ...)
```

Layout metadata (`place-on-canvas!`) is model data the UI renders; it is not a mouse click.

## Model Test API (Gherkin backend)

Thin adapter used only by acceptance step handlers. Lives under test-capable paths (`src/stella/test/` or equivalent).

```clojure
(ns stella.test.model-api
  (:require [stella.commands :as cmd]
            [stella.model :as model]))

;; Writes → commands
(defn create-stock [ctx id name value]
  (update ctx :model cmd/create-stock! id name value))

;; Reads → direct model access (assertions only)
(defn stock-value [ctx id]
  (model/stock-value (:model ctx) id))
```

### Gherkin step style

```gherkin
Given a stock <name> with initial value <initial>
When I connect converter <converter> to flow <flow>
And I run the simulation for <steps> steps
Then stock <name> should be <expected>
```

### Acceptance pipeline

- Feature files in `features/` using APS Gherkin subset.
- APS `gherkin-parser` → JSON IR → project entrypoint generator → generated tests.
- Step handlers in `src/stella/acceptance/` with regex-based parameter extraction.
- Normal run: `bb accept` (parse, generate, run).
- Mutation: hardender runs `gherkin-mutator` against Gherkin only, not QA.

## UI Driver (QA backend)

Robot layer driving the running CljFX app. Implemented with TestFX (or equivalent JavaFX robot), callable from Clojure.

### Targeting strategy (semantic default, coordinates fallback)

Resolution order:

1. **Visible text** — `"About Stella"`, `"Quit"`
2. **Element + name** — `:stock "Cats"`, `:flow "Births"`
3. **Relative region** — `:canvas :center`, field `"Initial value"`
4. **Coordinates** — `(200 150)` only when explicit or when semantic resolution is impossible

```clojure
(ns stella.qa.ui-driver)

(defn launch-app [opts] ...)
(defn quit-app [] ...)

;; Semantic (preferred)
(defn click-on [visible-text] ...)
(defn menu-choose [menu item] ...)
(defn click-element [kind name] ...)
(defn double-click-element [kind name] ...)
(defn type-into-field [label text] ...)

;; Relative
(defn click-in [region & opts] ...)

;; Coordinate fallback (explicit)
(defn click-at [x y] ...)
(defn drag-from [from & {:keys [to]}] ...)

;; Observations (user-visible only)
(defn window-title [] ...)
(defn visible-text [] ...)
(defn dialog-text [] ...)
(defn assert-visible-text [substring] ...)
(defn assert-element-shows [kind name text] ...)
(defn wait-for-text [text & {:keys [timeout-ms]}] ...)
```

### Hit-test index

`stella.qa.hit-test` maps semantic targets to screen bounds from rendered CljFX nodes, e.g. `[:stock "Cats"] → {:x :y :w :h}`. Rebuilt after each interaction. The robot clicks the center of those bounds — not a model API call.

### User-visible identity on diagram elements

| Element | Identity |
|---|---|
| Stock | name label on canvas |
| Flow | pipe label or endpoint context |
| Converter | circle label |
| Connector | resolved via source/target names |

### When coordinates are allowed

- Placing a new object on blank canvas
- Drag gestures (semantic endpoints, coordinate path)
- Pixel/color probes when text assertion is insufficient

Coordinates must be explicit in the QA procedure; the driver must not silently fall back to coordinates when semantic resolution fails.

## Directory layout

```
features/                         # specifier — Gherkin
  shell/
  model/
  simulation/

src/stella/
  model.clj                       # domain data + query functions
  commands.clj                    # sole mutation path
  simulation/
  app.clj                         # CljFX — calls commands
  acceptance/                     # APS step handlers
  test/
    model_api.clj                 # Gherkin backend
  qa/
    ui_driver.clj
    hit_test.clj

qa/
  procedures/                     # specifier — human-readable QA specs
  scripts/                        # QA — executable scripts

test/stella/                      # unit tests
```

## Run commands

```bash
bb test          # unit tests + Gherkin acceptance (headless)
bb qa            # E2E UI suite (headed, TestFX)
bb accept        # APS normal path: parse → generate → run
```

Run `bb test` on every agent handoff. Run `bb qa` at QA milestones (requires display).

## Scenario pairing

The same behavior may appear in both layers with different emphasis:

**Gherkin** proves model/simulation correctness (fast, mutation-tested).

**QA** proves the user can accomplish the task and see the result (slow, high confidence).

Example:

| Gherkin | QA |
|---|---|
| `Given a stock Cats with initial value 10` | Place stock via palette; name it Cats; set 10 in inspector |
| `When I run simulation for 5 steps` | Click Run; wait for completion |
| `Then stock Cats should be 42` | Assert stock Cats on canvas shows 42 |

## What each layer catches

| Failure | Gherkin | QA |
|---|---|---|
| Wrong simulation math | yes | maybe (if displayed) |
| Flow not connected in model | yes | no |
| Stock not rendered on canvas | no | yes |
| Menu Quit doesn't exit | no | yes |
| Dialog doesn't appear | no | yes |
| Drag-to-connect broken | no | yes |
| Wrong example value survives mutation | yes | no |

## Phasing

| Phase | Gherkin | QA |
|---|---|---|
| `cljfx-shell` | scaffold / skip until model exists | launch, menus, quit, about |
| `commands` + model v1 | create stock/flow via commands | place stock via palette + semantic click |
| simulation | run + assert values | click Run, read on-canvas label |
| connectors | connect via commands | drag between semantic endpoints |

## SwarmForge ownership

| Artifact | Specifier | Coder | QA |
|---|---|---|---|
| `features/*.feature` | writes | implements handlers | verifies |
| `stella.commands` | — | implements | — |
| `stella.test.model-api` | — | implements | — |
| `stella.qa.ui-driver` | — | implements primitives | uses in scripts |
| `qa/procedures/*.qa.md` | writes | — | converts to scripts |
| `qa/scripts/*.clj` | — | — | owns |

## Decisions log

| Decision | Choice |
|---|---|
| QA canvas targeting | Semantic default; coordinates only when explicit or unavoidable |
| Gherkin mutation path | Shared `stella.commands` for writes; direct `stella.model` read for assertions |
| QA vs Gherkin scope | Gherkin owns depth; QA owns golden-path user journeys |
| API separation | Strict — Gherkin never uses UI Driver; QA never uses Model Test API |