#!/bin/bash
# Offline fallback database: a separate H2 server process in PostgreSQL mode on port 9092.
# Use with: db.url=jdbc:h2:tcp://localhost:9092/./ludot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE  db.user=sa  db.password=
cd "$(dirname "$0")"
mkdir -p h2data
exec java -cp "lib/h2-2.2.224.jar" org.h2.tools.Server -tcp -tcpPort 9092 -baseDir ./h2data -ifNotExists
