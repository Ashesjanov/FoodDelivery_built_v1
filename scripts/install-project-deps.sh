#!/usr/bin/env bash
# 解析 Maven/npm 依赖并验证后端和前端构建。
set -Eeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-${PROJECT_ROOT}/.m2/repository}"
NPM_CACHE_DIR="${NPM_CACHE_DIR:-${PROJECT_ROOT}/.npm-cache}"
DEPENDENCY_TIMEOUT="${DEPENDENCY_TIMEOUT:-900}"
MAVEN_SETTINGS="${PROJECT_ROOT}/.mvn/settings.xml"
MAVEN_SETTINGS_CENTRAL="${PROJECT_ROOT}/.mvn/settings-central.xml"

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

run_maven() {
  local settings_file="$1"
  shift
  timeout "${DEPENDENCY_TIMEOUT}" mvn \
    --batch-mode \
    -s "${settings_file}" \
    -Dmaven.repo.local="${MAVEN_REPO_LOCAL}" \
    -Dmaven.artifact.threads=8 \
    "$@"
}

run_npm() {
  local registry="$1"

  if [[ -f package-lock.json ]]; then
    npm ci --registry="${registry}"
  else
    npm install --registry="${registry}"
  fi
}

mkdir -p "${MAVEN_REPO_LOCAL}" "${NPM_CACHE_DIR}"
export npm_config_cache="${NPM_CACHE_DIR}"

# 先解析后端依赖图，便于在构建前报告缺失构件。
info "Resolving backend Maven dependencies."
(
  cd "${PROJECT_ROOT}/backend"
  run_maven "${MAVEN_SETTINGS}" dependency:resolve -DincludeScope=test
) || {
  warn "Aliyun Maven mirror failed; retrying with Maven Central."
  MAVEN_SETTINGS="${MAVEN_SETTINGS_CENTRAL}"
  (
    cd "${PROJECT_ROOT}/backend"
    run_maven "${MAVEN_SETTINGS}" dependency:resolve -DincludeScope=test
  ) || die "Maven dependency resolution failed or exceeded ${DEPENDENCY_TIMEOUT}s."
}

# 有锁文件时使用 npm ci，保证安装结果可复现。
info "Installing frontend npm dependencies."
(
  cd "${PROJECT_ROOT}/frontend"
  if ! run_npm "https://registry.npmmirror.com"; then
    warn "npmmirror.com failed; retrying with the official npm registry."
    run_npm "https://registry.npmjs.org"
  fi
) || die "npm dependency installation failed."

info "Verifying the backend build."
(
  cd "${PROJECT_ROOT}/backend"
  run_maven "${MAVEN_SETTINGS}" -DskipTests package
) || die "Backend build failed."

info "Verifying the frontend build."
(
  cd "${PROJECT_ROOT}/frontend"
  npm run build
) || die "Frontend build failed."

info "All project dependencies are installed and both builds passed."
