#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
"${DIR}/traffic.sh" switch "${1:-auto}"
