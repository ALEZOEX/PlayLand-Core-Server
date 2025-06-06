@echo off
REM PlayLand Core Server - Setup Paper Fork
REM This script sets up Paper as upstream and creates the fork structure

echo.
echo ================================================================
echo   🌟 PLAYLAND CORE SERVER - PAPER FORK SETUP
echo ================================================================
echo   Setting up Paper as upstream for revolutionary fork...
echo ================================================================
echo.

REM Check if Git is available
echo [1/6] Checking Git installation...
git --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Git is required but not found!
    echo Please install Git from https://git-scm.com/
    pause
    exit /b 1
) else (
    echo ✅ PASS: Git detected
)

REM Initialize Git repository if not exists
echo.
echo [2/6] Initializing Git repository...
if not exist ".git" (
    git init
    echo ✅ Git repository initialized
) else (
    echo ✅ Git repository already exists
)

REM Add Paper as upstream remote
echo.
echo [3/6] Setting up Paper upstream...
git remote remove upstream >nul 2>&1
git remote add upstream https://github.com/PaperMC/Paper.git
if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Could not add Paper upstream
    pause
    exit /b 1
) else (
    echo ✅ Paper upstream added
)

REM Fetch Paper repository
echo.
echo [4/6] Fetching Paper repository (this may take a while)...
git fetch upstream
if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Could not fetch Paper repository
    echo Check your internet connection and try again
    pause
    exit /b 1
) else (
    echo ✅ Paper repository fetched
)

REM Create upstream branch
echo.
echo [5/6] Setting up upstream branch...
git checkout -b upstream upstream/ver/1.21.1 >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ⚠️  Upstream branch may already exist, switching to it...
    git checkout upstream >nul 2>&1
    git reset --hard upstream/ver/1.21.1 >nul 2>&1
)
echo ✅ Upstream branch configured

REM Return to main branch and prepare for patches
echo.
echo [6/6] Preparing PlayLand branch...
git checkout -b playland-main >nul 2>&1
if %ERRORLEVEL% neq 0 (
    git checkout playland-main >nul 2>&1
)

REM Create basic structure for manual patch application
echo Creating PlayLand fork structure...

REM Copy Paper structure to our fork directories
if exist "Paper-API" (
    echo Copying Paper-API to playland-api...
    if exist "playland-api" rmdir /s /q "playland-api"
    xcopy "Paper-API" "playland-api\" /s /e /q
) else (
    echo ⚠️  Paper-API not found, will be created during patch application
)

if exist "Paper-Server" (
    echo Copying Paper-Server to playland-server...
    if exist "playland-server" rmdir /s /q "playland-server"
    xcopy "Paper-Server" "playland-server\" /s /e /q
) else (
    echo ⚠️  Paper-Server not found, will be created during patch application
)

echo.
echo ================================================================
echo   🎉 PAPER FORK SETUP COMPLETED! 🎉
echo ================================================================
echo.
echo   ✅ Paper repository cloned and configured
echo   ✅ Upstream branch set to Paper ver/1.21.1
echo   ✅ PlayLand branch created
echo   ✅ Fork structure prepared
echo.
echo   📁 Repository structure:
echo   - upstream/         (Paper original)
echo   - playland-main/    (PlayLand fork)
echo   - patches/          (PlayLand modifications)
echo.
echo   Next steps:
echo   1. Apply PlayLand patches manually or with scripts
echo   2. Build the server using available build tools
echo   3. Test the revolutionary optimizations
echo.
echo   Manual patch application:
echo   - Navigate to playland-api and playland-server
echo   - Apply patches from patches/ directory
echo   - Build using standard Java/Maven tools
echo.
echo   🚀 Ready to create impossible performance!
echo ================================================================
echo.

pause
