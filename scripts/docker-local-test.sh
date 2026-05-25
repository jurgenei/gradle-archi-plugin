#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL_SOURCE="${1:-}"

if [[ -z "$MODEL_SOURCE" ]]; then
  echo "Usage: $0 /absolute/path/to/model.archimate"
  exit 1
fi

if [[ ! -f "$MODEL_SOURCE" ]]; then
  echo "ERROR: model file not found: $MODEL_SOURCE"
  exit 1
fi

INPUT_DIR="$ROOT_DIR/build/docker-input"
EXPORT_DIR_HOST="$ROOT_DIR/build/archi-export"
MODEL_NAME="$(basename "$MODEL_SOURCE")"
PACKAGE_NAME="${MODEL_NAME%.archimate}"
TARGET_MODEL="$INPUT_DIR/model.archimate"

mkdir -p "$INPUT_DIR"
rm -rf "$EXPORT_DIR_HOST"
cp "$MODEL_SOURCE" "$TARGET_MODEL"

export ARCHI_FILE="/workspace/build/docker-input/model.archimate"
export EXPORT_DIR="/workspace/build/archi-export"
export EXPORT_LOG="/workspace/build/archi-export/logs"
export PACKAGE_NAME

cd "$ROOT_DIR"

if [[ "${SKIP_DOCKER_BUILD:-0}" != "1" ]]; then
  docker compose build archi
fi
docker compose run --rm archi

XML_EXPORT="$EXPORT_DIR_HOST/$PACKAGE_NAME.export.xml"
XLSX_EXPORT="$EXPORT_DIR_HOST/$PACKAGE_NAME.xlsx"
PDF_DIR="$EXPORT_DIR_HOST/pdf"

if [[ ! -s "$XML_EXPORT" ]]; then
  echo "ERROR: missing XML export: $XML_EXPORT"
  exit 1
fi

if [[ ! -s "$XLSX_EXPORT" ]]; then
  echo "ERROR: missing XLSX export: $XLSX_EXPORT"
  exit 1
fi

if ! find "$PDF_DIR" -maxdepth 1 -type f -name "*.pdf" | grep -q .; then
  echo "ERROR: no PDF exports found in: $PDF_DIR"
  exit 1
fi

echo "Docker export test passed"
echo "XML:  $XML_EXPORT"
echo "XLSX: $XLSX_EXPORT"
echo "PDFs: $PDF_DIR"

