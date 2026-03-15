#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

export BASE_URL="${BASE_URL:-https://stg.tasteam.kr}"

echo ""
echo "🚀 스모크 테스트 시작"
echo "   target=${BASE_URL}"
echo "   script=${SCRIPT_DIR}/smoke_test.js"
echo ""

cd "$SCRIPT_DIR"
k6 run smoke_test.js

echo ""
echo "✅ 스모크 테스트 완료"
echo ""
