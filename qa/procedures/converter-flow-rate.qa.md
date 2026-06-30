# QA: Converter flow rate

**Task:** `converter-flow-rate`  
**Suite:** converter-flow-rate

Verify a converter formula referencing stock values computes a rate, displays that value centered on the converter icon, drives the connected flow rate, and transfers stock during simulation.

## Preconditions

- Display available (headed UI).
- `connectors`, `edit-converter`, `edit-stock`, and `run-simulation` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stocks and flow per `connect-flow` QA (stocks at `:center` and offset `(+150, +50)`).
3. Place converter: click palette `Converter`, click in region `:canvas` at offset `(-50, +80)` from `:center`.
4. Connect converter to flow: click palette `Connector`, click `:converter "Converter1"`, click `:flow "Flow1"`.
5. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
6. Assert element `:converter "Converter1"` shows `0` centered on the icon (not the name label below).
7. Right-click element `:converter "Converter1"`.
8. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
9. Type `Stock1 * 0.1` into dialog field `Formula`.
10. Click `OK` on the `Edit Converter` dialog.
11. Double-click element `:stock "Stock1"`, set `Initial value` to `100`, confirm the edit dialog.
12. Assert element `:converter "Converter1"` shows `10` centered on the icon.
13. Assert element `:flow "Flow1"` shows rate `10`.
14. Assert element `:connector "Connector1"` shows `Stock1 * 0.1`.
15. Assert element `:stock "Stock2"` shows value `0`.
16. Click visible text `Step` once.
17. Assert element `:stock "Stock1"` shows value `99`.
18. Assert element `:stock "Stock2"` shows value `1`.
19. Assert element `:converter "Converter1"` shows `9.9` centered on the icon.
20. Assert element `:flow "Flow1"` shows rate `9.9`.
21. Right-click element `:converter "Converter1"`.
22. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
23. Type `Missing * 2` into dialog field `Formula`.
24. Click `OK` on the `Edit Converter` dialog.
25. Assert dialog titled `Edit Converter` is still visible (invalid formula rejected).
26. Click `Cancel` on the `Edit Converter` dialog.
27. Quit the application using `File` → `Quit`.

## Pass criteria

- Converter center shows computed numeric value; name remains on the label below the circle.
- Connected flow rate label matches the converter computed value.
- Connector arrow still shows the formula text.
- Simulation transfers stock using the computed rate, and the displayed rate updates as stock values change.
- Unknown stock names and malformed formulas are rejected in the Edit Converter dialog.
- No project API used.