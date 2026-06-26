# QA: Connect flow

**Task:** `connect-flow`  
**Suite:** connect-flow

Verify the user can connect two stocks with a flow using two semantic stock clicks.

## Preconditions

- Display available (headed UI).
- `place-stock` behavior intact.

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
10. Assert flow `Flow1` is directed from stock `Stock1` toward stock `Stock2` (visible arrow or ordered endpoint labels).
11. Quit the application using `File` → `Quit`.

## Pass criteria

- Flow requires palette arm plus two stock clicks (source then destination).
- Flow shows name and rate `0`.
- Direction from `Stock1` to `Stock2` is user-visible.
- No project API used.