# QA: Shell launch

**Task:** `cljfx-shell`  
**Suite:** shell-launch

Verify the user can launch Stella and see the main window with a standard menu bar.

## Preconditions

- Display available (headed UI).
- Run via `bb qa` or `clojure -M:qa --qa 90 <suite>` (the `--qa` flag auto-closes Stella after 90 seconds).

## Procedure

1. Launch the application using the QA launch command.
2. Wait until the main window is visible (timeout 5 seconds).
3. Assert the window title is `Stella`.
4. Assert visible text includes `File`.
5. Assert visible text includes `Edit`.
6. Assert visible text includes `View`.
7. Assert visible text includes `Help`.
8. Assert the diagram canvas region is visible (semantic region `:canvas`).
9. Quit the application using `File` → `Quit`.
10. Assert the application process has exited.

## Pass criteria

- Steps 3–8 succeed without using any project API.
- Step 10 confirms clean shutdown after the test.