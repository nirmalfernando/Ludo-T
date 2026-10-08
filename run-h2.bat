@echo off
rem Offline fallback database: a separate H2 server process in PostgreSQL mode on port 9092.
cd /d "%~dp0"
if not exist h2data mkdir h2data
java -cp "lib\h2-2.2.224.jar" org.h2.tools.Server -tcp -tcpPort 9092 -baseDir .\h2data -ifNotExists
