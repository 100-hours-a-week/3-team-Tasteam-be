#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
"${DIR}/traffic.sh" rollback "${1:-auto}"
