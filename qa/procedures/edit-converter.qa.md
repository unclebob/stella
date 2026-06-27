# QA: Edit converter

**Task:** `edit-converter`  
**Suite:** edit-converter

Verify the user can right-click a converter (same gesture as **Edit Stock** / **Edit Flow**), edit name and formula in the modal **Edit Converter** dialog, and see the formula on the connector arrow.

## Preconditions

- Display available (headed UI).
- `connectors` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stocks and flow per `connect-flow` QA (stocks at `:center` and offset `(+150, +50)`).
3. Place converter: click palette `Converter`, click in region `:canvas` at offset `(-50, +80)` from `:center`.
4. Connect converter to flow: click palette `Connector`, click `:converter "Converter1"`, click `:flow "Flow1"`.
5. Wait until element `:connector "Connector1"` is visible (timeout 2 seconds).
6. Assert element `:converter "Converter1"` shows `Converter1` (name only; not value `0`).
7. Right-click element `:converter "Converter1"`.
8. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
9. Type `Growth` into dialog field `Name`.
10. Type `Stock1 * 0.1` into dialog field `Formula`.
11. Click `OK` on the `Edit Converter` dialog.
12. Wait until element `:converter "Growth"` is visible (timeout 2 seconds).
13. Assert element `:converter "Growth"` shows `Growth`.
14. Assert element `:connector "Connector1"` shows `Stock1 * 0.1`.
15. Place a second converter: click palette `Converter`, click in region `:canvas` at offset `(+200, +80)` from `:center`.
16. Wait until element `:converter "Converter2"` is visible (timeout 2 seconds).
17. Right-click element `:converter "Growth"`.
18. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
19. Type `Converter2` into dialog field `Name`.
20. Click `OK` on the `Edit Converter` dialog.
21. Assert element `:converter "Growth"` is still visible (duplicate rename rejected).
22. Right-click element `:converter "Converter2"`.
23. Wait until dialog titled `Edit Converter` is visible (timeout 2 seconds).
24. Type `Stock1 * 0.2` into dialog field `Formula`.
25. Click `OK` on the `Edit Converter` dialog.
26. Assert element `:converter "Converter2"` shows `Converter2` only (no formula on converter; formula edit rejected without converter→flow link).
27. Quit the application using `File` → `Quit`.

## Pass criteria

- Edit Converter dialog opens from right-click on semantic converter target.
- Name updates converter label; formula updates connector arrow label.
- Formula is stored on the connector, not shown on the converter circle.
- Duplicate rename and formula-without-flow-link are rejected.
- No project API used.