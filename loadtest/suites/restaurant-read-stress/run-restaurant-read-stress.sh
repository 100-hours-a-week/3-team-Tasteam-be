#!/bin/bash
#
# 음식점 조회 집중 부하 테스트 실행 스크립트
#
# 사용법:
#   TEST_TYPE=detail-only ./run-restaurant-read-stress.sh [--no-prometheus]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"
export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"
export TEST_TYPE="${TEST_TYPE:-detail-only}"

USE_PROMETHEUS=true

while [[ $# -gt 0 ]]; do
    case $1 in
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

echo ""
echo "🔥 음식점 조회 집중 부하 테스트 시작..."
echo "   스크립트: ${SCRIPT_DIR}/restaurant_read_stress_test.js"
echo "   Target: ${BASE_URL}"
echo "   TEST_TYPE: ${TEST_TYPE}"
echo ""

if [[ "$USE_PROMETHEUS" == "true" ]]; then
    K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
    K6_OUTPUT_ARG=""
fi

TEST_ID="restaurant-read-stress-$(date +%Y%m%d-%H%M%S)"

cd "$SCRIPT_DIR"
k6 run $K6_OUTPUT_ARG \
  --tag testid=$TEST_ID \
  -e TEST_ID=$TEST_ID \
  -e TEST_TYPE=$TEST_TYPE \
  restaurant_read_stress_test.js

echo ""
echo "✅ 음식점 조회 집중 부하 테스트 완료!"
