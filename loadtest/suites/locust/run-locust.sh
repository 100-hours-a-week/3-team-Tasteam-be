#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"
USERS="${USERS:-1000}"
SPAWN_RATE="${SPAWN_RATE:-50}"
RUN_TIME="${RUN_TIME:-35m}"

cd "$SCRIPT_DIR"

echo ""
echo "🚀 Locust Journey 테스트 시작"
echo "   target=${BASE_URL}"
echo "   users=${USERS}"
echo "   spawn_rate=${SPAWN_RATE}"
echo "   run_time=${RUN_TIME}"
echo ""

locust -f locustfile.py --headless -u "$USERS" -r "$SPAWN_RATE" --run-time "$RUN_TIME"
