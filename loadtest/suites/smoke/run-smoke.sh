#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

K6_ARGS=(run)
if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    mkdir -p "$(dirname "$K6_SUMMARY_EXPORT")"
    K6_ARGS+=(--summary-export "$K6_SUMMARY_EXPORT")
fi

echo ""
echo "🚀 스모크 테스트 시작"
echo "   target=${BASE_URL}"
echo "   script=${SCRIPT_DIR}/smoke_test.js"
if [[ -n "${K6_SUMMARY_EXPORT:-}" ]]; then
    echo "   summary=${K6_SUMMARY_EXPORT}"
fi
echo ""

cd "$SCRIPT_DIR"
K6_ARGS+=(smoke_test.js)
k6 "${K6_ARGS[@]}"

echo ""
echo "✅ 스모크 테스트 완료"
echo ""
