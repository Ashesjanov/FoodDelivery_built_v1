#!/usr/bin/env bash
# 安装 Manjaro/Arch 开发包并启动本地基础设施。
set -Eeuo pipefail
IFS=$'\n\t'

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
TARGET_USER="${SUDO_USER:-${USER}}"
TARGET_HOME="$(getent passwd "${TARGET_USER}" | cut -d: -f6)"

info() {
  printf '[INFO] %s\n' "$*"
}

warn() {
  printf '[WARN] %s\n' "$*" >&2
}

die() {
  printf '[FAIL] %s\n' "$*" >&2
  exit 1
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

version_at_least() {
  local current="$1"
  local required="$2"
  [[ "$(printf '%s\n%s\n' "${required}" "${current}" | sort -V | head -n 1)" == "${required}" ]]
}

set_env_value() {
  local key="$1"
  local value="$2"
  local env_file="${PROJECT_ROOT}/.env"
  local temporary_file

  temporary_file="$(mktemp "${env_file}.XXXXXX")"
  awk -v key="${key}" -v value="${value}" '
    index($0, key "=") == 1 {
      print key "=" value
      next
    }
    { print }
  ' "${env_file}" > "${temporary_file}"
  mv "${temporary_file}" "${env_file}"
}

if [[ "${EUID}" -eq 0 ]]; then
  die "Run this script as your normal user, not with sudo. The script calls sudo where needed."
fi

if [[ -r /etc/os-release ]]; then
  # shellcheck disable=SC1091
  source /etc/os-release
fi

if [[ "${ID:-}" != "manjaro" && "${ID_LIKE:-}" != *arch* ]]; then
  die "This setup script supports Manjaro/Arch Linux only."
fi

command_exists pacman || die "pacman was not found."
command_exists sudo || die "sudo was not found."
sudo -v

packages=(
  base-devel
  curl
  docker
  docker-buildx
  docker-compose
  git
  jdk21-openjdk
  maven
  openssl
  unzip
  zip
)

install_node=false
if ! command_exists node || ! command_exists npm; then
  install_node=true
else
  node_version="$(node --version | sed 's/^v//')"
  if ! version_at_least "${node_version}" "20"; then
    install_node=true
  fi
fi

if [[ "${install_node}" == true ]]; then
  packages+=(nodejs npm)
else
  info "Reusing the existing Node.js $(node --version) and npm $(npm --version)."
fi

missing_packages=()
for package in "${packages[@]}"; do
  if ! pacman -Q "${package}" >/dev/null 2>&1; then
    missing_packages+=("${package}")
  fi
done

if (( ${#missing_packages[@]} > 0 )); then
  info "Installing required Manjaro packages: ${missing_packages[*]}"
  sudo pacman -Syu --needed --noconfirm "${missing_packages[@]}"
else
  info "All required Manjaro packages are already installed."
fi

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
[[ -x "${JAVA_HOME}/bin/java" ]] || die "Java 21 was not installed at ${JAVA_HOME}."

if command_exists archlinux-java; then
  sudo archlinux-java set java-21-openjdk
fi

runtime_env_dir="${TARGET_HOME}/.config/food-delivery"
runtime_env_file="${runtime_env_dir}/env.sh"
mkdir -p "${runtime_env_dir}"
cat > "${runtime_env_file}" <<'EOF'
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH="${JAVA_HOME}/bin:${PATH}"
EOF
chmod 600 "${runtime_env_file}"

for shell_rc in "${TARGET_HOME}/.zshrc" "${TARGET_HOME}/.bashrc"; do
  marker="# food-delivery development environment"
  if ! grep -Fq "${marker}" "${shell_rc}" 2>/dev/null; then
    printf '\n%s\nsource "$HOME/.config/food-delivery/env.sh"\n' "${marker}" >> "${shell_rc}"
  fi
done

info "Configured JAVA_HOME for Java 21 in the user shell startup files."

sudo systemctl enable --now docker.service

if ! id -nG "${TARGET_USER}" | tr ' ' '\n' | grep -Fxq docker; then
  sudo usermod -aG docker "${TARGET_USER}"
  warn "Added ${TARGET_USER} to the docker group. Run 'exec newgrp docker' in the current terminal, or log out and back in."
fi

"${SCRIPT_DIR}/init-env.sh"

compose_command=()
if sudo docker compose version >/dev/null 2>&1; then
  compose_command=(sudo docker compose)
elif command_exists docker-compose; then
  compose_command=(sudo docker-compose)
else
  die "Docker Compose is unavailable after installation."
fi

# 先显式拉取镜像，让仓库失败比隐式创建容器时更清晰。
info "Pulling MySQL 8, Redis 7, and MinIO images."
if ! "${compose_command[@]}" --project-directory "${PROJECT_ROOT}" pull; then
  warn "Upstream image pull failed. Switching to the DaoCloud registry mirror and retrying."
  set_env_value MYSQL_IMAGE "docker.m.daocloud.io/library/mysql:8.4"
  set_env_value REDIS_IMAGE "docker.m.daocloud.io/library/redis:7.4-alpine"
  set_env_value MINIO_IMAGE "quay.m.daocloud.io/minio/minio:latest"

  if ! "${compose_command[@]}" --project-directory "${PROJECT_ROOT}" pull; then
    die "Unable to pull required images from the upstream registries or DaoCloud mirror. Check network/proxy settings, then rerun this script."
  fi
fi

info "Starting MySQL 8, Redis 7, and MinIO with Docker Compose."
"${compose_command[@]}" --project-directory "${PROJECT_ROOT}" up -d

info "Waiting for container health checks."
for _ in $(seq 1 30); do
  mysql_id="$("${compose_command[@]}" --project-directory "${PROJECT_ROOT}" ps -q mysql)"
  redis_id="$("${compose_command[@]}" --project-directory "${PROJECT_ROOT}" ps -q redis)"
  mysql_health="$(sudo docker inspect --format '{{.State.Health.Status}}' "${mysql_id}" 2>/dev/null || true)"
  redis_health="$(sudo docker inspect --format '{{.State.Health.Status}}' "${redis_id}" 2>/dev/null || true)"
  if [[ "${mysql_health}" == healthy && "${redis_health}" == healthy ]]; then
    break
  fi
  sleep 2
done

if [[ "${mysql_health:-}" != healthy || "${redis_health:-}" != healthy ]]; then
  die "MySQL or Redis did not become healthy within 60 seconds. Inspect logs with: ${compose_command[*]} --project-directory ${PROJECT_ROOT} logs mysql redis"
fi

"${compose_command[@]}" --project-directory "${PROJECT_ROOT}" ps

info "Manjaro development environment is configured."
info "Open a new login shell, then run ${SCRIPT_DIR}/verify-env.sh."
