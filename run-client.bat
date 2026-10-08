@echo off
rem The JavaFX client: run-client.bat [--host H] [--port P] [--nickname N] [--connect] [--join R-0001]
rem (run download-tools once for the JavaFX SDK, and compile.bat)
cd /d "%~dp0"
java --module-path lib\javafx-sdk\lib --add-modules javafx.controls -cp "dist\client.jar;dist\protocol.jar" com.ludot.client.ClientMain %*
