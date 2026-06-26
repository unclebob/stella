# cljfx-shell Gherkin Notes

**Task:** `cljfx-shell`

## Shell object model (Gherkin backend)

Until the diagram model exists, Gherkin targets a **shell application object** separate from the future `stella.model` diagram.

```clojure
{:showing true
 :window-title "Stella"
 :about-visible false
 :about-text "..."
 :menu-bar [...]           ; nested menu/item metadata with :disabled flags
 :diagram-elements []}      ; empty for this slice
```

## Commands (writes via `stella.commands`)

| Command | Effect |
|---|---|
| `default-shell!` | Return initial shell state (used by Given step) |
| `show-about!` | Set `:about-visible true`, populate `:about-text` |
| `quit!` | Set `:showing false` |

CljFX UI handlers for `Quit` and `About Stella` must call the same commands.

## Assertions (direct shell reads)

Step handlers read shell state directly for `Then` steps (per testing strategy).

## Step handler summary

| Step | Type |
|---|---|
| `Given a default shell application` | fixture via `default-shell!` |
| `Then the shell menu bar should include <menu>` | assert top-level menu label |
| `Then the shell menu item <item> should be disabled` | assert `:disabled true` |
| `Then the shell menu item <item> should be enabled` | assert `:disabled false` |
| `Then the shell window title should be <title>` | assert `:window-title` |
| `Then the shell should be showing` | assert `:showing true` |
| `Then the shell should not be showing` | assert `:showing false` |
| `When I show the about dialog` | `show-about!` |
| `Then the about dialog should be visible` | assert `:about-visible true` |
| `Then the about dialog text should include <app_name>` | assert substring in `:about-text` |
| `When I quit the shell application` | `quit!` |
| `Then the diagram canvas should be empty` | assert `:diagram-elements` empty |

## Acceptance pipeline

Coder installs APS normal path at startup:

```bash
gherkin-parser <feature> build/acceptance/ir/<name>.json
# gherkin-ir-dry-checker <ir> build/acceptance/dry/<name>.json  ; when available
acceptance-entrypoint-generator ...
bb accept   # parse, generate, run
```

Gherkin runs headless against shell state — no JavaFX required for acceptance tests in this slice.