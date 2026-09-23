#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"

failures=0

ok() {
  printf '[OK]   %s\n' "$*"
}

warn() {
  printf '[WARN] %s\n' "$*"
}

fail() {
  printf '[FAIL] %s\n' "$*" >&2
  failures=$((failures + 1))
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

version_at_least() {
  local current="$1"
  local required="$2"
  [[ "$(printf '%s\n%s\n' "${required}" "${current}" | sort -V | head -n 1)" == "${required}" ]]
}

if command_exists java; then
  java_spec="$(java -XshowSettings:properties -version 2>&1 | awk -F= '/java.specification.version/ {gsub(/[[:space:]]/, "", $2); print $2; exit}')"
  if [[ "${java_spec}" == "21" ]]; then
    ok "Java ${java_spec}"
  else
    fail "Java 21 is required; found ${java_spec:-unknown}. Run scripts/setup-manjaro.sh."
  fi
else
  fail "Java is missing. Run scripts/setup-manjaro.sh."
fi

if command_exists mvn; then
  maven_version="$(mvn -version 2>/dev/null | awk 'NR == 1 {print $3}')"
  ok "Maven ${maven_version:-installed}"
else
  fail "Maven is missing. Run scripts/setup-manjaro.sh."
fi

if command_exists node && command_exists npm; then
  node_version="$(node --version | sed 's/^v//')"
  if version_at_least "${node_version}" "20"; then
    ok "Node.js ${node_version}, npm $(npm --version)"
  else
    fail "Node.js 20 or newer is required; found ${node_version}."
  fi
else
  fail "Node.js and npm are required. Run scripts/setup-manjaro.sh."
fi

docker_command=()
compose_command=()
if command_exists docker; then
  docker_access_error=""
  if docker info >/dev/null 2>&1; then
    docker_command=(docker)
  elif sudo -n docker info >/dev/null 2>&1; then
    docker_command=(sudo docker)
  else
    docker_access_error="$(docker info 2>&1 >/dev/null || true)"
  fi

  if docker compose version >/dev/null 2>&1; then
    compose_command=(docker compose)
    ok "Docker Compose plugin is available"
  elif command_exists docker-compose; then
    compose_command=(docker-compose)
    ok "Docker Compose is available"
  else
    fail "Docker Compose is missing. Run scripts/setup-manjaro.sh."
  fi

  if (( ${#docker_command[@]} > 0 )); then
    ok "Docker daemon is available"
  elif [[ "${docker_access_error}" == *"permission denied"* ]]; then
    current_user="${SUDO_USER:-${USER}}"
    if getent group docker | cut -d: -f4 | tr ',' '\n' | grep -Fxq "${current_user}"; then
      fail "Docker group already contains ${current_user}, but this terminal started before group membership changed. Run: exec newgrp docker"
    else
      fail "Docker socket permission denied and ${current_user} is not in the docker group. Run scripts/setup-manjaro.sh."
    fi
  elif [[ "${docker_access_error}" == *"Cannot connect"* || "${docker_access_error}" == *"docker daemon"* ]]; then
    fail "Docker daemon is not running. Start it with: sudo systemctl enable --now docker.service"
  else
    fail "Docker daemon is unavailable: ${docker_access_error:-unknown error}"
  fi
else
  fail "Docker is missing. Run scripts/setup-manjaro.sh."
fi

if [[ -f "${PROJECT_ROOT}/.env" ]]; then
  ok ".env exists"
  set -a
  # shellcheck disable=SC1091
  source "${PROJECT_ROOT}/.env"
  set +a
else
  warn ".env is missing; run scripts/init-env.sh."
fi

if (( ${#compose_command[@]} > 0 )) && [[ -f "${PROJECT_ROOT}/.env" ]]; then
  if (cd "${PROJECT_ROOT}" && "${compose_command[@]}" config --quiet); then
    ok "docker-compose.yml is valid"
  else
    fail "docker-compose.yml or .env contains invalid configuration."
  fi

  if (( ${#docker_command[@]} > 0 )); then
    for service in mysql redis minio; do
      service_id="$(cd "${PROJECT_ROOT}" && "${compose_command[@]}" ps -q "${service}" 2>/dev/null || true)"
      if [[ -n "${service_id}" ]]; then
        state="$("${docker_command[@]}" inspect --format '{{.State.Status}}' "${service_id}" 2>/dev/null || true)"
        if [[ "${state}" == running ]]; then
          ok "${service} container is running"
        else
          fail "${service} container exists but is ${state:-unknown}."
        fi
      else
        warn "${service} container is not created; run docker compose up -d."
      fi
    done
  fi

  if command_exists curl; then
    if curl -fsS "http://127.0.0.1:${MINIO_API_PORT:-9000}/minio/health/live" >/dev/null 2>&1; then
      ok "MinIO API is responding"
    else
      warn "MinIO API is not responding on port ${MINIO_API_PORT:-9000}."
    fi
  fi
fi

if (( failures > 0 )); then
  printf '\nEnvironment verification failed with %d error(s).\n' "${failures}" >&2
  exit 1
fi

printf '\nEnvironment verification passed.\n'
