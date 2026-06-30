# QA: Stock thermometer

**Task:** `stock-thermometer`  
**Suite:** stock-thermometer

Verify each stock icon shows a horizontal light-blue thermometer between the top name and bottom bound labels, and that the fill grows when the stock value increases during simulation.

## Preconditions

- Display available (headed UI).
- `place-stock`, `place-cloud`, `connect-cloud-flow`, `edit-stock`, and `run-simulation` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stock `Stock1`: click palette `Stock`, click in region `:canvas` at `:center`.
3. Double-click element `:stock "Stock1"`, set `Minimum` to `0`, `Maximum` to `100`, `Initial value` to `0`, confirm the edit dialog.
4. Assert element `:stock "Stock1"` shows name `Stock1` at the top of the icon.
5. Assert element `:stock "Stock1"` shows minimum `0` and maximum `100` at the bottom corners.
6. Assert semantic region `:stock-thermometer-fill` for `:stock "Stock1"` is visible with light blue fill and near-zero width.
7. Re-open the edit dialog for `:stock "Stock1"`, set `Initial value` to `50`, confirm.
8. Assert `:stock-thermometer-fill` for `:stock "Stock1"` width is visibly about half the thermometer track width.
9. Click palette `Source`, click in region `:canvas` at offset `(-150, 0)` from `:center`.
10. Click palette `Flow`, click element `:source "Source1"`, click element `:stock "Stock1"`.
11. Double-click element `:flow "Flow1"`, set `Rate` to `10`, confirm.
12. Click visible text `Step`.
13. Assert `:stock-thermometer-fill` for `:stock "Stock1"` width is slightly greater than after step 8.
14. Quit the application using `File` → `Quit`.

## Pass criteria

- Stock name is at the top; min and max labels remain at the bottom.
- A horizontal thermometer track sits between the name and bound labels.
- Thermometer fill is light blue and reflects stock value (near empty at `0`, about half full at `50`, slightly wider after one Step adds inflow).
- No project API used.