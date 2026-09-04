@echo off
REM Run the preserved TindaKart Swing application with Java 21.
set "JAVA21=%USERPROFILE%\AppData\Roaming\Cursor\User\globalStorage\pleiades.java-extension-pack-jdk\java\21"
if exist "%JAVA21%\bin\java.exe" (
    set "JAVA_HOME=%JAVA21%"
)
cd /d "%~dp0.."
call gradlew.bat -p legacy run %*
