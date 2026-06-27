# QA: Edit flow

**Task:** `edit-flow`  
**Suite:** edit-flow

Verify the user can right-click a placed flow (same gesture as **Edit Stock**), edit name and rate in the modal **Edit Flow** dialog, and see updated labels on the flow pipe.

## Preconditions

- Display available (headed UI).
- `place-stock` and `connect-flow` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stock `Stock1`: click palette `Stock`, click in region `:canvas` at `:center`.
3. Place stock `Stock2`: click palette `Stock`, click in region `:canvas` at offset `(+150, +50)` from `:center`.
4. Click the palette item `Flow`.
5. Click element `:stock "Stock1"`.
6. Click element `:stock "Stock2"`.
7. Wait until element `:flow "Flow1"` is visible (timeout 2 seconds).
8. Assert element `:flow "Flow1"` shows `Flow1`.
9. Assert element `:flow "Flow1"` shows `0`.
10. Right-click element `:flow "Flow1"`.
11. Wait until dialog titled `Edit Flow` is visible (timeout 2 seconds).
12. Type `Drain` into dialog field `Name`.
13. Type `5` into dialog field `Rate`.
14. Click `OK` on the `Edit Flow` dialog.
15. Wait until element `:flow "Drain"` is visible (timeout 2 seconds).
16. Assert element `:flow "Drain"` shows `Drain`.
17. Assert element `:flow "Drain"` shows `5`.
18. Create a second flow: click palette `Flow`, click `:stock "Stock2"`, click `:stock "Stock1"`.
19. Wait until element `:flow "Flow2"` is visible (timeout 2 seconds).
20. Right-click element `:flow "Drain"`.
21. Wait until dialog titled `Edit Flow` is visible (timeout 2 seconds).
22. Type `Flow2` into dialog field `Name`.
23. Click `OK` on the `Edit Flow` dialog.
24. Assert element `:flow "Drain"` is still visible (duplicate rename rejected).
25. Assert element `:flow "Flow2"` is visible.
26. Right-click element `:flow "Drain"`.
27. Wait until dialog titled `Edit Flow` is visible (timeout 2 seconds).
28. Clear dialog field `Rate` (empty value).
29. Click `OK` on the `Edit Flow` dialog.
30. Assert element `:flow "Drain"` shows `5` (empty rate rejected).
31. Quit the application using `File` → `Quit`.

## Pass criteria

- Edit Flow dialog opens from a right-click on a semantic flow target without using any project API.
- OK applies valid name and rate edits; pipe labels update accordingly.
- Duplicate rename and empty rate are rejected; canvas unchanged after rejection.
- No project API used.