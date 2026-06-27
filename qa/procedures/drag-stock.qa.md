# QA: Drag stock

**Task:** `drag-stock`  
**Suite:** drag-stock

Verify the user can drag a placed stock to a new canvas position using a primary-button drag gesture.

## Preconditions

- Display available (headed UI).
- `place-stock` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click the palette item `Stock`.
3. Click in region `:canvas` at `:center`.
4. Wait until element `:stock "Stock1"` is visible (timeout 2 seconds).
5. Record initial bounds of `:stock "Stock1"` (optional sanity check).
6. Drag element `:stock "Stock1"` to region `:canvas` at offset `(+120, +60)` from `:center`.
7. Wait until stock bounds change (timeout 2 seconds).
8. Assert element `:stock "Stock1"` is visible at the new location (not at the original center placement).
9. Assert the diagram still shows exactly one stock named `Stock1`.
10. Place a second stock: click palette `Stock`, click in region `:canvas` at offset `(+200, +40)` from `:center`.
11. Wait until element `:stock "Stock2"` is visible (timeout 2 seconds).
12. Click palette `Flow`, click `:stock "Stock1"`, click `:stock "Stock2"`.
13. Wait until element `:flow "Flow1"` is visible (timeout 2 seconds).
14. Drag element `:stock "Stock1"` to region `:canvas` at offset `(+40, -20)` from `:center`.
15. Assert element `:flow "Flow1"` is still visible and directed from `Stock1` toward `Stock2`.
16. Assert element `:stock "Stock2"` is still visible (second stock did not move).
17. Click palette `Stock` (arm placement).
18. Attempt to drag element `:stock "Stock2"` to region `:canvas` at `:center`.
19. Assert element `:stock "Stock2"` did not move to `:center` (drag disabled while placement armed).
20. Quit the application using `File` → `Quit`.

## Pass criteria

- Primary-button drag repositions a stock when no placement tool is armed.
- Stock count unchanged; attached flow follows visually.
- Drag does not run while a placement tool is armed.
- No project API used.