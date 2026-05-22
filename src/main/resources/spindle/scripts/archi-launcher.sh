#!/bin/sh
set -eu

input=""
output=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --loadModel)
      input="$2"
      shift 2
      ;;
    --xmlexchange.export)
      output="$2"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done

if [ -z "$output" ]; then
  echo "Missing --xmlexchange.export argument" >&2
  exit 1
fi

mkdir -p "$(dirname "$output")"

cat > "$output" <<'EOF'
<model xmlns="http://www.opengroup.org/xsd/archimate/3.0/">
  <name>Mock Export</name>
</model>
EOF

echo "Exported Archi model to $output"
if [ -n "$input" ]; then
  echo "Loaded model from $input"
fi

