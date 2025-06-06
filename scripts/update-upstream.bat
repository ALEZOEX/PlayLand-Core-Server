@echo off
REM PlayLand Core Server - Update Upstream Script
REM This script updates the upstream Paper dependency to the latest version

echo.
echo ================================================================
echo   PLAYLAND CORE SERVER v2.2.0 - UPSTREAM UPDATER
echo ================================================================
echo   Updating upstream Paper to maintain revolutionary compatibility
echo ================================================================
echo.

echo WARNING: This will update PlayLand to the latest Paper version.
echo This may require updating patches if there are conflicts.
echo.
set /p confirm="Do you want to continue? (y/N): "
if /i not "%confirm%"=="y" (
    echo Update cancelled.
    pause
    exit /b 0
)

echo.
echo [1/6] Backing up current patches...
if exist "patches-backup" rmdir /s /q "patches-backup"
mkdir "patches-backup"
if exist "patches\api" xcopy "patches\api" "patches-backup\api\" /s /e /q
if exist "patches\server" xcopy "patches\server" "patches-backup\server\" /s /e /q
echo ✓ Patches backed up to patches-backup/

echo.
echo [2/6] Cleaning workspace...
call gradlew clean cleanCache
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to clean workspace
    pause
    exit /b 1
)

echo.
echo [3/6] Updating upstream Paper...
call gradlew updateUpstream
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to update upstream
    echo.
    echo This might be due to:
    echo - Network connectivity issues
    echo - Invalid Paper version in gradle.properties
    echo - Upstream repository issues
    echo.
    echo Restoring backup...
    if exist "patches" rmdir /s /q "patches"
    if exist "patches-backup" (
        mkdir "patches"
        xcopy "patches-backup\*" "patches\" /s /e /q
    )
    pause
    exit /b 1
)

echo.
echo [4/6] Attempting to apply existing patches...
call gradlew applyPatches
if %ERRORLEVEL% neq 0 (
    echo.
    echo WARNING: Patches failed to apply cleanly!
    echo This is normal when updating upstream - patches may need updates.
    echo.
    echo [4a/6] Attempting to apply patches with conflicts...
    call gradlew applyPatches --continue
    
    if %ERRORLEVEL% neq 0 (
        echo.
        echo CONFLICT: Some patches have conflicts that need manual resolution.
        echo.
        echo To resolve conflicts:
        echo 1. Navigate to playland-api or playland-server directories
        echo 2. Resolve Git conflicts in the affected files
        echo 3. Run: git add . && git am --continue
        echo 4. Repeat until all patches are applied
        echo 5. Run make-patches.bat to update the patch files
        echo.
        echo Original patches are backed up in patches-backup/
        pause
        exit /b 1
    )
)

echo.
echo [5/6] Testing build with updated upstream...
call gradlew build
if %ERRORLEVEL% neq 0 (
    echo ERROR: Build failed with updated upstream
    echo.
    echo This indicates that the code changes are incompatible with the new Paper version.
    echo You may need to update the PlayLand code to work with the new Paper API.
    echo.
    echo Restoring backup...
    if exist "patches" rmdir /s /q "patches"
    if exist "patches-backup" (
        mkdir "patches"
        xcopy "patches-backup\*" "patches\" /s /e /q
        call gradlew clean setupUpstream applyPatches
    )
    pause
    exit /b 1
)

echo.
echo [6/6] Cleaning up backup...
if exist "patches-backup" rmdir /s /q "patches-backup"

echo.
echo ================================================================
echo   🌟 UPSTREAM UPDATE COMPLETED SUCCESSFULLY! 🌟
echo ================================================================
echo.
echo   ✅ Paper upstream updated to latest version
echo   ✅ PlayLand patches applied successfully
echo   ✅ Build completed without errors
echo.
echo   📊 Current versions:
call gradlew printPaperVersion
call gradlew printMinecraftVersion
echo.
echo   🚀 PlayLand is now running on the latest Paper!
echo   💡 Consider testing thoroughly before deploying to production
echo ================================================================
echo.

pause
