# QA: Control panel

**Task:** `run-simulation`  
**Suite:** control-panel

Verify the control panel at the top of the window exposes a Step button and simulation time, and ignores diagram placement and drag like the palette.

## Preconditions

- Display available (headed UI).
- `place-stock` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Assert semantic region `:control-panel` is visible.
3. Assert visible text includes `Step`.
4. Assert visible text in region `:control-panel` includes `0` (simulation time at start).
5. Click the palette item `Stock`.
6. Click in region `:control-panel` at `:center`.
7. Assert the diagram canvas region `:canvas` is still empty (no stock placed).
8. Place stock `Stock1`: click palette `Stock`, click in region `:canvas` at `:center`.
9. Drag from region `:control-panel` at `:center` to offset `(+100, 0)` from `:control-panel` `:center`.
10. Assert element `:stock "Stock1"` is still visible at its original canvas position (drag within control panel did not move the stock).
11. Quit the application using `File` → `Quit`.

## Pass criteria

- Control panel is visible at the top of the window below the menu bar.
- Step button and simulation time `0` are user-visible before any step.
- Armed stock placement and stock drag do not occur when the pointer interaction starts in `:control-panel`.
- No project API used.