# QA: Delete selection

**Task:** `delete-selection`  
**Suite:** delete-selection

Verify Delete and Backspace remove selected diagram objects and cascade dependent links.

## Preconditions

- Display available (headed UI).
- `select-objects` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stocks `Stock1` and `Stock2` per `connect-flow` QA offsets.
3. Connect flow: palette `Flow`, click `:stock "Stock1"`, click `:stock "Stock2"`.
4. Wait until element `:flow "Flow1"` is visible (timeout 2 seconds).
5. Click element `:stock "Stock1"`.
6. Assert element `:stock "Stock1"` shows a grey selection outline.
7. Press `Delete`.
8. Assert element `:stock "Stock1"` is not visible (timeout 2 seconds).
9. Assert element `:flow "Flow1"` is not visible (cascade).
10. Assert element `:stock "Stock2"` is visible.
11. Place converter: palette `Converter`, click region `:canvas` at offset `(-60, +100)` from `:center`.
12. Connect stock to converter: palette `Connector`, click `:stock "Stock2"`, click `:converter "Converter1"`.
13. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
14. Click element `:converter "Converter1"`.
15. Press `Backspace`.
16. Assert element `:converter "Converter1"` is not visible (timeout 2 seconds).
17. Assert element `:connector "Connector1"` is not visible (cascade).
18. Assert element `:stock "Stock2"` is still visible.
19. Click palette `Flow` (arm placement).
20. Press `Delete`.
21. Assert element `:stock "Stock2"` is still visible (delete disabled while placement armed).
22. Quit the application using `File` → `Quit`.

## Pass criteria

- Delete and Backspace remove selected objects when placement is idle.
- Cascade removes dependent flows and connectors.
- Delete ignored while placement tool is armed.
- No project API used.