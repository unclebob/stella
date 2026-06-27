# QA: Place stock

**Task:** `place-stock`  
**Suite:** place-stock

Verify the user can arm the stock tool, place stocks on the canvas one at a time, and see each stock's name and minimum value.

## Preconditions

- Display available (headed UI).
- `cljfx-shell` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Assert the diagram canvas region is visible (semantic region `:canvas`).
3. Click the palette item `Stock` (semantic click on `Stock`).
4. Click in region `:canvas` at `:center`.
5. Wait until element `:stock "Stock1"` is visible (timeout 2 seconds).
6. Assert element `:stock "Stock1"` shows `Stock1`.
7. Assert element `:stock "Stock1"` shows `0`.
8. Click the palette item `Stock` again.
9. Click in region `:canvas` at offset `(+100, +50)` from `:center`.
10. Wait until element `:stock "Stock2"` is visible (timeout 2 seconds).
11. Assert element `:stock "Stock2"` shows `Stock2`.
12. Assert element `:stock "Stock2"` shows `0`.
13. Quit the application using `File` → `Quit`.

## Pass criteria

- Each stock requires a separate palette arm + canvas click (disarm between placements).
- Both stocks show name and minimum `0` on the canvas (initial value is not shown on the icon).
- No project API used; palette and canvas interactions only.