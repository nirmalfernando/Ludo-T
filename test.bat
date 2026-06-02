@echo off
java -cp "out\production;out\test;lib\*" org.junit.platform.console.ConsoleLauncher --scan-class-path
