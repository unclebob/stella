# QA: Converter flow rate

**Task:** `converter-flow-rate`  
**Suite:** converter-flow-rate

Verify stock-to-converter connectors supply named values for converter formulas, the computed rate appears centered on the converter icon, drives the connected flow rate, and transfers stock during simulation.

## Preconditions

- Display available (headed UI).
- `connectors`, `edit-converter`, `edit-stock`, and `run-simulation` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stocks and flow per `connect-flow` QA (stocks at `:center` and offset `(+150, +50)`).
3. Place converter: click palette `Converter`, click in region `:canvas` at offset `(-50, +80)` from `:center`.
4. Connect stock to converter: click palette `Connector`, click `:stock "Stock1"`, click `:converter "Converter1"`.
5. Wait until element `:connector "Connector2"` is visible (timeout 2 seconds).
6. Connect converter to flow: click palette `Connector`, click `:converter "Converter1"`, click `:flow "Flow1"`.
7. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
8. Assert element `:converter "Converter1"` shows `0` centered on the icon (not the name label below).
9. Right-click element `:converter "Converter1"`.
10. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
11. Type `Stock1 * 0.1` into dialog field `Formula`.
12. Click `OK` on the `Edit Converter` dialog.
13. Double-click element `:stock "Stock1"`, set `Initial value` to `100`, confirm the edit dialog.
14. Assert element `:converter "Converter1"` shows `10` centered on the icon.
15. Assert element `:flow "Flow1"` shows rate `10`.
16. Assert element `:connector "Connector1"` shows `Stock1 * 0.1`.
17. Assert element `:stock "Stock2"` shows value `0`.
18. Click visible text `Step` once.
19. Assert element `:stock "Stock1"` shows value `99`.
20. Assert element `:stock "Stock2"` shows value `1`.
21. Assert element `:converter "Converter1"` shows `9.9` centered on the icon.
22. Assert element `:flow "Flow1"` shows rate `9.9`.
23. Right-click element `:converter "Converter1"`.
24. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
25. Type `(Stock1 + 10) / 2` into dialog field `Formula`.
26. Click `OK` on the `Edit Converter` dialog.
27. Assert element `:converter "Converter1"` shows `54.5` centered on the icon (Stock1 is 99 after step 18).
28. Right-click element `:converter "Converter1"`.
29. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
30. Type `Missing * 2` into dialog field `Formula`.
31. Click `OK` on the `Edit Converter` dialog.
32. Assert dialog titled `Edit Converter` is still visible (name not available).
33. Click `Cancel` on the `Edit Converter` dialog.
34. Quit the application using `File` → `Quit`.

## Pass criteria

- Stock-to-converter connectors bind stock names to current stock values for formula evaluation.
- Converter center shows computed numeric value; name remains on the label below the circle.
- Connected flow rate label matches the converter computed value.
- Converter-to-flow connector arrow still shows the formula text.
- Formulas support `+`, `-`, `*`, `/`, and parentheses.
- Simulation transfers stock using the computed rate, and the displayed rate updates as stock values change.
- Unbound names, unknown names, and malformed operators are rejected in the Edit Converter dialog.
- No project API used.