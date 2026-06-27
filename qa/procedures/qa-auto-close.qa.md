# QA: Auto-close flag

**Task:** `qa-auto-close`  
**Suite:** `qa-auto-close`

Define and verify the `--qa <seconds>` launch flag that forces Stella to exit after a timeout. All headed QA runs must use this flag so orphaned windows eventually close.

## Preconditions

- Display available (headed UI).

## Flag usage (mandatory for headed QA)

| Launch context | Command |
|---|---|
| QA suite runner | `clojure -M:qa --qa <seconds> <suite>` |
| Full QA batch | `bb qa` (script passes `--qa 90` to every suite) |
| Manual Stella | `clojure -M:run -- --qa <seconds>` |

Rules:

- `<seconds>` is a positive integer.
- `--qa` schedules a JVM exit after that many seconds; it does not hide windows first.
- Every executable QA suite launched by `scripts/run_qa_suites.sh` must include `--qa 90` unless this procedure specifies a different value for a dedicated test.
- Other `qa/procedures/*.qa.md` files may say “QA launch command”; that means `clojure -M:qa --qa 90 <suite>` for normal suites.

## Procedure (verification suite)

1. Launch `clojure -M:qa --qa 5 qa-auto-close` (short timeout for this test only).
2. Wait until the Stella main window is visible (timeout 5 seconds).
3. Assert the window title is `Stella`.
4. Do **not** choose `File` → `Quit`.
5. Wait until the application process exits on its own (timeout 10 seconds).
6. Assert no Stella window remains visible.

## Pass criteria

- Process exits within the `--qa` timeout without manual quit.
- Executable `qa-auto-close` suite is registered in `src/qa/cljfx_shell.clj`.
- `scripts/run_qa_suites.sh` includes the `qa-auto-close` suite and passes `--qa 90` to all other suites.

## QA implementation notes

- Implement `run-qa-auto-close!` in `src/qa/cljfx_shell.clj` per the procedure above.
- Add `"qa-auto-close"` to the suites map and to `scripts/run_qa_suites.sh` (run first so a broken flag is caught early).