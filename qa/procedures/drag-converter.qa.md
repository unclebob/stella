# QA: Drag converter

**Task:** `drag-converter`  
**Suite:** drag-converter

Verify the user can drag a placed converter to a new canvas position using a primary-button drag gesture.

## Preconditions

- Display available (headed UI).
- `connectors` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click the palette item `Converter`.
3. Click in region `:canvas` at `:center`.
4. Wait until element `:converter "Converter1"` is visible (timeout 2 seconds).
5. Drag element `:converter "Converter1"` to region `:canvas` at offset `(+100, +80)` from `:center`.
6. Wait until converter bounds change (timeout 2 seconds).
7. Assert element `:converter "Converter1"` is visible at the new location.
8. Assert the diagram still shows exactly one converter named `Converter1`.
9. Place stocks and flow per `connect-flow` QA, then connect converter to flow: palette `Connector`, click `:converter "Converter1"`, click `:flow "Flow1"`.
10. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
11. Drag element `:converter "Converter1"` to region `:canvas` at offset `(-40, +120)` from `:center`.
12. Assert element `:connector "Connector1"` is still visible and directed from `Converter1` toward `Flow1`.
13. Place a second converter: click palette `Converter`, click in region `:canvas` at offset `(+220, +60)` from `:center`.
14. Wait until element `:converter "Converter2"` is visible (timeout 2 seconds).
15. Assert element `:converter "Converter2"` is visible (second converter did not move with first).
16. Click palette `Converter` (arm placement).
17. Attempt to drag element `:converter "Converter2"` to region `:canvas` at `:center`.
18. Assert element `:converter "Converter2"` did not move to `:center` (drag disabled while placement armed).
19. Quit the application using `File` → `Quit`.

## Pass criteria

- Primary-button drag repositions a converter when no placement tool is armed.
- Converter count unchanged; attached connector follows visually.
- Drag does not run while a placement tool is armed.
- No project API used.