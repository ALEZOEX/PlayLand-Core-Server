@echo off
REM PlayLand Core Server - Download Paper
REM This script downloads Paper JAR for analysis and fork creation

echo.
echo ================================================================
echo   📦 PLAYLAND CORE SERVER - PAPER DOWNLOAD
echo ================================================================
echo   Downloading Paper 1.21.1 for fork creation...
echo ================================================================
echo.

REM Create downloads directory
if not exist "downloads" mkdir "downloads"

REM Download Paper 1.21.1
echo [1/3] Downloading Paper 1.21.1...
echo This may take a few minutes depending on your connection...

powershell -Command "& {Invoke-WebRequest -Uri 'https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/131/downloads/paper-1.21.1-131.jar' -OutFile 'downloads\paper-1.21.1-131.jar'}"

if %ERRORLEVEL% neq 0 (
    echo ❌ FAIL: Could not download Paper JAR
    echo.
    echo Trying alternative download method...

    REM Try with curl if available
    curl -L -o "downloads\paper-1.21.1-131.jar" "https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/131/downloads/paper-1.21.1-131.jar"

    if %ERRORLEVEL% neq 0 (
        echo ❌ FAIL: Alternative download also failed
        echo.
        echo Please download Paper manually:
        echo 1. Go to https://papermc.io/downloads/paper
        echo 2. Download Paper 1.21.1 build 131
        echo 3. Place it in downloads\ folder as paper-1.21.1-131.jar
        pause
        exit /b 1
    )
)

echo ✅ Paper JAR downloaded successfully

REM Verify download
echo.
echo [2/3] Verifying download...
if exist "downloads\paper-1.21.1-131.jar" (
    for %%A in ("downloads\paper-1.21.1-131.jar") do (
        echo ✅ Paper JAR verified (%%~zA bytes)
    )
) else (
    echo ❌ FAIL: Paper JAR not found after download
    pause
    exit /b 1
)

REM Create reference copy
echo.
echo [3/3] Setting up reference files...
copy "downloads\paper-1.21.1-131.jar" "paper-reference.jar" >nul
echo ✅ Reference Paper JAR created

REM Extract some information about Paper
echo.
echo 📊 Paper Information:
echo Version: 1.21.1
echo Build: 131
echo File: paper-1.21.1-131.jar
for %%A in ("downloads\paper-1.21.1-131.jar") do echo Size: %%~zA bytes

echo.
echo ================================================================
echo   🎉 PAPER DOWNLOAD COMPLETED! 🎉
echo ================================================================
echo.
echo   ✅ Paper 1.21.1 build 131 downloaded
echo   ✅ Reference JAR created
echo   ✅ Ready for fork analysis
echo.
echo   📁 Files created:
echo   - downloads\paper-1.21.1-131.jar (original)
echo   - paper-reference.jar (reference copy)
echo.
echo   Next steps for fork creation:
echo   1. Analyze Paper structure and code
echo   2. Create patches based on PlayLand optimizations
echo   3. Apply patches to create PlayLand fork
echo   4. Build revolutionary server
echo.
echo   🔬 You can now examine Paper's code structure
echo   🚀 Ready to create impossible performance fork!
echo ================================================================
echo.

pause
