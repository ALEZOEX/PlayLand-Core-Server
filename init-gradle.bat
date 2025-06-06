@echo off
REM PlayLand Core Server - Initialize Gradle Wrapper
REM This script initializes Gradle wrapper for the project

echo.
echo ================================================================
echo   🔧 PLAYLAND CORE SERVER - GRADLE INITIALIZATION
echo ================================================================
echo   Setting up Gradle wrapper for revolutionary build system...
echo ================================================================
echo.

REM Check if Gradle is installed globally
echo [1/4] Checking for Gradle installation...
gradle --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Gradle not found globally. Installing Gradle wrapper...
    
    REM Download Gradle wrapper manually
    echo [2/4] Downloading Gradle wrapper...
    
    REM Create gradle wrapper directory
    if not exist "gradle\wrapper" mkdir "gradle\wrapper"
    
    REM Download gradle-wrapper.jar using PowerShell
    powershell -Command "& {Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'}"
    
    if %ERRORLEVEL% neq 0 (
        echo ❌ Failed to download Gradle wrapper
        echo.
        echo Please install Gradle manually:
        echo 1. Download Gradle from https://gradle.org/releases/
        echo 2. Extract to a directory
        echo 3. Add bin directory to PATH
        echo 4. Run this script again
        pause
        exit /b 1
    )
    
    echo ✅ Gradle wrapper downloaded
) else (
    echo ✅ Gradle found globally
    
    echo [2/4] Initializing Gradle wrapper...
    gradle wrapper --gradle-version 8.5
    
    if %ERRORLEVEL% neq 0 (
        echo ❌ Failed to initialize Gradle wrapper
        pause
        exit /b 1
    )
    
    echo ✅ Gradle wrapper initialized
)

echo.
echo [3/4] Testing Gradle wrapper...
call gradlew --version
if %ERRORLEVEL% neq 0 (
    echo ❌ Gradle wrapper test failed
    pause
    exit /b 1
) else (
    echo ✅ Gradle wrapper working
)

echo.
echo [4/4] Testing basic Gradle tasks...
call gradlew tasks --quiet
if %ERRORLEVEL% neq 0 (
    echo ❌ Basic Gradle tasks failed
    pause
    exit /b 1
) else (
    echo ✅ Basic Gradle tasks working
)

echo.
echo ================================================================
echo   🎉 GRADLE INITIALIZATION COMPLETED! 🎉
echo ================================================================
echo.
echo   ✅ Gradle wrapper installed and configured
echo   ✅ Build system ready for Paper fork creation
echo.
echo   Next steps:
echo   1. Run 'gradlew setupUpstream' to download Paper
echo   2. Run 'gradlew applyPatches' to apply PlayLand patches
echo   3. Run 'gradlew build' to build the server
echo.
echo   Or use the automated scripts:
echo   - build.bat (full automated build)
echo   - scripts\apply-patches.bat (apply patches only)
echo ================================================================
echo.

pause
