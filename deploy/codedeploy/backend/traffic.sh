#!/usr/bin/env bash
set -euo pipefail

# Caddy traffic switch helper for Spring blue/green instances.
# Modes: switch | rollback | status

MODE="${1:-status}"
REQUESTED_SLOT="${2:-auto}" # blue|green|auto

CADDYFILE="${CADDYFILE:-/etc/caddy/Caddyfile}"
BLUE_UPSTREAM="${BLUE_UPSTREAM:-10.0.1.10:8080}"
GREEN_UPSTREAM="${GREEN_UPSTREAM:-10.0.1.11:8080}"
ACTIVE_SLOT_FILE="${ACTIVE_SLOT_FILE:-/opt/tasteam/caddy-active-slot}"
PREV_SLOT_FILE="${PREV_SLOT_FILE:-/opt/tasteam/caddy-prev-slot}"

HEALTH_SCHEME="${HEALTH_SCHEME:-http}"
HEALTH_PATH="${HEALTH_PATH:-/actuator/health}"
HEALTH_RETRIES="${HEALTH_RETRIES:-30}"
HEALTH_INTERVAL_SEC="${HEALTH_INTERVAL_SEC:-2}"
POST_SWITCH_HEALTH_URL="${POST_SWITCH_HEALTH_URL:-}"

log() {
  echo "[traffic] $*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing command: $1"
    exit 1
  }
}

slot_to_upstream() {
  case "$1" in
    blue) echo "${BLUE_UPSTREAM}" ;;
    green) echo "${GREEN_UPSTREAM}" ;;
    *)
      log "invalid slot: $1"
      exit 1
      ;;
  esac
}

other_slot() {
  case "$1" in
    blue) echo "green" ;;
    green) echo "blue" ;;
    *)
      log "invalid slot: $1"
      exit 1
      ;;
  esac
}

detect_active_slot() {
  if [ -f "${ACTIVE_SLOT_FILE}" ]; then
    local slot
    slot="$(tr -d '[:space:]' < "${ACTIVE_SLOT_FILE}" || true)"
    if [ "${slot}" = "blue" ] || [ "${slot}" = "green" ]; then
      echo "${slot}"
      return 0
    fi
  fi

  local has_blue has_green
  has_blue=0
  has_green=0
  grep -F "${BLUE_UPSTREAM}" "${CADDYFILE}" >/dev/null 2>&1 && has_blue=1
  grep -F "${GREEN_UPSTREAM}" "${CADDYFILE}" >/dev/null 2>&1 && has_green=1

  if [ "${has_blue}" -eq 1 ] && [ "${has_green}" -eq 0 ]; then
    echo "blue"
    return 0
  fi
  if [ "${has_green}" -eq 1 ] && [ "${has_blue}" -eq 0 ]; then
    echo "green"
    return 0
  fi

  log "cannot determine active slot. set ${ACTIVE_SLOT_FILE} explicitly"
  exit 1
}

wait_healthy() {
  local slot="$1"
  local target i
  target="$(slot_to_upstream "${slot}")"
  log "health check ${slot}: ${HEALTH_SCHEME}://${target}${HEALTH_PATH}"

  for i in $(seq 1 "${HEALTH_RETRIES}"); do
    if curl -fsS "${HEALTH_SCHEME}://${target}${HEALTH_PATH}" >/dev/null; then
      log "health check passed (${slot})"
      return 0
    fi
    sleep "${HEALTH_INTERVAL_SEC}"
  done
  log "health check failed (${slot})"
  return 1
}

post_switch_verify() {
  [ -z "${POST_SWITCH_HEALTH_URL}" ] && return 0
  local i
  for i in $(seq 1 "${HEALTH_RETRIES}"); do
    if curl -fsS "${POST_SWITCH_HEALTH_URL}" >/dev/null; then
      log "post-switch health passed"
      return 0
    fi
    sleep "${HEALTH_INTERVAL_SEC}"
  done
  log "post-switch health failed: ${POST_SWITCH_HEALTH_URL}"
  return 1
}

replace_upstream() {
  local from="$1"
  local to="$2"
  local tmp
  tmp="$(mktemp)"
  sed "s|${from}|${to}|g" "${CADDYFILE}" > "${tmp}"
  cat "${tmp}" > "${CADDYFILE}"
  rm -f "${tmp}"
}

validate_and_reload() {
  caddy validate --config "${CADDYFILE}" >/dev/null
  caddy reload --config "${CADDYFILE}" >/dev/null
}

switch_slot() {
  local target_slot="$1"
  local active_slot from_upstream to_upstream backup

  [ "${target_slot}" = "blue" ] || [ "${target_slot}" = "green" ] || {
    log "invalid target slot: ${target_slot}"
    exit 1
  }

  active_slot="$(detect_active_slot)"
  if [ "${active_slot}" = "${target_slot}" ]; then
    log "already active: ${target_slot}"
    wait_healthy "${target_slot}"
    return 0
  fi

  wait_healthy "${target_slot}"

  from_upstream="$(slot_to_upstream "${active_slot}")"
  to_upstream="$(slot_to_upstream "${target_slot}")"

  mkdir -p "$(dirname "${ACTIVE_SLOT_FILE}")"
  mkdir -p "$(dirname "${PREV_SLOT_FILE}")"
  printf '%s\n' "${active_slot}" > "${PREV_SLOT_FILE}"

  backup="${CADDYFILE}.backup.$(date +%Y%m%d%H%M%S)"
  cp "${CADDYFILE}" "${backup}"

  replace_upstream "${from_upstream}" "${to_upstream}"
  if ! validate_and_reload; then
    cp "${backup}" "${CADDYFILE}"
    validate_and_reload || true
    log "switch failed. caddy restored"
    return 1
  fi

  if ! post_switch_verify; then
    cp "${backup}" "${CADDYFILE}"
    validate_and_reload || true
    printf '%s\n' "${active_slot}" > "${ACTIVE_SLOT_FILE}"
    log "post-switch verify failed. rolled back"
    return 1
  fi

  printf '%s\n' "${target_slot}" > "${ACTIVE_SLOT_FILE}"
  log "switch complete: ${active_slot} -> ${target_slot}"
}

run_switch() {
  local target
  if [ "${REQUESTED_SLOT}" = "auto" ]; then
    target="$(other_slot "$(detect_active_slot)")"
  else
    target="${REQUESTED_SLOT}"
  fi
  switch_slot "${target}"
}

run_rollback() {
  local target
  if [ "${REQUESTED_SLOT}" = "auto" ]; then
    if [ -f "${PREV_SLOT_FILE}" ]; then
      target="$(tr -d '[:space:]' < "${PREV_SLOT_FILE}" || true)"
    else
      target="$(other_slot "$(detect_active_slot)")"
    fi
  else
    target="${REQUESTED_SLOT}"
  fi
  switch_slot "${target}"
}

run_status() {
  log "active slot: $(detect_active_slot)"
  log "blue upstream: ${BLUE_UPSTREAM}"
  log "green upstream: ${GREEN_UPSTREAM}"
  grep -n "reverse_proxy" "${CADDYFILE}" || true
}

require_cmd sed
require_cmd curl
require_cmd caddy

case "${MODE}" in
  switch) run_switch ;;
  rollback) run_rollback ;;
  status) run_status ;;
  *)
    log "unknown mode: ${MODE}"
    exit 1
    ;;
esac
