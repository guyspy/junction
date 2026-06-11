@echo off
REM JVM CLI Demo Run Script for Windows
REM This script builds and runs the demo without Gradle's progress bar interference

echo Building JVM CLI Demo...
call ..\..\..\gradlew :catenin:examples:jvm-cli-demo:installDist --console=plain

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

echo Starting game...
echo.

REM Run using the generated script (no Gradle interference)
.\build\install\jvm-cli-demo\bin\jvm-cli-demo.bat