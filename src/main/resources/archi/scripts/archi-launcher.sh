#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARCHI_RUNTIME="${ARCHI_RUNTIME:-$(cd "$SCRIPT_DIR/.." && pwd)}"
ARCHI_APP=""
ARCHI_DEFAULT_RELEASE_TAG="5.10_0"
ARCHI_DEFAULT_VERSION="5.10.0"

log() {
  echo "INFO: $*"
}

warn() {
  echo "WARN: $*" >&2
}

is_ci() {
  [[ "${GITHUB_ACTIONS:-}" == "true" || "${TF_BUILD:-}" == "true" || "${CI:-}" == "true" ]]
}

load_release_defaults() {
  local release_file="$ARCHI_RUNTIME/conf/archi-release.env"
  if [[ -f "$release_file" ]]; then
    # shellcheck disable=SC1090
    source "$release_file"
  fi

  ARCHI_RELEASE_TAG="${ARCHI_RELEASE_TAG:-${ARCHI_DEFAULT_RELEASE_TAG}}"
  ARCHI_VERSION="${ARCHI_VERSION:-${ARCHI_DEFAULT_VERSION}}"
}

auto_install_archi_on_ci() {
  if [[ "${ARCHI_AUTO_INSTALL:-true}" == "false" ]]; then
    log "Skipping Archi auto-install because ARCHI_AUTO_INSTALL=false"
    return
  fi

  if ! is_ci; then
    return
  fi

  local install_dir="${ARCHI_HOME:-$HOME/.local/archi}"
  if [[ -x "$install_dir/Archi" ]]; then
    export ARCHI_HOME="$install_dir"
    log "Using existing Archi installation at $ARCHI_HOME"
    return
  fi

  load_release_defaults
  local download_url="${ARCHI_DOWNLOAD_URL:-https://github.com/archimatetool/archi.io/releases/download/${ARCHI_RELEASE_TAG}/Archi-Linux64-${ARCHI_VERSION}.tgz}"
  local archive
  archive="$(mktemp /tmp/archi-XXXXXX.tgz)"

  log "Auto-installing Archi ${ARCHI_VERSION} from ${download_url}"
  mkdir -p "$install_dir"
  curl -fsSL "$download_url" -o "$archive"
  tar -xzf "$archive" -C "$install_dir" --strip-components=1
  chmod +x "$install_dir/Archi"
  rm -f "$archive"
  export ARCHI_HOME="$install_dir"
}

detect_os() {
  if [[ -f "/.dockerenv" ]]; then
    echo "docker"
    return
  fi

  case "${OSTYPE:-}" in
    darwin*)
      echo "darwin"
      ;;
    linux*|linux-gnu*)
      echo "linux-gnu"
      ;;
    *)
      case "$(uname -s 2>/dev/null || true)" in
        Darwin)
          echo "darwin"
          ;;
        Linux)
          echo "linux-gnu"
          ;;
        *)
          echo "unknown"
          ;;
      esac
      ;;
  esac
}

find_archi_macos() {
  local candidates=(
    "${ARCHI_HOME:-}"
    "$HOME/Applications/Archi.app"
    "/Applications/Archi.app"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -n "$candidate" && -x "$candidate/Contents/MacOS/Archi" ]]; then
      echo "$candidate/Contents/MacOS/Archi"
      return 0
    fi
  done

  return 1
}

find_archi_linux() {
  local candidates=(
    "${ARCHI_HOME:-}"
    "/opt/archi"
    "/usr/local/archi"
    "$HOME/.local/archi"
    "$HOME/.archi"
  )

  for candidate in "${candidates[@]}"; do
    if [[ -n "$candidate" && -x "$candidate/Archi" ]]; then
      echo "$candidate/Archi"
      return 0
    fi
  done

  return 1
}

resolve_archi_executable() {
  local os_name
  os_name="$(detect_os)"

  if [[ "$os_name" == "darwin" ]]; then
    find_archi_macos && return 0
  fi

  if [[ "$os_name" == "linux-gnu" || "$os_name" == "docker" ]]; then
    find_archi_linux && return 0
  fi

  return 1
}

install_plugins() {
  local plugins_dir="$ARCHI_RUNTIME/plugins"
  local installer="$ARCHI_RUNTIME/bin/install-archiplugin.sh"

  if [[ ! -d "$plugins_dir" ]]; then
    warn "Plugin directory not found: $plugins_dir"
    return
  fi

  local plugins=()
  while IFS= read -r plugin_file; do
    plugins+=("$plugin_file")
  done < <(find "$plugins_dir" -maxdepth 1 -type f -name "*.archiplugin" | sort)
  if [[ ${#plugins[@]} -eq 0 ]]; then
    log "No .archiplugin files found, skipping plugin install"
    return
  fi

  if [[ ! -x "$installer" ]]; then
    chmod +x "$installer"
  fi

  log "Installing ${#plugins[@]} Archi plugins"
  ARCHI_HOME="${ARCHI_HOME:-$(dirname "$ARCHI_APP")}" "$installer" "${plugins[@]}"
}

run_archi() {
  local os_name
  os_name="$(detect_os)"

  local cmd=(
    "$ARCHI_APP"
    -application com.archimatetool.commandline.app
    -consoleLog
    -nosplash
    "$@"
  )

  if [[ "$os_name" == "linux-gnu" || "$os_name" == "docker" ]]; then
    if command -v xvfb-run >/dev/null 2>&1; then
      cmd=(xvfb-run -a "${cmd[@]}")
    else
      warn "xvfb-run not found; running without virtual framebuffer"
    fi
  fi

  log "Running Archi commandline"
  "${cmd[@]}"
}

main() {
  auto_install_archi_on_ci

  ARCHI_APP="$(resolve_archi_executable || true)"
  if [[ -z "$ARCHI_APP" ]]; then
    echo "ERROR: Could not resolve Archi executable. Set ARCHI_HOME to your Archi install." >&2
    exit 1
  fi

  export ARCHI_FILE="${ARCHI_FILE:-}"
  export EXPORT_DIR="${EXPORT_DIR:-$ARCHI_RUNTIME/export}"
  export PACKAGE_NAME="${PACKAGE_NAME:-archi-export}"
  export EXPORT_LOG="${EXPORT_LOG:-$EXPORT_DIR/logs}"

  mkdir -p "$EXPORT_DIR" "$EXPORT_LOG"
  mkdir -p "$HOME/Documents/Archi/scripts"

  install_plugins
  run_archi "$@"
}

main "$@"
