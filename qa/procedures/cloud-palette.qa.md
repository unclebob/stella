# QA: Cloud palette

**Task:** `cloud-palette`  
**Suite:** cloud-palette

Verify Source and Sink palette tools highlight when armed, reset after each placement, and switch highlight when the user picks the other cloud tool.

## Preconditions

- Display available (headed UI).
- `place-cloud` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click the palette item `Source`.
3. Assert the palette item `Source` is visually active (highlighted).
4. Assert the palette item `Sink` is not visually active.
5. Click in region `:canvas` at offset `(-150, 0)` from `:center`.
6. Assert element `:source "Source1"` is visible.
7. Assert no palette item is visually active.
8. Click the palette item `Sink`.
9. Assert the palette item `Sink` is visually active.
10. Click in region `:canvas` at offset `(+150, 0)` from `:center`.
11. Assert element `:sink "Sink1"` is visible.
12. Assert no palette item is visually active.
13. Click the palette item `Sink` again.
14. Click in region `:canvas` at offset `(+200, +40)` from `:center`.
15. Assert element `:sink "Sink2"` is visible.
16. Quit the application using `File` → `Quit`.

## Pass criteria

- Arming Source or Sink highlights only that palette tool.
- Each successful source or sink placement disarms the palette (no tool stays highlighted).
- Placing a second sink requires clicking the Sink palette tool again between placements.
- No project API used.