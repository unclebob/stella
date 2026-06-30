# QA: Run simulation

**Task:** `run-simulation`  
**Suite:** run-simulation

Verify the user can advance a simple source-stock-sink simulation with dt `0.1` using the Step button in the control panel.

## Preconditions

- Display available (headed UI).
- `place-stock`, `place-cloud`, `connect-flow`, `connect-cloud-flow`, and `edit-stock` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click palette `Source`.
3. Click in region `:canvas` at offset `(-150, 0)` from `:center`.
4. Click palette `Sink`.
5. Click in region `:canvas` at offset `(+150, 0)` from `:center`.
6. Click palette `Stock`.
7. Click in region `:canvas` at `:center`.
8. Connect source flow: click palette `Flow`, click element `:source "Source1"`, click element `:stock "Stock1"`.
9. Connect sink flow: click palette `Flow`, click element `:stock "Stock1"`, click element `:sink "Sink1"`.
10. Double-click element `:flow "Flow1"`, set field `Rate` to `10`, confirm the edit dialog.
11. Double-click element `:flow "Flow2"`, set field `Rate` to `5`, confirm the edit dialog.
12. Assert visible text in region `:control-panel` includes `0`.
13. Click visible text `Step`.
14. Assert visible text in region `:control-panel` includes `0.1`.
15. Click visible text `Step`.
16. Assert visible text in region `:control-panel` includes `0.2`.
17. Quit the application using `File` → `Quit`.

## Pass criteria

- Each Step click advances the user-visible simulation time by `0.1`.
- Two Step clicks show simulation time `0.2` on a source-stock-sink diagram with inflow rate `10` and outflow rate `5`.
- Source, sink, stock, flow, and rate edits use only palette, canvas, and dialog interactions.
- No project API used.