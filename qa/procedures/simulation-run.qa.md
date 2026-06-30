# QA: Simulation run

**Task:** `simulation-run`  
**Suite:** simulation-run

Verify the control panel speed slider sets delay between simulation ticks from 1 second down to 0, and the Run button auto-steps the simulation at that speed while toggling to Stop.

## Preconditions

- Display available (headed UI).
- `place-stock`, `connect-flow`, `edit-flow`, and `control-panel` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Assert semantic region `:control-panel` is visible.
3. Assert the simulation tick delay display shows `1`.
4. Assert visible button text `Run` is present in region `:control-panel`.
5. Place stocks and flow per `connect-flow` QA.
6. Double-click element `:flow "Flow1"`, set field `Rate` to `10`, confirm the edit dialog.
7. Set simulation tick delay to `0` using the speed slider.
8. Assert the simulation tick delay display shows `0`.
9. Click visible text `Run`.
10. Assert visible button text `Stop` is present in region `:control-panel`.
11. Wait until visible text in region `:control-panel` includes `0.1` (timeout 2 seconds).
12. Click visible text `Stop`.
13. Assert visible button text `Run` is present in region `:control-panel`.
14. Wait 1 second.
15. Assert visible text in region `:control-panel` still includes `0.1` (time did not advance while stopped).
16. Set simulation tick delay to `0.5` using the speed slider.
17. Assert the simulation tick delay display shows `0.5`.
18. Quit the application using `File` → `Quit`.

## Pass criteria

- Speed slider range is 1 second (slowest) to 0 (fastest); display shows the current delay in seconds.
- Run starts automatic stepping at the selected delay; button label becomes `Stop`.
- Stop halts automatic stepping; button label returns to `Run`.
- Each automatic tick advances simulation time by `0.1`, same as Step.
- Step button remains available when run is stopped.
- No project API used.