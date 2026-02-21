#!/usr/bin/env bash
set -euo pipefail

# CodeDeploy hook script for backend deployment.
# Modes: stop | deploy | health

MODE="${1:-deploy}"
ENV_NAME="${ENV_NAME:-}"                    # dev|stg|prod (required)
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
APP_DIR="${APP_DIR:-/opt/tasteam}"
CONTAINER_NAME="${CONTAINER_NAME:-tasteam-be}"
CONTAINER_PORT="${CONTAINER_PORT:-8080}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${CONTAINER_PORT}/actuator/health}"
BACKEND_SERVICE_NAME="${BACKEND_SERVICE_NAME:-backend}"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-tasteam-be}"

ECR_REGISTRY="${ECR_REGISTRY:-}"
ECR_REPO_BACKEND="${ECR_REPO_BACKEND:-}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-${APP_DIR}/backend.env}"
DEPLOY_ENV_FILE="${DEPLOY_ENV_FILE:-${SCRIPT_DIR}/.env.deploy}"
BACKEND_COMPOSE_FILE="${BACKEND_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.backend.yml}"
DEV_INFRA_COMPOSE_FILE="${DEV_INFRA_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.dev-infra.yml}"
DEV_INFRA_DB_CONTAINER="${DEV_INFRA_DB_CONTAINER:-tasteam-dev-db}"
DEV_INFRA_REDIS_CONTAINER="${DEV_INFRA_REDIS_CONTAINER:-tasteam-dev-redis}"

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

load_deploy_env_file() {
  if [ -f "${DEPLOY_ENV_FILE}" ]; then
    log "load deploy env file: ${DEPLOY_ENV_FILE}"
    set -a
    # shellcheck disable=SC1090
    . "${DEPLOY_ENV_FILE}"
    set +a
  fi
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

  if [ ! -s "${out_file}" ]; then
    log "no SSM parameters found for path: ${path}"
    exit 1
  fi
}

read_env_value() {
  local key="$1"
  sed -n "s/^${key}=//p" "${BACKEND_ENV_FILE}" | head -n 1
}

validate_backend_env() {
  local max_file_size max_history
  max_file_size="$(read_env_value "LOG_MAX_FILE_SIZE")"
  max_history="$(read_env_value "LOG_MAX_HISTORY")"

  if [ -z "${max_file_size}" ] || [ "${max_file_size}" = "PLACEHOLDER" ]; then
    log "invalid LOG_MAX_FILE_SIZE in ${BACKEND_ENV_FILE} (value='${max_file_size:-}')"
    exit 1
  fi

  if [ -z "${max_history}" ] || [ "${max_history}" = "PLACEHOLDER" ]; then
    log "invalid LOG_MAX_HISTORY in ${BACKEND_ENV_FILE} (value='${max_history:-}')"
    exit 1
  fi

  if ! printf '%s' "${max_file_size}" | grep -Eq '^[0-9]+(KB|MB|GB)$'; then
    log "LOG_MAX_FILE_SIZE format invalid: ${max_file_size} (expected like 100MB)"
    exit 1
  fi

  if ! printf '%s' "${max_history}" | grep -Eq '^[0-9]+$'; then
    log "LOG_MAX_HISTORY format invalid: ${max_history} (expected integer)"
    exit 1
  fi
}

compose_up() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return 0
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return 0
  fi

  log "docker compose command not found"
  return 1
}

backend_compose() {
  if [ ! -f "${BACKEND_COMPOSE_FILE}" ]; then
    log "backend compose file not found: ${BACKEND_COMPOSE_FILE}"
    return 1
  fi

  BACKEND_IMAGE="${BACKEND_IMAGE:-public.ecr.aws/docker/library/busybox:latest}" \
  BACKEND_ENV_FILE="${BACKEND_ENV_FILE}" \
  CONTAINER_NAME="${CONTAINER_NAME}" \
  CONTAINER_PORT="${CONTAINER_PORT}" \
  compose_up --project-name "${COMPOSE_PROJECT_NAME}" -f "${BACKEND_COMPOSE_FILE}" "$@"
}

wait_container_healthy() {
  local container_name="$1"
  local retries="${2:-40}"
  local interval="${3:-2}"
  local i status

  for i in $(seq 1 "${retries}"); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "${container_name}" 2>/dev/null || true)"
    case "${status}" in
      healthy|running)
        return 0
        ;;
      unhealthy|exited|dead)
        log "container unhealthy: ${container_name} (status=${status})"
        docker logs --tail 100 "${container_name}" || true
        return 1
        ;;
    esac
    sleep "${interval}"
  done

  log "container health timeout: ${container_name}"
  docker logs --tail 100 "${container_name}" || true
  return 1
}

start_dev_infra() {
  if [ "${ENV_NAME}" != "dev" ]; then
    return 0
  fi

  if [ ! -f "${DEV_INFRA_COMPOSE_FILE}" ]; then
    log "dev infra compose file not found: ${DEV_INFRA_COMPOSE_FILE}"
    exit 1
  fi

  log "start dev local infra (db/redis): ${DEV_INFRA_COMPOSE_FILE}"
  compose_up --env-file "${BACKEND_ENV_FILE}" -f "${DEV_INFRA_COMPOSE_FILE}" up -d db redis

  wait_container_healthy "${DEV_INFRA_DB_CONTAINER}" 60 2
  wait_container_healthy "${DEV_INFRA_REDIS_CONTAINER}" 30 2
  log "dev local infra is ready"
}

stop_container() {
  if [ -f "${BACKEND_COMPOSE_FILE}" ]; then
    log "stop compose service: ${BACKEND_SERVICE_NAME}"
    backend_compose rm -fsv "${BACKEND_SERVICE_NAME}" || true
  fi

  log "stop container fallback: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true
}

deploy_backend() {
  require_cmd aws
  require_cmd docker

  if [ -z "${ENV_NAME}" ]; then
    log "ENV_NAME is required (dev|stg|prod)"
    exit 1
  fi

  if [ -z "${ECR_REGISTRY}" ] || [ -z "${ECR_REPO_BACKEND}" ]; then
    log "ECR_REGISTRY/ECR_REPO_BACKEND is required"
    exit 1
  fi

  log "deploy config: env=${ENV_NAME}, region=${AWS_REGION}, image=${ECR_REGISTRY}/${ECR_REPO_BACKEND}:${IMAGE_TAG}"

  mkdir -p "${APP_DIR}"

  log "fetch backend env from /${ENV_NAME}/tasteam/backend"
  fetch_ssm_env "/${ENV_NAME}/tasteam/backend" "${BACKEND_ENV_FILE}"
  validate_backend_env
  start_dev_infra

  local image
  image="${ECR_REGISTRY}/${ECR_REPO_BACKEND}:${IMAGE_TAG}"

  log "ecr login"
  aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

  log "pull image: ${image}"
  docker pull "${image}"

  stop_container

  BACKEND_IMAGE="${image}"
  export BACKEND_IMAGE BACKEND_ENV_FILE CONTAINER_NAME CONTAINER_PORT

  log "run backend by docker compose (service=${BACKEND_SERVICE_NAME})"
  backend_compose up -d --remove-orphans "${BACKEND_SERVICE_NAME}"
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

load_deploy_env_file

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
