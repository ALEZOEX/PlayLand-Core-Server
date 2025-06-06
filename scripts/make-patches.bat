@echo off
REM PlayLand Core Server - Make Patches Script
REM This script creates patches from changes made to the PlayLand fork

echo.
echo ================================================================
echo   PLAYLAND CORE SERVER v2.2.0 - REVOLUTIONARY PATCH CREATOR
echo ================================================================
echo   Creating patches from your revolutionary modifications
echo ================================================================
echo.

REM Check if we're in the right directory
if not exist "playland-server" (
    echo ERROR: playland-server directory not found!
    echo Please run this script from the project root directory.
    echo Make sure you have applied patches first using apply-patches.bat
    pause
    exit /b 1
)

if not exist "playland-api" (
    echo ERROR: playland-api directory not found!
    echo Please run this script from the project root directory.
    echo Make sure you have applied patches first using apply-patches.bat
    pause
    exit /b 1
)

echo [1/3] Creating API patches...
call gradlew rebuildPatches
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to create patches
    echo.
    echo This might be due to:
    echo - No changes made to the source code
    echo - Git repository issues
    echo - Uncommitted changes
    echo.
    echo Make sure you have:
    echo 1. Applied patches first
    echo 2. Made your changes to playland-api or playland-server
    echo 3. Committed your changes in the respective directories
    pause
    exit /b 1
)

echo.
echo [2/3] Validating created patches...
if exist "patches\api\*.patch" (
    echo ✓ API patches created successfully
    for %%f in (patches\api\*.patch) do echo   - %%~nxf
) else (
    echo ℹ No API patches created (no changes detected)
)

if exist "patches\server\*.patch" (
    echo ✓ Server patches created successfully
    for %%f in (patches\server\*.patch) do echo   - %%~nxf
) else (
    echo ℹ No server patches created (no changes detected)
)

echo.
echo [3/3] Testing patch application...
echo Testing if patches can be cleanly applied...

REM Save current patches
if exist "patches-backup" rmdir /s /q "patches-backup"
mkdir "patches-backup"
if exist "patches\api" xcopy "patches\api" "patches-backup\api\" /s /e /q
if exist "patches\server" xcopy "patches\server" "patches-backup\server\" /s /e /q

REM Test clean application
call gradlew clean setupUpstream applyPatches
if %ERRORLEVEL% neq 0 (
    echo.
    echo WARNING: Created patches do not apply cleanly!
    echo This might indicate conflicts or issues with the patches.
    echo Please review and fix the patches before committing.
    echo.
    echo Restoring original patches...
    if exist "patches" rmdir /s /q "patches"
    if exist "patches-backup" (
        mkdir "patches"
        xcopy "patches-backup\*" "patches\" /s /e /q
        rmdir /s /q "patches-backup"
    )
    pause
    exit /b 1
)

REM Clean up backup
if exist "patches-backup" rmdir /s /q "patches-backup"

echo.
echo ================================================================
echo   🌟 PLAYLAND PATCHES CREATED SUCCESSFULLY! 🌟
echo ================================================================
echo.
echo   ✅ Patches validated and tested
echo   📁 Patches saved to: patches/api/ and patches/server/
echo   🔄 Patches can be cleanly applied
echo.
echo   Next steps:
echo   1. Review the created patches
echo   2. Commit the patches to version control
echo   3. Test the full build process
echo.
echo   🚀 Your revolutionary changes are now preserved!
echo ================================================================
echo.

pause
