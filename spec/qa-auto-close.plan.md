# Implementation Plan: QA Auto-Close Flag

**Task:** `qa-auto-close`  
**Status:** Approved  
**Depends on:** `cljfx-shell` (commits `d0316a8`, `12c859d`)

## User story

As QA, I have a documented and tested `--qa <seconds>` flag so headed runs always exit even when a suite fails to quit.

## Locked decisions

| Decision | Choice |
|---|---|
| Flag forms | `--qa <seconds>` on `stella.main` and `qa.cljfx_shell` |
| Exit behavior | `Platform.exit` / `System.exit` after timeout; no window hide step |
| Batch default | `scripts/run_qa_suites.sh` passes `--qa 90` per suite |
| Verification timeout | `qa-auto-close` suite uses `--qa 5` |
| Documentation | `qa/procedures/qa-auto-close.qa.md` is canonical for flag usage |

## QA-owned work

| Item | Action |
|---|---|
| `src/qa/cljfx_shell.clj` | Add `qa-auto-close` executable suite |
| `scripts/run_qa_suites.sh` | Add `qa-auto-close` suite; keep `--qa 90` on all suites |

## Testing

| Layer | Artifact |
|---|---|
| QA | `qa/procedures/qa-auto-close.qa.md` |