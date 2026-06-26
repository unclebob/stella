#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

suites=(shell-launch shell-menus shell-about shell-resize shell-quit
        place-stock connect-flow cloud-endpoints connectors)

for suite in "${suites[@]}"; do
  echo "Running QA suite: ${suite}"
  clojure -M:qa "${suite}"
  sleep 2
done

echo "All QA suites passed."