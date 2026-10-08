@echo off
rem Starts the LUDO-T server: run-server.bat [path\to\server.properties]   (run compile.bat first)
cd /d "%~dp0"
java -cp "dist\server.jar;dist\core.jar;dist\protocol.jar;lib\*" com.ludot.server.ServerMain %*
