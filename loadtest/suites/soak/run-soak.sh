#!/bin/bash
#
# 장기 소크 테스트 실행 스크립트 (P0)
#
# 사용법:
#   ./run-soak.sh [--no-prometheus]
#
# 환경변수:
#   SOAK_MODE   - 24h(기본값) | 48h
#   CACHE_MODE  - on(기본값) | off
#   BASE_URL    - https://stg.tasteam.kr (기본값)
#
# 옵션:
#   --no-prometheus  Prometheus 출력을 비활성화합니다.
#
# 주의: --reset-db 옵션은 소크 테스트에서 지원하지 않습니다.
#       장기 테스트 전 DB 초기화가 필요하면 수동으로 수행하세요.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"
export SOAK_MODE="${SOAK_MODE:-24h}"
export CACHE_MODE="${CACHE_MODE:-on}"

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
echo "장기 소크 테스트 시작... [SOAK_MODE=${SOAK_MODE}, CACHE_MODE=${CACHE_MODE}]"
echo "   스크립트: ${SCRIPT_DIR}/long_soak_test.js"

if [[ "$SOAK_MODE" == "48h" ]]; then
    echo "   모드: 48h - 0~12h(100 VU) → 12~36h(120 VU 피크) → 36~47h(100 VU) → 47~48h(종료)"
else
    echo "   모드: 24h - ramp 1h(40 VU) → steady 22h(100 VU) → close 1h"
fi

echo "   판정 기준: 30분 롤링 에러율 ≤ 0.3%, read p95 < 1s"
echo ""

if [[ "$USE_PROMETHEUS" == "true" ]]; then
    echo "Prometheus remote write 활성화: $K6_PROMETHEUS_RW_SERVER_URL"
    K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
    echo "Prometheus 출력 비활성화 (--no-prometheus 옵션)"
    K6_OUTPUT_ARG=""
fi

echo ""

TEST_ID="soak-${SOAK_MODE}-cache${CACHE_MODE}-$(date +%Y%m%d-%H%M%S)"
echo "Test ID: $TEST_ID"
echo ""

cd "$SCRIPT_DIR"
k6 run $K6_OUTPUT_ARG \
  --tag testid=$TEST_ID \
  --tag soak_mode=${SOAK_MODE} \
  --tag cache_mode=${CACHE_MODE} \
  -e TEST_ID=$TEST_ID \
  -e SOAK_MODE=${SOAK_MODE} \
  -e CACHE_MODE=${CACHE_MODE} \
  long_soak_test.js

echo ""
echo "장기 소크 테스트 완료!"
echo "   - Grafana 대시보드에서 30분 롤링 에러율 및 p95 추이를 확인하세요."
echo "   - 판정 기준: 에러율 ≤ 0.3%, read p95 < 1s"
