#!/usr/bin/env bash
set -euo pipefail

echo "=== 1/2 Running Cloudflare Backend WebSocket Protocol Tests ==="
(cd backend && node --test tests/*.test.mjs)

echo "=== 2/2 Running Android Unit & Robolectric Tests ==="
gradle :app:testDebugUnitTest

echo "=== All automated test suites completed successfully ==="
