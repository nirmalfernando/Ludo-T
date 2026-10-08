#!/bin/bash
# Starts the LUDO-T server: ./run-server.sh [path/to/server.properties]   (run ./compile.sh first)
cd "$(dirname "$0")"
exec java -cp "dist/server.jar:dist/core.jar:dist/protocol.jar:lib/*" com.ludot.server.ServerMain "$@"
