@echo off
rem Database setup and inspection: run-dbtool.bat reset ^| schema ^| seed ^| demo ^| drop ^| status
cd /d "%~dp0"
java -cp "dist\server.jar;dist\core.jar;dist\protocol.jar;lib\*" com.ludot.server.DbTool %*
