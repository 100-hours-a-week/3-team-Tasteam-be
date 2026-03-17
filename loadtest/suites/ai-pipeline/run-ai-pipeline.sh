#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
export TEST_TYPE="${TEST_TYPE:-review-create-burst}"

K6_ARGS=(run)
if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    mkdir -p "$(dirname "$K6_SUMMARY_EXPORT")"
    K6_ARGS+=(--summary-export "$K6_SUMMARY_EXPORT")
fi

echo ""
echo "🚀 AI 파이프라인 부하테스트 시작"
echo "   target=${BASE_URL}"
echo "   type=${TEST_TYPE}"
echo "   script=${SCRIPT_DIR}/ai_pipeline_test.js"
if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    echo "   summary=${K6_SUMMARY_EXPORT}"
fi
echo ""

cd "$SCRIPT_DIR"
K6_ARGS+=(ai_pipeline_test.js)
k6 "${K6_ARGS[@]}"

echo ""
echo "✅ AI 파이프라인 부하테스트 완료"
echo ""
