# Implementation Plan: CljFX Shell

**Task:** `cljfx-shell`  
**Status:** Approved  
**Scope:** JVM Clojure desktop app with one window, a standard menu bar, and a blank diagram canvas. No model logic, simulation, or file I/O yet.

## Goal

Launch Stella as a native desktop window that looks like the skeleton of the original app:

- A **menu bar** with conventional top-level menus
- A **blank canvas** filling the rest of the window
- Clean namespace layout so later features (stocks, flows, simulation) slot in without rework

## Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| UI framework | [CljFX](https://github.com/cljfx/cljfx) | Agreed stack; good widget + canvas support |
| Runtime | JVM Clojure (`deps.edn`) | CljFX requires full Clojure + JavaFX |
| Orchestration | Babashka (`bb.edn`) | SwarmForge tasks, acceptance pipeline, CLI helpers |
| Java version | 17+ | Stable OpenJFX support on macOS |
| State model | Single `atom` + CljFX renderer | Matches CljFX docs; simple for v1 |

**Project-shape note:** The constitution says Babashka, but the **application** runs on JVM Clojure. Babashka owns tooling only.

## Observable Behavior (v1)

1. Running the app opens a titled window (`"Stella"`).
2. Window shows a **menu bar** with: `File`, `Edit`, `View`, `Help`.
3. Menu items exist but are **disabled stubs** except:
   - `File → Quit` closes the app
   - `Help → About Stella` shows a simple informational dialog
4. Below the menu bar, a **blank white (or light gray) canvas** fills remaining client area.
5. Resizing the window resizes the canvas to match.
6. Closing the window (or Quit) exits cleanly.

## Project Layout

```
stella/
├── deps.edn
├── bb.edn
├── src/stella/
│   ├── main.clj
│   ├── app.clj
│   └── ui/
│       ├── root.clj
│       ├── menu.clj
│       └── canvas.clj
└── test/stella/ui/
    ├── menu_test.clj
    └── canvas_test.clj
```

Keep `swarmforge/` untouched.

## Dependencies (`deps.edn`)

```edn
{:paths ["src"]
 :deps {org.clojure/clojure {:mvn/version "1.12.0"}
        cljfx/cljfx {:mvn/version "<latest-stable>"}}
 :aliases
 {:run {:main-opts ["-m" "stella.main"]}
  :test {:extra-paths ["test"]
         :extra-deps {io.github.cognitect-labs/test-runner
                      {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
         :main-opts ["-m" "cognitect.test-runner"]
         :exec-args {:dirs ["test"]}}}}
```

Pin the CljFX version to whatever is current at implementation time.

## Module Responsibilities

### `stella.main`

- Set macOS-friendly JavaFX properties if needed.
- Call `cljfx.api/on-fx-thread` to start the app.
- Delegate to `stella.app/start!` and block until shutdown.

### `stella.app`

- Own the application `atom` (minimal state: `{:showing true}`).
- Create renderer with `fx/create-renderer` + `fx/mount-renderer`.
- Wire `::quit` and `::show-about` map-events to state changes / dialogs.
- Expose `start!` for `-main` and REPL dev.

### `stella.ui.root`

- Pure function `root-desc` → CljFX stage description.
- Layout: `border-pane` with menu bar on top, canvas in center.
- Default size ~1024×768; minimum size ~640×480.

### `stella.ui.menu`

- Pure function `menu-bar-desc` → `{:fx/type :menu-bar ...}`.
- Return menu structure as **data** so tests can assert labels and disabled flags without JavaFX.

| Menu | Items (all disabled except noted) |
|---|---|
| **File** | New, Open…, Save, Save As…, —, Quit ✓ |
| **Edit** | Undo, Redo, —, Cut, Copy, Paste |
| **View** | Zoom In, Zoom Out, Reset Zoom |
| **Help** | About Stella ✓ |

Handlers emit map-events (`::quit`, `::show-about`); no business logic here.

### `stella.ui.canvas`

- Pure function `canvas-desc` → `{:fx/type :pane ...}` or `:canvas`.
- v1: empty pane with light background; no drawing logic.
- Bind width/height to parent (grow with window).

## UI Composition

```
┌─────────────────────────────────────────────┐
│ File  Edit  View  Help                      │
├─────────────────────────────────────────────┤
│                                             │
│           blank canvas (center)             │
│                                             │
└─────────────────────────────────────────────┘
```

```clojure
{:fx/type :stage
 :title "Stella"
 :showing true
 :width 1024 :height 768
 :scene {:fx/type :scene
         :root {:fx/type :border-pane
                :top (menu-bar-desc handlers)
                :center (canvas-desc)}}}
```

## Testing Strategy

No acceptance tests for this slice. Unit-test **pure description builders** only.

### `menu_test.clj`

- Menu bar has exactly 4 top-level menus in order.
- Stub items are `:disable true`.
- `Quit` and `About Stella` are enabled.
- Quit action map-event is `::quit`.

### `canvas_test.clj`

- Canvas description uses expected fx-type and style (background color).
- Canvas is configured to grow (`vgrow`/`hgrow` or equivalent).

Do **not** spin up JavaFX in unit tests for v1.

## Babashka Tasks (`bb.edn`)

```edn
{:tasks
 {run  (shell "clojure -M:run")
  test (shell "clojure -M:test")}}
```

## Manual Verification Checklist

1. `bb run` opens the window.
2. Menu bar shows File / Edit / View / Help.
3. Disabled items are grayed out and do nothing.
4. Quit exits the process.
5. About shows a dialog with app name and a short placeholder line.
6. Canvas is blank and resizes with the window.
7. `bb test` passes.

## Out of Scope

- Gherkin features and acceptance pipeline
- End-to-end QA suite
- Diagram elements (stocks, flows, connectors)
- File New/Open/Save behavior
- Simulation engine
- Property tests
- Graal native-image packaging

## Implementation Order

1. Scaffold `deps.edn`, `bb.edn`, namespaces, `.gitignore` updates.
2. Write failing unit tests for `menu` and `canvas` descriptions.
3. Implement `stella.ui.menu` and `stella.ui.canvas` to pass tests.
4. Implement `stella.ui.root` composing menu + canvas.
5. Implement `stella.app` (atom, renderer, event handler).
6. Implement `stella.main` and confirm manual launch.
7. Add `bb run` / `bb test` tasks.