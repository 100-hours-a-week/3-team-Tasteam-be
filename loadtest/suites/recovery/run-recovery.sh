#!/bin/bash
#
# 복구 테스트 실행 스크립트
#
# 사용법:
#   ./run-recovery.sh [--reset-db] [--no-prometheus]
#
# 환경변수:
#   BASE_URL  - https://stg.tasteam.kr (기본값)
#
# 옵션:
#   --reset-db       테스트 전 개발 DB를 초기화합니다.
#   --no-prometheus  Prometheus 출력을 비활성화합니다.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "${SCRIPT_DIR}" rev-parse --show-toplevel)"
DB_RESET_SCRIPT="${REPO_ROOT}/v1-mvp/scripts/db-reset/reset-dev-db.sh"

export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

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

if [[ "$RESET_DB" == "true" ]]; then
    echo "개발 DB 초기화 중..."
    if [[ -f "$DB_RESET_SCRIPT" ]]; then
        bash "$DB_RESET_SCRIPT"
    else
        echo "DB 리셋 스크립트를 찾을 수 없습니다: $DB_RESET_SCRIPT"
        echo "DB 초기화를 건너뜁니다."
    fi
fi

echo ""
echo "복구 테스트 시작..."
echo "   Phase 1 (3m): 50→500 VU 스파이크"
echo "   Phase 2 (1m): 500 VU 유지"
echo "   Phase 3 (2m): 500→50 VU 램프다운"
echo "   Phase 4 (10m): 50 VU 저부하 복구 관찰"
echo "   스크립트: ${SCRIPT_DIR}/recovery_test.js"
echo ""

if [[ "$USE_PROMETHEUS" == "true" ]]; then
    echo "Prometheus remote write 활성화: $K6_PROMETHEUS_RW_SERVER_URL"
    K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
    echo "Prometheus 출력 비활성화 (--no-prometheus 옵션)"
    K6_OUTPUT_ARG=""
fi

echo ""

TEST_ID="recovery-$(date +%Y%m%d-%H%M%S)"
echo "Test ID: $TEST_ID"
echo ""

cd "$SCRIPT_DIR"
k6 run $K6_OUTPUT_ARG \
  --tag testid=$TEST_ID \
  -e TEST_ID=$TEST_ID \
  recovery_test.js

echo ""
echo "복구 테스트 완료!"
echo "   - Phase 4 구간의 p95 추이를 Grafana에서 확인하세요."
echo "   - 복구 완료 기준: read p95 < 1.5s, 에러율 < 0.1%"
