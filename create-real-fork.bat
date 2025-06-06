@echo off
REM PlayLand Core Server - Create REAL Paper Fork
REM This script creates a proper Paper fork using Git and paperweight

echo.
echo ================================================================
echo   🔧 PLAYLAND CORE SERVER - REAL PAPER FORK CREATION
echo ================================================================
echo   Creating a PROPER Paper fork with Git and paperweight...
echo ================================================================
echo.

REM Check if Git is available
echo [1/8] Checking Git installation...
git --version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Git is required for Paper forks!
    echo Please install Git from https://git-scm.com/
    pause
    exit /b 1
) else (
    echo ✅ Git detected
)

REM Check if Java 21 is available
echo.
echo [2/8] Checking Java 21...
java -version 2>&1 | findstr "21\." >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Java 21 is required!
    echo Please install Java 21
    pause
    exit /b 1
) else (
    echo ✅ Java 21 detected
)

REM Create real fork directory
echo.
echo [3/8] Creating real fork structure...
if exist "playland-real-fork" rmdir /s /q "playland-real-fork"
mkdir "playland-real-fork"
cd "playland-real-fork"

REM Initialize Git repository
echo.
echo [4/8] Initializing Git repository...
git init
git config user.name "PlayLand Team"
git config user.email "team@playland.ru"
echo ✅ Git repository initialized

REM Clone Paper as template (lightweight)
echo.
echo [5/8] Setting up Paper template structure...
echo This creates the proper paperweight structure...

REM Create proper paperweight structure
mkdir "Paper-API"
mkdir "Paper-Server"
mkdir "patches"
mkdir "patches\api"
mkdir "patches\server"

echo ✅ Paper structure created

REM Copy our build system
echo.
echo [6/8] Setting up build system...
copy "..\build.gradle.kts" "build.gradle.kts" >nul
copy "..\settings.gradle.kts" "settings.gradle.kts" >nul
copy "..\gradle.properties" "gradle.properties" >nul

REM Copy our patches
if exist "..\patches" (
    xcopy "..\patches" "patches\" /s /e /q
    echo ✅ Patches copied
)

REM Create gradle wrapper
echo.
echo [7/8] Setting up Gradle wrapper...
echo @echo off > gradlew.bat
echo java -jar gradle\wrapper\gradle-wrapper.jar %%* >> gradlew.bat

REM Create basic Paper fork structure
echo.
echo [8/8] Creating Paper fork files...

REM Create basic API structure
mkdir "Paper-API\src\main\java\org\bukkit"
echo // PlayLand API Extension > "Paper-API\src\main\java\org\bukkit\PlayLandAPI.java"

REM Create basic Server structure  
mkdir "Paper-Server\src\main\java\net\minecraft\server"
echo // PlayLand Server Core > "Paper-Server\src\main\java\net\minecraft\server\PlayLandCore.java"

REM Copy our revolutionary code
if exist "..\playland-server\src" (
    xcopy "..\playland-server\src" "Paper-Server\src\" /s /e /q
    echo ✅ PlayLand code integrated
)

echo.
echo ================================================================
echo   🎉 REAL PAPER FORK STRUCTURE CREATED! 🎉
echo ================================================================
echo.
echo   📁 Real fork structure:
echo   - Git repository initialized
echo   - Paper-API/ (API source)
echo   - Paper-Server/ (Server source)  
echo   - patches/ (Git patches)
echo   - build.gradle.kts (paperweight build)
echo.
echo   🔧 Next steps to make it work:
echo   1. Set up proper Paper upstream
echo   2. Configure paperweight correctly
echo   3. Apply patches to real Paper code
echo   4. Build with Gradle
echo.
echo   ⚠️  This is the foundation - now we need to:
echo   - Download real Paper source code
echo   - Set up paperweight properly
echo   - Create working Git patches
echo.
echo   🚀 Ready for real Paper fork development!
echo ================================================================

cd ..
pause
