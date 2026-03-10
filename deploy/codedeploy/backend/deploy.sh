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
HEALTH_RETRIES="${HEALTH_RETRIES:-45}"
HEALTH_INTERVAL_SECONDS="${HEALTH_INTERVAL_SECONDS:-2}"

ECR_REGISTRY="${ECR_REGISTRY:-}"
ECR_REPO_BACKEND="${ECR_REPO_BACKEND:-}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
DOCKER_PRUNE_UNUSED_IMAGES="${DOCKER_PRUNE_UNUSED_IMAGES:-true}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_ENV_FILE="${BACKEND_ENV_FILE:-${APP_DIR}/backend.env}"
DEPLOY_ENV_FILE="${DEPLOY_ENV_FILE:-${SCRIPT_DIR}/.env.deploy}"
BACKEND_COMPOSE_FILE="${BACKEND_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.backend.yml}"
DEV_INFRA_COMPOSE_FILE="${DEV_INFRA_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.dev-infra.yml}"
DEV_INFRA_DB_CONTAINER="${DEV_INFRA_DB_CONTAINER:-tasteam-dev-db}"
DEV_INFRA_REDIS_CONTAINER="${DEV_INFRA_REDIS_CONTAINER:-tasteam-dev-redis}"
DEV_INFRA_COMPOSE_PROJECT_NAME="${DEV_INFRA_COMPOSE_PROJECT_NAME:-tasteam-dev-infra}"
MONITORING_ENV_FILE="${MONITORING_ENV_FILE:-${APP_DIR}/monitoring.env}"
ALLOY_COMPOSE_FILE="${ALLOY_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.alloy.yml}"
ALLOY_COMPOSE_PROJECT_NAME="${ALLOY_COMPOSE_PROJECT_NAME:-tasteam-alloy}"

export AWS_PAGER=""

prune_unused_docker_images() {
  local phase="$1"

  if [ "${DOCKER_PRUNE_UNUSED_IMAGES}" != "true" ]; then
    log "skip docker image prune (${phase})"
    return 0
  fi

  log "docker image prune (${phase})"
  docker system df || true
  docker image prune -af || true
  docker system df || true
}

log() {
  echo "[deploy] $*"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    log "missing command: $1"
    exit 1
  }
}

cloud_map_required() {
  [ "${ENV_NAME}" = "prod" ] || [ "${ENV_NAME}" = "stg" ]
}

fetch_imds_token() {
  curl -fsS -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600"
}

get_instance_id() {
  local token
  token="$(fetch_imds_token)"

  curl -fsS -H "X-aws-ec2-metadata-token: ${token}" \
    http://169.254.169.254/latest/meta-data/instance-id
}

resolve_cloud_map_service_id() {
  if [ -n "${CLOUD_MAP_SERVICE_ID:-}" ]; then
    printf '%s\n' "${CLOUD_MAP_SERVICE_ID}"
    return 0
  fi

  if [ -f "${BACKEND_ENV_FILE}" ]; then
    sed -n 's/^CLOUD_MAP_SERVICE_ID=//p' "${BACKEND_ENV_FILE}" | head -n 1
  fi
}

update_cloud_map_instance_health() {
  local status="$1"
  local service_id instance_id

  service_id="$(resolve_cloud_map_service_id)"
  if [ -z "${service_id}" ]; then
    if cloud_map_required; then
      log "missing CLOUD_MAP_SERVICE_ID for ${ENV_NAME} deployment"
      return 1
    fi

    log "Cloud Map service id not configured; skip health update (${status})"
    return 0
  fi

  instance_id="$(get_instance_id)"
  log "update Cloud Map custom health: service=${service_id}, instance=${instance_id}, status=${status}"
  aws servicediscovery update-instance-custom-health-status \
    --region "${AWS_REGION}" \
    --service-id "${service_id}" \
    --instance-id "${instance_id}" \
    --status "${status}" >/dev/null
}

mark_cloud_map_instance_unhealthy() {
  update_cloud_map_instance_health UNHEALTHY
}

mark_cloud_map_instance_healthy() {
  update_cloud_map_instance_health HEALTHY
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

upsert_env_value() {
  local key="$1"
  local value="$2"
  local tmp_file

  tmp_file="$(mktemp)"
  if [ -f "${BACKEND_ENV_FILE}" ]; then
    grep -Ev "^${key}=" "${BACKEND_ENV_FILE}" > "${tmp_file}" || true
  fi
  printf '%s=%s\n' "${key}" "${value}" >> "${tmp_file}"
  mv "${tmp_file}" "${BACKEND_ENV_FILE}"
  chmod 600 "${BACKEND_ENV_FILE}"
}

validate_required_backend_env() {
  local required_keys key value
  required_keys=(DB_URL DB_USERNAME DB_PASSWORD REDIS_HOST REDIS_PORT)

  for key in "${required_keys[@]}"; do
    value="$(read_env_value "${key}")"
    if [ -z "${value}" ] || [ "${value}" = "PLACEHOLDER" ]; then
      log "missing or invalid ${key} in ${BACKEND_ENV_FILE}"
      exit 1
    fi
  done
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

trim_trailing_zeros() {
  local value="$1"
  printf '%s' "${value}" | sed -E 's/\.?0+$//'
}

mcpu_to_cpus() {
  local milli_cpu="$1"
  local cpu_value

  cpu_value="$(awk -v m="${milli_cpu}" 'BEGIN { printf "%.3f", m / 1000 }')"
  trim_trailing_zeros "${cpu_value}"
}

detect_total_vcpu() {
  local vcpu

  vcpu="$(getconf _NPROCESSORS_ONLN 2>/dev/null || true)"
  if [ -z "${vcpu}" ] && command -v nproc >/dev/null 2>&1; then
    vcpu="$(nproc 2>/dev/null || true)"
  fi

  if ! printf '%s' "${vcpu}" | grep -Eq '^[0-9]+$'; then
    vcpu=2
  fi
  if [ "${vcpu}" -lt 1 ]; then
    vcpu=1
  fi

  printf '%s\n' "${vcpu}"
}

detect_total_memory_mb() {
  local mem_kb mem_mb mem_bytes

  mem_kb="$(awk '/MemTotal:/ {print $2}' /proc/meminfo 2>/dev/null || true)"
  if printf '%s' "${mem_kb}" | grep -Eq '^[0-9]+$'; then
    mem_mb=$((mem_kb / 1024))
  elif command -v sysctl >/dev/null 2>&1; then
    mem_bytes="$(sysctl -n hw.memsize 2>/dev/null || true)"
    if printf '%s' "${mem_bytes}" | grep -Eq '^[0-9]+$'; then
      mem_mb=$((mem_bytes / 1024 / 1024))
    fi
  fi

  if ! printf '%s' "${mem_mb:-}" | grep -Eq '^[0-9]+$'; then
    mem_mb=4096
  fi
  if [ "${mem_mb}" -lt 1024 ]; then
    mem_mb=1024
  fi

  printf '%s\n' "${mem_mb}"
}

set_runtime_resource_limits() {
  local total_vcpu total_mem_mb
  local total_mcpu cpu_host_reserve_mcpu cpu_budget_mcpu
  local backend_mcpu alloy_mcpu
  local mem_host_reserve_mb mem_budget_mb
  local backend_mem_mb alloy_mem_mb
  local backend_mem_reservation_mb alloy_mem_reservation_mb
  local dev_infra_cpu_reserve_mcpu dev_infra_mem_reserve_mb

  total_vcpu="$(detect_total_vcpu)"
  total_mem_mb="$(detect_total_memory_mb)"

  if [ "${ENV_NAME}" = "dev" ]; then
    dev_infra_cpu_reserve_mcpu="${DEV_INFRA_CPU_RESERVE_MCPU:-300}"
    dev_infra_mem_reserve_mb="${DEV_INFRA_MEMORY_RESERVE_MB:-1024}"

    if ! printf '%s' "${dev_infra_cpu_reserve_mcpu}" | grep -Eq '^[0-9]+$'; then
      dev_infra_cpu_reserve_mcpu=300
    fi
    if ! printf '%s' "${dev_infra_mem_reserve_mb}" | grep -Eq '^[0-9]+$'; then
      dev_infra_mem_reserve_mb=1024
    fi
  else
    dev_infra_cpu_reserve_mcpu=0
    dev_infra_mem_reserve_mb=0
  fi

  total_mcpu=$((total_vcpu * 1000))
  cpu_host_reserve_mcpu=$((total_mcpu * 15 / 100))
  if [ "${cpu_host_reserve_mcpu}" -lt 300 ]; then
    cpu_host_reserve_mcpu=300
  fi
  if [ "${cpu_host_reserve_mcpu}" -gt 1200 ]; then
    cpu_host_reserve_mcpu=1200
  fi
  cpu_budget_mcpu=$((total_mcpu - cpu_host_reserve_mcpu - dev_infra_cpu_reserve_mcpu))
  if [ "${cpu_budget_mcpu}" -lt 500 ]; then
    cpu_budget_mcpu=500
  fi

  backend_mcpu=$((cpu_budget_mcpu * 75 / 100))
  alloy_mcpu=$((cpu_budget_mcpu - backend_mcpu))
  if [ "${alloy_mcpu}" -lt 200 ] && [ "${cpu_budget_mcpu}" -gt 700 ]; then
    alloy_mcpu=200
    backend_mcpu=$((cpu_budget_mcpu - alloy_mcpu))
  fi

  mem_host_reserve_mb=$((total_mem_mb * 25 / 100))
  if [ "${mem_host_reserve_mb}" -lt 768 ]; then
    mem_host_reserve_mb=768
  fi
  if [ "${mem_host_reserve_mb}" -gt 4096 ]; then
    mem_host_reserve_mb=4096
  fi
  mem_budget_mb=$((total_mem_mb - mem_host_reserve_mb - dev_infra_mem_reserve_mb))
  if [ "${mem_budget_mb}" -lt 1024 ]; then
    mem_budget_mb=$((total_mem_mb * 55 / 100))
  fi

  backend_mem_mb=$((mem_budget_mb * 70 / 100))
  alloy_mem_mb=$((mem_budget_mb - backend_mem_mb))
  if [ "${alloy_mem_mb}" -lt 384 ] && [ "${mem_budget_mb}" -gt 1280 ]; then
    alloy_mem_mb=384
    backend_mem_mb=$((mem_budget_mb - alloy_mem_mb))
  fi

  backend_mem_reservation_mb=$((backend_mem_mb * 75 / 100))
  alloy_mem_reservation_mb=$((alloy_mem_mb * 75 / 100))

  : "${BACKEND_CPU_LIMIT:=$(mcpu_to_cpus "${backend_mcpu}")}"
  : "${ALLOY_CPU_LIMIT:=$(mcpu_to_cpus "${alloy_mcpu}")}"
  : "${BACKEND_MEMORY_LIMIT:=${backend_mem_mb}m}"
  : "${ALLOY_MEMORY_LIMIT:=${alloy_mem_mb}m}"
  : "${BACKEND_MEMORY_RESERVATION:=${backend_mem_reservation_mb}m}"
  : "${ALLOY_MEMORY_RESERVATION:=${alloy_mem_reservation_mb}m}"

  export BACKEND_CPU_LIMIT ALLOY_CPU_LIMIT
  export BACKEND_MEMORY_LIMIT ALLOY_MEMORY_LIMIT
  export BACKEND_MEMORY_RESERVATION ALLOY_MEMORY_RESERVATION

  log "resource limits (host=${total_vcpu}vCPU/${total_mem_mb}MiB, dev-infra-reserve=${dev_infra_cpu_reserve_mcpu}mCPU/${dev_infra_mem_reserve_mb}MiB): \
backend=${BACKEND_CPU_LIMIT} CPU, ${BACKEND_MEMORY_LIMIT} (reserve ${BACKEND_MEMORY_RESERVATION}); \
alloy=${ALLOY_CPU_LIMIT} CPU, ${ALLOY_MEMORY_LIMIT} (reserve ${ALLOY_MEMORY_RESERVATION})"
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
  # Dev EC2 often has manually-started containers with the same fixed names.
  # Remove named containers first so CodeDeploy can recreate them consistently.
  docker rm -f "${DEV_INFRA_DB_CONTAINER}" "${DEV_INFRA_REDIS_CONTAINER}" >/dev/null 2>&1 || true

  compose_up \
    --project-name "${DEV_INFRA_COMPOSE_PROJECT_NAME}" \
    --env-file "${BACKEND_ENV_FILE}" \
    -f "${DEV_INFRA_COMPOSE_FILE}" \
    up -d db redis

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

stop_alloy() {
  if [ -f "${ALLOY_COMPOSE_FILE}" ]; then
    log "stop alloy"
    compose_up --project-name "${ALLOY_COMPOSE_PROJECT_NAME}" -f "${ALLOY_COMPOSE_FILE}" rm -fsv alloy || true
  fi
  docker rm -f alloy 2>/dev/null || true
}

start_alloy() {
  if [ ! -f "${ALLOY_COMPOSE_FILE}" ]; then
    log "alloy compose file not found: ${ALLOY_COMPOSE_FILE} (skip)"
    return 0
  fi

  log "fetch monitoring env from /${ENV_NAME}/tasteam/monitoring"
  fetch_ssm_env "/${ENV_NAME}/tasteam/monitoring" "${MONITORING_ENV_FILE}"
  printf 'ENVIRONMENT=%s\nHOSTNAME=%s\n' "${ENV_NAME}" "$(hostname)" >> "${MONITORING_ENV_FILE}"

  # POSTGRES_DSN 조립 (dev postgres exporter용)
  # prod RDS 메트릭은 shared 모니터링 서버의 Alloy에서 수집
  if [ "${ENV_NAME}" = "dev" ]; then
    local db_user db_pass
    db_user="$(read_env_value "DB_USERNAME")"
    db_pass="$(read_env_value "DB_PASSWORD")"
    printf 'POSTGRES_DSN=postgresql://%s:%s@host.docker.internal:5432/tasteam?sslmode=disable\n' \
      "${db_user}" "${db_pass}" >> "${MONITORING_ENV_FILE}"
  fi

  local alloy_config="${SCRIPT_DIR}/alloy/alloy-app.alloy"
  if [ "${ENV_NAME}" = "dev" ] && [ -f "${SCRIPT_DIR}/alloy/alloy-app-dev.alloy" ]; then
    alloy_config="${SCRIPT_DIR}/alloy/alloy-app-dev.alloy"
  fi

  log "start alloy (config=${alloy_config})"
  ALLOY_CONFIG_FILE="${alloy_config}" \
  compose_up \
    --project-name "${ALLOY_COMPOSE_PROJECT_NAME}" \
    --env-file "${MONITORING_ENV_FILE}" \
    -f "${ALLOY_COMPOSE_FILE}" \
    up -d alloy
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
  upsert_env_value "SPRING_PROFILES_ACTIVE" "${ENV_NAME}"
  validate_required_backend_env
  validate_backend_env
  start_dev_infra
  set_runtime_resource_limits

  local image
  image="${ECR_REGISTRY}/${ECR_REPO_BACKEND}:${IMAGE_TAG}"

  prune_unused_docker_images "before-pull"

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
  stop_alloy
  start_alloy

  prune_unused_docker_images "after-run"
}

health_check() {
  require_cmd aws
  require_cmd curl

  local i
  for i in $(seq 1 "${HEALTH_RETRIES}"); do
    if curl -fsS "${HEALTH_URL}" >/dev/null; then
      log "health check passed"
      mark_cloud_map_instance_healthy
      return 0
    fi
    sleep "${HEALTH_INTERVAL_SECONDS}"
  done

  log "health check failed: ${HEALTH_URL}"
  log "container status: ${CONTAINER_NAME}"
  docker ps -a --filter "name=${CONTAINER_NAME}" || true
  docker logs --tail 200 "${CONTAINER_NAME}" || true
  if [ "${ENV_NAME}" = "dev" ]; then
    docker logs --tail 100 "${DEV_INFRA_DB_CONTAINER}" || true
    docker logs --tail 100 "${DEV_INFRA_REDIS_CONTAINER}" || true
  fi
  return 1
}

load_deploy_env_file

case "${MODE}" in
  stop)
    require_cmd aws
    require_cmd docker
    mark_cloud_map_instance_unhealthy || true
    stop_container
    stop_alloy
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
