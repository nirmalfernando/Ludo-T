@echo off
rem Phase 1 style headless local game: run.bat <seed>   (run compile.bat first)
cd /d "%~dp0"
java -cp out\main\core com.ludot.Main %*
