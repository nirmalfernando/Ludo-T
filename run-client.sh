#!/bin/bash
# Starts a LUDO-T client (a GUI window): ./run-client.sh [--host H] [--port P] [--nickname N] [--connect]
# (run ./download-tools.sh once for the JavaFX SDK, and ./compile.sh)
cd "$(dirname "$0")"
# on macOS, name the app in the menu bar and Dock instead of "java"
dock=()
if [ "$(uname)" = "Darwin" ]; then
    dock=("-Xdock:name=LUDO-T Online")
fi
exec java ${dock[@]+"${dock[@]}"} --module-path lib/javafx-sdk/lib --add-modules javafx.controls \
    -cp "dist/client.jar:dist/protocol.jar" com.ludot.client.ClientMain "$@"
