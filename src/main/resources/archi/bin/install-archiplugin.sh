#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -lt 1 ]]; then
  echo "Usage: $0 plugin1.archiplugin [plugin2.archiplugin ...]"
  exit 1
fi

: "${ARCHI_HOME:=/opt/archi}"
PROFILE="SDKProfile"
ARCHI_RESOURCES="${HOME}/.archi"
DROPINS="${ARCHI_RESOURCES}/dropins"

JAVA="${ARCHI_HOME}/jre/bin/java"
LAUNCHER="$(find "$ARCHI_HOME" -name "org.eclipse.equinox.launcher_*.jar" | head -n 1)"
TMP_REPO="$(mktemp -d /tmp/archi-p2-repo-XXXXXX)"

mkdir -p "$DROPINS"

is_p2_archiplugin() {
  local plugin="$1"
  unzip -l "$plugin" | grep -qE "(^|/)content\.(xml|jar)$"
}

P2_PLUGINS=()
DROPINS_PLUGINS=()

for plugin in "$@"; do
  if [[ ! -f "$plugin" ]]; then
    echo "ERROR: file not found: $plugin"
    exit 1
  fi

  if is_p2_archiplugin "$plugin"; then
    unzip -oq "$plugin" -d "$TMP_REPO"
    P2_PLUGINS+=("$plugin")
  else
    DROPINS_PLUGINS+=("$plugin")
  fi
done

if [[ "${#P2_PLUGINS[@]}" -gt 0 ]]; then
  if [[ ! -x "$JAVA" || -z "$LAUNCHER" ]]; then
    echo "ERROR: missing Archi p2 tooling in ARCHI_HOME=$ARCHI_HOME"
    exit 1
  fi

  FEATURES=$("$JAVA" -jar "$LAUNCHER" \
    -application org.eclipse.equinox.p2.director \
    -repository "file:$TMP_REPO" \
    -list \
    | awk '/feature\.group/ {print $1}' \
    | paste -sd ',' -)

  if [[ -z "$FEATURES" ]]; then
    echo "ERROR: no feature.group IUs found in p2 repository"
    exit 1
  fi

  "$JAVA" -jar "$LAUNCHER" \
    -application org.eclipse.equinox.p2.director \
    -repository "file:$TMP_REPO" \
    -installIU "$FEATURES" \
    -destination "$ARCHI_HOME" \
    -profile "$PROFILE" \
    -profileProperties org.eclipse.update.install.features=true \
    -roaming
fi

if [[ "${#DROPINS_PLUGINS[@]}" -gt 0 ]]; then
  for plugin in "${DROPINS_PLUGINS[@]}"; do
    unzip -oq "$plugin" -d "$DROPINS"
  done
  rm -f "$DROPINS/archi-plugin"
fi

rm -rf "$TMP_REPO"

echo "Installed Archi plugins successfully"

