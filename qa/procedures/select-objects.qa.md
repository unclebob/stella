# QA: Select objects

**Task:** `select-objects`  
**Suite:** select-objects

Verify click, shift-click, marquee, and Escape selection behavior with grey outline feedback.

## Preconditions

- Display available (headed UI).
- `place-stock` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stock `Stock1`: palette `Stock`, click region `:canvas` at `:center`.
3. Place stock `Stock2`: palette `Stock`, click region `:canvas` at offset `(+180, +60)` from `:center`.
4. Wait until elements `:stock "Stock1"` and `:stock "Stock2"` are visible (timeout 2 seconds).
5. Click element `:stock "Stock1"`.
6. Assert element `:stock "Stock1"` shows a grey selection outline.
7. Assert element `:stock "Stock2"` does not show a grey selection outline.
8. Click element `:stock "Stock1"` again.
9. Assert element `:stock "Stock1"` does not show a grey selection outline.
10. Click element `:stock "Stock1"`.
11. Shift-click element `:stock "Stock2"`.
12. Assert both `:stock "Stock1"` and `:stock "Stock2"` show grey selection outlines.
13. Shift-click element `:stock "Stock1"`.
14. Assert element `:stock "Stock1"` does not show a grey selection outline.
15. Assert element `:stock "Stock2"` still shows a grey selection outline.
16. Marquee-select on region `:canvas` from offset `(-120, -80)` to `(+80, +80)` relative to `:center`.
17. Assert element `:stock "Stock1"` shows a grey selection outline.
18. Assert element `:stock "Stock2"` does not show a grey selection outline.
19. Press `Escape`.
20. Assert no diagram element shows a grey selection outline.
21. Click palette `Flow` (arm placement).
22. Click element `:stock "Stock1"`.
23. Assert element `:stock "Stock1"` does not show a grey selection outline (selection disabled while armed).
24. Quit the application using `File` → `Quit`.

## Pass criteria

- Plain click selects with grey outline; second click deselects.
- Shift-click toggles multi-select without clearing others.
- Marquee selects intersecting objects only.
- Escape clears all selections.
- Selection ignored while placement tool is armed.
- No project API used.