#!/bin/bash
# The JavaFX client: ./run-client.sh [--host H] [--port P] [--nickname N] [--connect] [--join R-0001]
# (run ./download-tools.sh once for the JavaFX SDK, and ./compile.sh)
cd "$(dirname "$0")"
exec java --module-path lib/javafx-sdk/lib --add-modules javafx.controls \
    -cp "dist/client.jar:dist/protocol.jar" com.ludot.client.ClientMain "$@"
