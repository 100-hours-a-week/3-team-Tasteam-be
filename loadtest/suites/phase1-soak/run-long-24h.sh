#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"
export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

CACHE_MODE="off"
USE_PROMETHEUS=true

while [[ $# -gt 0 ]]; do
  case $1 in
    --cache-mode)
      CACHE_MODE="$2"
      shift 2
      ;;
    --no-prometheus)
      USE_PROMETHEUS=false
      shift
      ;;
    *)
      echo "알 수 없는 옵션: $1"
      exit 1
      ;;
  esac
done

TEST_ID="soak-24h-${CACHE_MODE}-$(date +%Y%m%d-%H%M%S)"

if [[ "$USE_PROMETHEUS" == "true" ]]; then
  K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
  K6_OUTPUT_ARG=""
fi

echo ""
echo "🚀 24h Soak 테스트 시작"
echo "   cache_mode=${CACHE_MODE}"
echo "   target=${BASE_URL}"
echo "   testid=${TEST_ID}"
echo ""

cd "$SCRIPT_DIR"
k6 run $K6_OUTPUT_ARG \
  --tag testid="$TEST_ID" \
  -e TEST_ID="$TEST_ID" \
  -e SOAK_MODE=24h \
  -e CACHE_MODE="$CACHE_MODE" \
  long_soak_test.js

echo ""
echo "✅ 24h Soak 테스트 완료"
echo ""
