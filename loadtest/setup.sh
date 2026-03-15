#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

need_okay=0
warn_only=0

check_cmd() {
  local cmd=$1
  if command -v "$cmd" >/dev/null 2>&1; then
    echo "OK: $cmd"
    return 0
  fi

  echo "MISSING: $cmd"
  return 1
}

echo "🚀 부하테스트 환경 체크 시작"

check_cmd k6 || need_okay=1
check_cmd python3 || need_okay=1

if check_cmd locust; then
  :
else
  warn_only=1
fi

if [[ ! -f "$SCRIPT_DIR/.envrc" ]]; then
  echo "MISSING: .envrc"
  need_okay=1
else
  echo "OK: .envrc"
fi

if [[ $need_okay -ne 0 ]]; then
  echo "필수 구성요소가 누락되었습니다. 누락 항목을 설치/복원한 뒤 재시도하세요."
  exit 1
fi

if [[ $warn_only -ne 0 ]]; then
  echo "WARN: locust가 없어 locust 기반 suite는 실행되지 않습니다."
  echo "설치: python3 -m pip install locust"
fi

echo "부하테스트 실행용 최소 환경이 준비되었습니다."
echo "예시: (cd $SCRIPT_DIR && source .envrc && cd suites/smoke && ./run-smoke.sh --no-prometheus)"
