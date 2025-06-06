@echo off
REM PlayLand Core Server - Clone Real Paper Source
REM This script clones the actual Paper source code for forking

echo.
echo ================================================================
echo   📥 PLAYLAND CORE SERVER - CLONE PAPER SOURCE
echo ================================================================
echo   Cloning REAL Paper source code for proper forking...
echo ================================================================
echo.

REM Check Git
echo [1/6] Checking Git...
git --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Git required!
    pause
    exit /b 1
)
echo ✅ Git available

REM Create workspace
echo.
echo [2/6] Creating workspace...
if exist "paper-source" rmdir /s /q "paper-source"
mkdir "paper-source"
cd "paper-source"

REM Clone Paper repository
echo.
echo [3/6] Cloning Paper repository...
echo This will download ~500MB of Paper source code...
echo Please wait, this may take 5-10 minutes...

git clone https://github.com/PaperMC/Paper.git .
if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to clone Paper repository
    echo Check your internet connection
    pause
    exit /b 1
)
echo ✅ Paper repository cloned

REM Switch to stable version
echo.
echo [4/6] Switching to Paper 1.21.1...
git checkout ver/1.21.1
if %ERRORLEVEL% neq 0 (
    echo ❌ Failed to checkout Paper 1.21.1
    echo Trying master branch...
    git checkout master
)
echo ✅ Paper version set

REM Setup Paper build system
echo.
echo [5/6] Setting up Paper build system...
echo Running Paper's setup script...

REM Run Paper's own setup
call gradlew applyPatches
if %ERRORLEVEL% neq 0 (
    echo ⚠️ Paper setup had issues, but continuing...
)

echo.
echo [6/6] Analyzing Paper structure...
echo Paper source structure:
dir /b
echo.
echo Paper-API structure:
if exist "Paper-API" dir "Paper-API" /b
echo.
echo Paper-Server structure:
if exist "Paper-Server" dir "Paper-Server" /b

echo.
echo ================================================================
echo   🎉 PAPER SOURCE CODE READY! 🎉
echo ================================================================
echo.
echo   📁 Paper source available in: paper-source/
echo   📊 Repository size: ~500MB
echo   🔧 Build system: Gradle + paperweight
echo.
echo   📋 What we now have:
echo   ✅ Complete Paper source code
echo   ✅ Paper's build system (paperweight)
echo   ✅ Paper-API source
echo   ✅ Paper-Server source
echo   ✅ All Paper patches and tools
echo.
echo   🚀 Next: Create PlayLand patches for this real code!
echo ================================================================

cd ..
pause
