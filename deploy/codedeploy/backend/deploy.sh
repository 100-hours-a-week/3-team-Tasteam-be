#!/usr/bin/env bash
set -euo pipefail

# CodeDeploy hook script for backend deployment.
# Modes: stop | deploy | health

MODE="${1:-deploy}"
ENV_NAME="${ENV_NAME:-prod}"                # dev|stg|prod
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="${APP_DIR:-/opt/tasteam}"
CONTAINER_NAME="${CONTAINER_NAME:-tasteam-be}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${CONTAINER_PORT}/actuator/health}"

ECR_REGISTRY="${ECR_REGISTRY:-}"
ECR_REPO_BACKEND="${ECR_REPO_BACKEND:-}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-${APP_DIR}/backend.env}"

export AWS_PAGER=""

log() {
  echo "[deploy] $*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing command: $1"
    exit 1
  }
}

fetch_ssm_env() {
  local path="$1"
  local out_file="$2"
  local tmp_file

  tmp_file="$(mktemp)"

  aws ssm get-parameters-by-path \
    --region "${AWS_REGION}" \
    --path "${path}" \
    --recursive \
    --with-decryption \
    --query "Parameters[*].[Name,Value]" \
    --output text > "${tmp_file}"

  : > "${out_file}"
  while IFS= read -r line || [ -n "$line" ]; do
    [ -z "$line" ] && continue

    local name value key
    name="${line%%$'\t'*}"
    value="${line#*$'\t'}"
    key="${name##*/}"

    printf '%s=%s\n' "$key" "$value" >> "${out_file}"
  done < "${tmp_file}"

  rm -f "${tmp_file}"
  chmod 600 "${out_file}"
}

stop_container() {
  log "stop container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true
}

deploy_backend() {
  require_cmd aws
  require_cmd docker

  if [ -z "${ECR_REGISTRY}" ] || [ -z "${ECR_REPO_BACKEND}" ]; then
    log "ECR_REGISTRY/ECR_REPO_BACKEND is required"
    exit 1
  fi

  mkdir -p "${APP_DIR}"

  log "fetch backend env from /${ENV_NAME}/tasteam/backend"
  fetch_ssm_env "/${ENV_NAME}/tasteam/backend" "${BACKEND_ENV_FILE}"

  local image
  image="${ECR_REGISTRY}/${ECR_REPO_BACKEND}:${IMAGE_TAG}"

  log "ecr login"
  aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

  log "pull image: ${image}"
  docker pull "${image}"

  stop_container

  log "run container"
  docker run -d \
    --name "${CONTAINER_NAME}" \
    --restart unless-stopped \
    --env-file "${BACKEND_ENV_FILE}" \
    -p "${CONTAINER_PORT}:8080" \
    "${image}"
}

health_check() {
  require_cmd curl

  local i
  for i in $(seq 1 30); do
    if curl -fsS "${HEALTH_URL}" >/dev/null; then
      log "health check passed"
      return 0
    fi
    sleep 2
  done

  log "health check failed: ${HEALTH_URL}"
  return 1
}

case "${MODE}" in
  stop)
    require_cmd docker
    stop_container
    ;;
  deploy)
    deploy_backend
    ;;
  health)
    health_check
    ;;
  *)
    log "unknown mode: ${MODE}"
    exit 1
    ;;
esac
