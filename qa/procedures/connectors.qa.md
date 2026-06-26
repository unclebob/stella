# QA: Connectors

**Task:** `connectors`  
**Suite:** connectors

Verify the user can place a converter and draw connectors to a flow and from a stock.

## Preconditions

- Display available (headed UI).
- Stock and flow placement behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Place stock `Stock1`: click palette `Stock`, click in region `:canvas` at offset `(-100, 0)` from `:center`.
3. Place stock `Stock2`: click palette `Stock`, click in region `:canvas` at offset `(+100, 0)` from `:center`.
4. Connect flow: click palette `Flow`, click `:stock "Stock1"`, click `:stock "Stock2"`.
5. Assert element `:flow "Flow1"` is visible.
6. Click palette `Converter`.
7. Click in region `:canvas` at offset `(0, +100)` from `:center`.
8. Assert element `:converter "Converter1"` is visible.
9. Assert element `:converter "Converter1"` shows `0`.
10. Click palette `Connector`.
11. Click element `:converter "Converter1"`.
12. Click element `:flow "Flow1"`.
13. Assert connector `Connector1` is visible from `Converter1` toward `Flow1`.
14. Click palette `Connector`.
15. Click element `:stock "Stock1"`.
16. Click element `:converter "Converter1"`.
17. Assert connector `Connector2` is visible from `Stock1` toward `Converter1`.
18. Quit the application using `File` → `Quit`.

## Pass criteria

- Converter placed via palette + canvas click.
- Each connector requires palette arm plus two semantic element clicks.
- Connectors are visually distinct from flows.
- No project API used.