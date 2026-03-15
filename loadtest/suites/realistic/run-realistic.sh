#!/bin/bash
#
# 리얼리스틱 부하 테스트 실행 스크립트
#
# 사용법:
#   ./run-realistic.sh [--no-prometheus]
#
# 옵션:
#   --no-prometheus  Prometheus 출력을 비활성화합니다.
#
# 기본값 (환경변수로 override 가능):
#   K6_PROMETHEUS_RW_SERVER_URL  - https://prom-dev.tasteam.kr/api/v1/write
#   K6_PROMETHEUS_RW_USERNAME    - tasteam
#   K6_PROMETHEUS_RW_PASSWORD    - tasteam-k6-metrics
#   BASE_URL                     - https://stg.tasteam.kr

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ============ 기본값 설정 ============
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"
export REVERSE_GEOCODE_MODE="${REVERSE_GEOCODE_MODE:-per-vu-once}"

# ============ 옵션 파싱 ============
USE_PROMETHEUS=true
K6_ARGS=(run)

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

if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    mkdir -p "$(dirname "$K6_SUMMARY_EXPORT")"
    K6_ARGS+=(--summary-export "$K6_SUMMARY_EXPORT")
fi

# ============ k6 실행 ============
echo ""
echo "🚀 리얼리스틱 부하 테스트 시작..."
echo "   스크립트: ${SCRIPT_DIR}/realistic_test.js"
echo "   Target: ${BASE_URL}"
echo "   Reverse Geocode Mode: ${REVERSE_GEOCODE_MODE}"
if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    echo "   Summary: ${K6_SUMMARY_EXPORT}"
fi
echo ""

# Prometheus 출력 설정
if [[ "$USE_PROMETHEUS" == "true" ]]; then
    echo "📊 Prometheus remote write 활성화: $K6_PROMETHEUS_RW_SERVER_URL"
    K6_ARGS+=(-o experimental-prometheus-rw)
else
    echo "ℹ️  Prometheus 출력 비활성화 (--no-prometheus 옵션)"
fi

echo ""

# Generate specific Test ID
TEST_ID="realistic-$(date +%Y%m%d-%H%M%S)"

echo "🆔 Test ID: $TEST_ID"
echo ""

# k6 실행
cd "$SCRIPT_DIR"
K6_ARGS+=(--tag "testid=${TEST_ID}")
K6_ARGS+=(-e "TEST_ID=${TEST_ID}")
K6_ARGS+=(realistic_test.js)
k6 "${K6_ARGS[@]}"

echo ""
echo "✅ 리얼리스틱 부하 테스트 완료!"
echo "   - 결과 요약은 위 출력을 확인하세요."
echo "   - Prometheus 출력 시 Grafana 대시보드에서 상세 분석 가능합니다."
