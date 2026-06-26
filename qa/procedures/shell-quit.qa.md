# QA: Quit from menu

**Task:** `cljfx-shell`  
**Suite:** shell-quit

Verify the user can exit the application from the File menu.

## Preconditions

- Display available (headed UI).

## Procedure

1. Launch the application using the QA launch command.
2. Assert the main window is visible.
3. Choose `File` → `Quit` using menu clicks.
4. Wait until the application process exits (timeout 5 seconds).
5. Assert the main window is no longer visible.

## Pass criteria

- Application exits cleanly from the menu without manual process kill.