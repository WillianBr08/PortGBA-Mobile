#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
if ! python3 -c "import tkinter" >/dev/null 2>&1 && [ -d "$PROJECT_DIR/tools/runtime/usr/lib" ]; then
    export LD_LIBRARY_PATH="$PROJECT_DIR/tools/runtime/usr/lib${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
    export TCL_LIBRARY="$PROJECT_DIR/tools/runtime/usr/lib/tcl8.6"
    export TK_LIBRARY="$PROJECT_DIR/tools/runtime/usr/lib/tk8.6"
fi
exec python3 "$PROJECT_DIR/tools/portar_gba.py" "$@"
