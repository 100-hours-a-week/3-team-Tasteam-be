#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SMOKE_SCRIPT="${SCRIPT_DIR}/suites/smoke/run-smoke.sh"
REALISTIC_SCRIPT="${SCRIPT_DIR}/suites/realistic/run-realistic.sh"

RESULT_ROOT="${RESULT_ROOT:-${SCRIPT_DIR}/results/smoke-realistic}"
USE_PROMETHEUS=true

usage() {
    cat <<'EOF'
사용법:
  ./run-smoke-realistic.sh [--no-prometheus] [--results-dir <dir>] [--help]

설명:
  1. smoke suite를 먼저 실행합니다.
  2. smoke가 성공하면 realistic suite를 이어서 실행합니다.
  3. 각 단계의 로그와 k6 summary json을 결과 디렉터리에 저장합니다.

옵션:
  --no-prometheus    realistic 실행의 Prometheus remote write를 비활성화합니다.
  --results-dir DIR  실행 산출물 루트 디렉터리를 지정합니다.
  --help             도움말을 출력합니다.

공통 환경변수:
  BASE_URL
  TEST_GROUP_CODE
  GROUP_SEARCH_KEYWORDS
  USER_POOL
  REVERSE_GEOCODE_MODE
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-prometheus)
            USE_PROMETHEUS=false
            shift
            ;;
        --results-dir)
            if [[ $# -lt 2 ]]; then
                echo "옵션 --results-dir 에 경로가 필요합니다."
                exit 1
            fi
            RESULT_ROOT="$2"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "알 수 없는 옵션: $1"
            echo ""
            usage
            exit 1
            ;;
    esac
done

RUN_ID="smoke-realistic-$(date +%Y%m%d-%H%M%S)"
RUN_DIR="${RESULT_ROOT}/${RUN_ID}"
SMOKE_LOG="${RUN_DIR}/smoke.log"
SMOKE_SUMMARY="${RUN_DIR}/smoke-summary.json"
REALISTIC_LOG="${RUN_DIR}/realistic.log"
REALISTIC_SUMMARY="${RUN_DIR}/realistic-summary.json"

mkdir -p "$RUN_DIR"

run_and_capture() {
    local log_file="$1"
    shift

    set +e
    "$@" 2>&1 | tee "$log_file"
    local command_status=${PIPESTATUS[0]}
    set -e

    return "$command_status"
}

echo ""
echo "🚀 Smoke -> Realistic 원클릭 부하 테스트 시작"
echo "   target=${BASE_URL:-https://stg.tasteam.kr}"
echo "   result_dir=${RUN_DIR}"
echo "   smoke_log=${SMOKE_LOG}"
echo "   realistic_log=${REALISTIC_LOG}"
echo ""

if ! run_and_capture "$SMOKE_LOG" env K6_SUMMARY_EXPORT="$SMOKE_SUMMARY" "$SMOKE_SCRIPT"; then
    echo ""
    echo "❌ smoke 단계 실패로 realistic 실행을 중단합니다."
    echo "   smoke_log=${SMOKE_LOG}"
    echo "   smoke_summary=${SMOKE_SUMMARY}"
    exit 1
fi

REALISTIC_COMMAND=(env K6_SUMMARY_EXPORT="$REALISTIC_SUMMARY" "$REALISTIC_SCRIPT")
if [[ "$USE_PROMETHEUS" == "false" ]]; then
    REALISTIC_COMMAND+=(--no-prometheus)
fi

if ! run_and_capture "$REALISTIC_LOG" "${REALISTIC_COMMAND[@]}"; then
    echo ""
    echo "❌ realistic 단계 실패"
    echo "   realistic_log=${REALISTIC_LOG}"
    echo "   realistic_summary=${REALISTIC_SUMMARY}"
    exit 1
fi

echo ""
echo "✅ Smoke -> Realistic 원클릭 부하 테스트 완료"
echo "   smoke_summary=${SMOKE_SUMMARY}"
echo "   realistic_summary=${REALISTIC_SUMMARY}"
echo ""
