# QA: Edit stock

**Task:** `edit-stock`  
**Suite:** edit-stock

Verify the user can right-click a placed stock, edit fields in the Edit Stock dialog, edit the stock current value, and see name, minimum, and maximum on the stock icon.

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
17. Assert dialog field `Current value` is visible.
18. Type `25` into dialog field `Initial value`.
19. Type `30` into dialog field `Current value`.
20. Type `5` into dialog field `Minimum`.
21. Type `100` into dialog field `Maximum`.
22. Click `OK` on the `Edit Stock` dialog.
23. Assert element `:stock "Cats"` shows `Cats`.
24. Assert element `:stock "Cats"` shows `5`.
25. Assert element `:stock "Cats"` shows `100`.
26. Assert element `:stock "Cats"` does not show `25` on the icon (initial value is dialog-only).
27. Right-click element `:stock "Cats"`.
28. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
29. Type `40` into dialog field `Minimum`.
30. Type `35` into dialog field `Current value`.
31. Click `OK` on the `Edit Stock` dialog.
32. Assert the stock current value for `:stock "Cats"` is `40`.
33. Right-click element `:stock "Cats"`.
34. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
35. Type `80` into dialog field `Maximum`.
36. Type `90` into dialog field `Current value`.
37. Click `OK` on the `Edit Stock` dialog.
38. Assert the stock current value for `:stock "Cats"` is `80`.
39. Place a second stock: click palette `Stock`, click in region `:canvas` at offset `(+120, +40)` from `:center`.
40. Wait until element `:stock "Stock2"` is visible (timeout 2 seconds).
41. Right-click element `:stock "Cats"`.
42. Wait until dialog titled `Edit Stock` is visible (timeout 2 seconds).
43. Type `Stock2` into dialog field `Name`.
44. Click `OK` on the `Edit Stock` dialog.
45. Assert element `:stock "Cats"` is still visible (duplicate rename rejected).
46. Assert element `:stock "Stock2"` is visible.
47. Quit the application using `File` → `Quit`.

## Pass criteria

- Edit Stock dialog opens from a right-click on a semantic stock target without using any project API.
- OK applies valid edits; icon shows name centered and min/max in bottom corners.
- The dialog exposes editable fields labeled `Initial value` and `Current value`.
- Current value edits below the stock minimum clamp to the minimum.
- Current value edits above the stock maximum clamp to the maximum.
- Min and max labels use visibly smaller text than the centered name.
- Duplicate rename is rejected and the original stock name remains on the canvas.
