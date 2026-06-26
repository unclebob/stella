# QA: Shell menu stubs

**Task:** `cljfx-shell`  
**Suite:** shell-menus

Verify stub menu items appear disabled and essential items appear enabled.

## Preconditions

- Display available (headed UI).

## Procedure

1. Launch the application using the QA launch command.
2. Open the `File` menu using a menu click on `File`.
3. Assert `New` is visible.
4. Assert `New` is disabled (grayed or click produces no action).
5. Assert `Quit` is visible.
6. Assert `Quit` is enabled.
7. Close the `File` menu without quitting (press `Escape` or click the canvas center).
8. Open the `Help` menu using a menu click on `Help`.
9. Assert `About Stella` is visible.
10. Assert `About Stella` is enabled.
11. Close the `Help` menu (press `Escape`).
12. Quit the application using `File` → `Quit`.

## Pass criteria

- Disabled items are visibly inactive.
- `Quit` and `About Stella` are visibly active.