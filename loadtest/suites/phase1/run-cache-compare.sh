#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULT_DIR="${SCRIPT_DIR}/results"
mkdir -p "$RESULT_DIR"

export K6_PROMETHEUS_RW_SERVER_URL="${K6_PROMETHEUS_RW_SERVER_URL:-https://prom-dev.tasteam.kr/api/v1/write}"
export K6_PROMETHEUS_RW_USERNAME="${K6_PROMETHEUS_RW_USERNAME:-tasteam}"
export K6_PROMETHEUS_RW_PASSWORD="${K6_PROMETHEUS_RW_PASSWORD:-tasteam-k6-metrics}"
export K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM="${K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM:-true}"
export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

SUITE="mixed"
USE_PROMETHEUS=true

while [[ $# -gt 0 ]]; do
  case $1 in
    --suite)
      SUITE="$2"
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

RUN_ID="cache-compare-${SUITE}-$(date +%Y%m%d-%H%M%S)"
OFF_TEST_ID="${RUN_ID}-off"
ON_TEST_ID="${RUN_ID}-on"
OFF_SUMMARY="${RESULT_DIR}/${OFF_TEST_ID}-summary.json"
ON_SUMMARY="${RESULT_DIR}/${ON_TEST_ID}-summary.json"
REPORT_FILE="${RESULT_DIR}/${RUN_ID}-report.md"

if [[ "$USE_PROMETHEUS" == "true" ]]; then
  K6_OUTPUT_ARG="-o experimental-prometheus-rw"
else
  K6_OUTPUT_ARG=""
fi

cd "$SCRIPT_DIR"

echo ""
echo "🚀 Cache/Kafka OFF 기준선 실행"
echo "   suite=${SUITE} | testid=${OFF_TEST_ID}"
k6 run $K6_OUTPUT_ARG \
  --summary-export "$OFF_SUMMARY" \
  --tag testid="$OFF_TEST_ID" \
  -e TEST_ID="$OFF_TEST_ID" \
  -e TEST_SUITE="$SUITE" \
  -e CACHE_MODE=off \
  phase1_test.js

echo ""
echo "🚀 Cache/Kafka ON 개선안 실행"
echo "   suite=${SUITE} | testid=${ON_TEST_ID}"
k6 run $K6_OUTPUT_ARG \
  --summary-export "$ON_SUMMARY" \
  --tag testid="$ON_TEST_ID" \
  -e TEST_ID="$ON_TEST_ID" \
  -e TEST_SUITE="$SUITE" \
  -e CACHE_MODE=on \
  phase1_test.js

cat > "$REPORT_FILE" <<REPORT
# Cache/Kafka OFF vs ON 비교 리포트

- Run ID: ${RUN_ID}
- Target: ${BASE_URL}
- Suite: ${SUITE}
- OFF Test ID: ${OFF_TEST_ID}
- ON Test ID: ${ON_TEST_ID}
- OFF Summary: ${OFF_SUMMARY}
- ON Summary: ${ON_SUMMARY}

## 판정 기준

- 에러율: 1차 기준 0.3% 이하(또는 최소 1% 이하)
- 지연시간: read p95 < 1s, write p95 < 3s, 전체 p95 < 2.2s
- 개선율: ON이 OFF 대비 p95 latency 30% 이상 개선

## 결과 요약 (수동 기입)

| 항목 | OFF | ON | 개선율 | 판정 |
|---|---:|---:|---:|---|
| http_req_duration p95 (ms) |  |  |  |  |
| http_req_duration p99 (ms) |  |  |  |  |
| read p95 (ms) |  |  |  |  |
| write p95 (ms) |  |  |  |  |
| http_req_failed rate (%) |  |  |  |  |
| Redis hit ratio (%) |  |  |  |  |
| Kafka consumer lag |  |  |  |  |

## 이상 징후

- 401/403 비중:
- 429 + 5xx 동반상승 여부:
- Redis hit ratio 급감(<60%) 여부:
- Kafka lag 급증 여부:

## 결론

- 합격/보류:
- 조치 항목:
REPORT

echo ""
echo "✅ 비교 실행 완료"
echo "   report=${REPORT_FILE}"
echo ""
