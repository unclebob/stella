#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

suites=(qa-auto-close shell-launch shell-menus shell-about shell-resize shell-quit
        place-stock connect-flow cloud-endpoints connectors edit-stock edit-flow edit-converter
        drag-stock drag-converter select-objects delete-selection)

for suite in "${suites[@]}"; do
  echo "Running QA suite: ${suite}"
  if [ "${suite}" = "qa-auto-close" ]; then
    clojure -M:qa --qa 5 "${suite}"
  else
    clojure -M:qa --qa 90 "${suite}"
  fi
  sleep 2
done

echo "All QA suites passed."