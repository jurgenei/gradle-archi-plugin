#!/bin/bash
ARCHI=/Users/cs79en/Applications/Archi.app/Contents/MacOS/Archi
export ARCHI_FILE
export EXPORT_DIR
export PACKAGE_NAME
CMD="$ARCHI -application com.archimatetool.commandline.app -consoleLog -nosplash $*"
$CMD
exit 0
