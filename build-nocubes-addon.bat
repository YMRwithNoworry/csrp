@echo off
setlocal
call "%~dp0gradlew.bat" build
if errorlevel 1 exit /b %errorlevel%
call "%~dp0gradlew.bat" -p "%~dp0nocubes-srp-combat-addon" build
exit /b %errorlevel%
