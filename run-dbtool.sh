#!/bin/bash
# Database setup and inspection: ./run-dbtool.sh reset | schema | seed | demo | drop | status
# "reset" drops, creates and seeds the tables and plays the demo game: the demo's initial state.
cd "$(dirname "$0")"
exec java -cp "dist/server.jar:dist/core.jar:dist/protocol.jar:lib/*" com.ludot.server.DbTool "$@"
