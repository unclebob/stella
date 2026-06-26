# QA: About dialog

**Task:** `cljfx-shell`  
**Suite:** shell-about

Verify the user can open and dismiss the About dialog from the menu.

## Preconditions

- Display available (headed UI).

## Procedure

1. Launch the application using the QA launch command.
2. Choose `Help` → `About Stella` using menu clicks.
3. Wait until dialog text includes `Stella` (timeout 2 seconds).
4. Assert the about dialog is the frontmost dialog.
5. Dismiss the dialog by clicking `OK` (semantic click on `OK`; if no button, press `Escape`).
6. Assert dialog text no longer includes the about message.
7. Assert the main window title is still `Stella`.
8. Quit the application using `File` → `Quit`.

## Pass criteria

- About dialog opens from the menu and dismisses without quitting the app.