# QA: Edit stock

**Task:** `edit-stock`  
**Suite:** edit-stock

Verify the user can right-click a placed stock, edit fields in the Edit Stock dialog, and see name, minimum, and maximum on the stock icon.

## Preconditions

- Display available (headed UI).
- `place-stock` behavior intact.

## Stock icon layout (assertions)

- **Name** — prominent, centered on the icon.
- **Minimum** — bottom-left, small font.
- **Maximum** — bottom-right, small font; empty when unbounded.
- Initial value is not shown on the icon.

## Procedure

1. Launch the application using the QA launch command.
2. Assert the diagram canvas region is visible (semantic region `:canvas`).
3. Click the palette item `Stock`.
4. Click in region `:canvas` at `:center`.
5. Wait until element `:stock "Stock1"` is visible (timeout 2 seconds).
6. Assert element `:stock "Stock1"` shows `Stock1` (centered name).
7. Assert element `:stock "Stock1"` shows `0` (default minimum, bottom-left).
8. Right-click element `:stock "Stock1"`.
9. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
10. Type `Cats` into dialog field `Name`.
11. Click `OK` on the `Edit Stock` dialog.
12. Wait until element `:stock "Cats"` is visible (timeout 2 seconds).
13. Assert element `:stock "Cats"` shows `Cats`.
14. Assert element `:stock "Cats"` shows `0`.
15. Right-click element `:stock "Cats"`.
16. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
17. Type `25` into dialog field `Initial value`.
18. Type `5` into dialog field `Minimum`.
19. Type `100` into dialog field `Maximum`.
20. Click `OK` on the `Edit Stock` dialog.
21. Assert element `:stock "Cats"` shows `Cats`.
22. Assert element `:stock "Cats"` shows `5`.
23. Assert element `:stock "Cats"` shows `100`.
24. Assert element `:stock "Cats"` does not show `25` on the icon (initial value is dialog-only).
25. Place a second stock: click palette `Stock`, click in region `:canvas` at offset `(+120, +40)` from `:center`.
26. Wait until element `:stock "Stock2"` is visible (timeout 2 seconds).
27. Right-click element `:stock "Cats"`.
28. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
29. Type `Stock2` into dialog field `Name`.
30. Click `OK` on the `Edit Stock` dialog.
31. Assert element `:stock "Cats"` is still visible (duplicate rename rejected).
32. Assert element `:stock "Stock2"` is visible.
33. Quit the application using `File` → `Quit`.

## Pass criteria

- Edit Stock dialog opens from a right-click on a semantic stock target without using any project API.
- OK applies valid edits; icon shows name centered and min/max in bottom corners.
- Min and max labels use visibly smaller text than the centered name.
- Duplicate rename is rejected and the original stock name remains on the canvas.