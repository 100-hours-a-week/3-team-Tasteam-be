#!/bin/bash
#
# 브레이크포인트 테스트 실행 스크립트
#
# 사용법:
#   ./run-breakpoint.sh [--reset-db] [--no-prometheus]
#
# 옵션:
#   --reset-db       테스트 전 개발 DB를 초기화합니다.
#   --no-prometheus  Prometheus 출력을 비활성화합니다.
#
# 기본값 (환경변수로 override 가능):
#   K6_PROMETHEUS_RW_SERVER_URL  - https://prom-dev.tasteam.kr/api/v1/write
#   K6_PROMETHEUS_RW_USERNAME    - (설정된 기본값)
#   K6_PROMETHEUS_RW_PASSWORD    - (설정된 기본값)
#   K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM - true (네이티브 히스토그램)
#
#   BASE_URL - https://stg.tasteam.kr

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
DB_RESET_SCRIPT="${REPO_ROOT}/v1-mvp/scripts/db-reset/reset-dev-db.sh"

# ============ 기본값 설정 ============
# 환경변수가 설정되어 있지 않으면 기본값 사용
export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"

# 테스트 대상 서버
export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

# ============ 옵션 파싱 ============
RESET_DB=false
USE_PROMETHEUS=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --reset-db)
            RESET_DB=true
            shift
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

# ============ DB 초기화 (옵션) ============
if [[ "$RESET_DB" == "true" ]]; then
    echo "🔄 개발 DB 초기화 중..."
    if [[ -f "$DB_RESET_SCRIPT" ]]; then
        bash "$DB_RESET_SCRIPT"
    else
        echo "⚠️  DB 리셋 스크립트를 찾을 수 없습니다: $DB_RESET_SCRIPT"
        echo "   DB 초기화를 건너뜁니다."
    fi
fi

# ============ k6 실행 ============
echo ""
echo "🚀 브레이크포인트 테스트 시작..."
echo "   스크립트: ${SCRIPT_DIR}/breakpoint_test.js"
echo ""

# Prometheus 출력 설정
if [[ "$USE_PROMETHEUS" == "true" ]]; then
    echo "📊 Prometheus remote write 활성화: $K6_PROMETHEUS_RW_SERVER_URL"
    K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
    echo "ℹ️  Prometheus 출력 비활성화 (--no-prometheus 옵션)"
    K6_OUTPUT_ARG=""
fi

echo ""

# Generate specific Test ID
TEST_ID="breakpoint-$(date +%Y%m%d-%H%M%S)"

echo "🆔 Test ID: $TEST_ID"
echo ""

# k6 실행
cd "$SCRIPT_DIR"
k6 run $K6_OUTPUT_ARG \
  --tag testid=$TEST_ID \
  -e TEST_ID=$TEST_ID \
  breakpoint_test.js

echo ""
echo "✅ 브레이크포인트 테스트 완료!"
echo "   - 결과 요약은 위 출력을 확인하세요."
echo "   - Prometheus 출력 시 Grafana 대시보드에서 상세 분석 가능합니다."
