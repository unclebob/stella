# QA: Canvas resizes with window

**Task:** `cljfx-shell`  
**Suite:** shell-resize

Verify the diagram canvas remains usable after the user resizes the window.

## Preconditions

- Display available (headed UI).

## Procedure

1. Launch the application using the QA launch command with window size `800` × `600`.
2. Assert the diagram canvas region is visible.
3. Record the canvas region bounds (semantic region `:canvas`).
4. Resize the window by dragging the bottom-right corner from canvas-relative coordinates `(750, 550)` to `(950, 700)` using an explicit drag gesture.
5. Wait 500 ms for layout to settle.
6. Assert the diagram canvas region is still visible.
7. Assert the canvas region bounds differ from step 3 (width and height both increased).
8. Click in region `:canvas` at `:center` (semantic relative click).
9. Assert the main window is still showing.
10. Quit the application using `File` → `Quit`.

## Pass criteria

- Canvas survives resize and remains clickable at its center.
- Coordinate-based drag is used only for the resize gesture (step 4).