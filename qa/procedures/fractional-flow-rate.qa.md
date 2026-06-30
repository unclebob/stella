# QA: Fractional flow rate

**Task:** `fractional-flow-rate`  
**Suite:** fractional-flow-rate

Verify a source flow with rate less than 1 accumulates visible stock value over multiple Step clicks.

## Preconditions

- Display available (headed UI).
- `place-stock`, `place-cloud`, `connect-flow`, `edit-flow`, and `run-simulation` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click palette `Source`.
3. Click in region `:canvas` at offset `(-150, 0)` from `:center`.
4. Click palette `Stock`.
5. Click in region `:canvas` at `:center`.
6. Connect source flow: click palette `Flow`, click element `:source "Source1"`, click element `:stock "Stock1"`.
7. Right-click element `:flow "Flow1"`, set field `Rate` to `0.1`, confirm the edit dialog.
8. Assert element `:stock "Stock1"` shows value `0`.
9. Click visible text `Step` ten times.
10. Assert element `:stock "Stock1"` shows value `0.1`.
11. Click visible text `Step` ten more times.
12. Assert element `:stock "Stock1"` shows value `0.2`.
13. Quit the application using `File` → `Quit`.

## Pass criteria

- Rate `0.1` is accepted in the Edit Flow dialog.
- Stock value remains `0` until enough steps accumulate a visible fractional transfer.
- After 10 Step clicks stock value is `0.1`; after 20 total clicks it is `0.2`.
- No project API used.