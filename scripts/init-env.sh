#!/usr/bin/env bash
# 创建包含唯一开发密钥的本地 `.env`；已有文件保持不变。
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${PROJECT_ROOT}/.env"
ENV_EXAMPLE="${PROJECT_ROOT}/.env.example"

info() {
  printf '[INFO] %s\n' "$*"
}

die() {
  printf '[FAIL] %s\n' "$*" >&2
  exit 1
}

random_hex() {
  local bytes="${1:-24}"
  openssl rand -hex "${bytes}"
}

set_env_value() {
  local key="$1"
  local value="$2"
  local temporary_file

  temporary_file="$(mktemp "${ENV_FILE}.XXXXXX")"
  awk -v key="${key}" -v value="${value}" '
    index($0, key "=") == 1 {
      print key "=" value
      next
    }
    { print }
  ' "${ENV_FILE}" > "${temporary_file}"
  mv "${temporary_file}" "${ENV_FILE}"
}

command -v openssl >/dev/null 2>&1 || die "openssl is required to generate local secrets."

if [[ -f "${ENV_FILE}" ]]; then
  info ".env already exists; leaving it unchanged."
  exit 0
fi

[[ -f "${ENV_EXAMPLE}" ]] || die ".env.example is missing."
cp "${ENV_EXAMPLE}" "${ENV_FILE}"
chmod 600 "${ENV_FILE}"

set_env_value MYSQL_PASSWORD "$(random_hex 18)"
set_env_value MYSQL_ROOT_PASSWORD "$(random_hex 24)"
set_env_value REDIS_PASSWORD "$(random_hex 24)"
set_env_value MINIO_ROOT_PASSWORD "$(random_hex 24)"
set_env_value JWT_SECRET "$(openssl rand -base64 48 | tr -d '\n')"

info "Created ${ENV_FILE} with unique local development secrets."
