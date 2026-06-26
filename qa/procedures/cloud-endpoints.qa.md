# QA: Cloud endpoints

**Task:** `cloud-endpoints`  
**Suite:** cloud-endpoints

Verify the user can place sources and sinks and connect them to a stock with flows.

## Preconditions

- Display available (headed UI).
- `place-stock` and `connect-flow` behavior intact.

## Procedure

1. Launch the application using the QA launch command.
2. Click palette `Source`.
3. Click in region `:canvas` at offset `(-150, 0)` from `:center`.
4. Assert element `:source "Source1"` is visible.
5. Click palette `Sink`.
6. Click in region `:canvas` at offset `(+150, 0)` from `:center`.
7. Assert element `:sink "Sink1"` is visible.
8. Click palette `Stock`.
9. Click in region `:canvas` at `:center`.
10. Assert element `:stock "Stock1"` is visible.
11. Click palette `Flow`.
12. Click element `:source "Source1"`.
13. Click element `:stock "Stock1"`.
14. Assert element `:flow "Flow1"` is visible and shows `0`.
15. Assert flow `Flow1` is directed from `Source1` toward `Stock1`.
16. Click palette `Flow`.
17. Click element `:stock "Stock1"`.
18. Click element `:sink "Sink1"`.
19. Assert element `:flow "Flow2"` is visible and shows `0`.
20. Assert flow `Flow2` is directed from `Stock1` toward `Sink1`.
21. Quit the application using `File` → `Quit`.

## Pass criteria

- Sources and sinks placed via palette + canvas click (disarm between tools).
- Source→Stock and Stock→Sink flows use two semantic clicks each.
- Both flows show rate `0` and visible direction.
- No project API used.