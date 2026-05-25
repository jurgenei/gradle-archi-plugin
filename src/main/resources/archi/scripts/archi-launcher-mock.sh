#!/bin/sh
set -eu

input="${ARCHI_FILE:-}"
export_dir="${EXPORT_DIR:-$(pwd)/build/archi-export}"
package_name="${PACKAGE_NAME:-archi-export}"
output="${ARCHI_OUTPUT_FILE:-$export_dir/$package_name.export.xml}"
pdf_dir="$export_dir/pdf"
excel_file="$export_dir/$package_name.xlsx"

mkdir -p "$export_dir" "$pdf_dir" "$(dirname "$output")"

cat > "$output" <<'EOF'
<model xmlns="http://www.opengroup.org/xsd/archimate/3.0/">
  <name>Mock Export</name>
</model>
EOF

echo "mock-pdf" > "$pdf_dir/mock-view.pdf"
echo "mock-xlsx" > "$excel_file"

echo "Exported Archi model to $output"
echo "Exported mock PDF to $pdf_dir/mock-view.pdf"
echo "Exported mock XLSX to $excel_file"

if [ -n "$input" ]; then
  echo "Model loaded by script from $input"
fi

