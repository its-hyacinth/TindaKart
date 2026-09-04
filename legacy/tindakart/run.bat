@echo off
REM Project root moved up one folder — forward to Capstone-uno\run.bat
cd /d "%~dp0.."
call run.bat %*
