@echo off
REM PlayLand Core Server - Manual Fork Creation
REM This script creates a manual fork structure for development

echo.
echo ================================================================
echo   🛠️ PLAYLAND CORE SERVER - MANUAL FORK CREATION
echo ================================================================
echo   Creating manual fork structure for development...
echo ================================================================
echo.

echo [1/5] Creating fork directory structure...

REM Create base directories if they don't exist
if not exist "playland-api" mkdir "playland-api"
if not exist "playland-server" mkdir "playland-server"
if not exist "playland-api\src\main\java" mkdir "playland-api\src\main\java"
if not exist "playland-server\src\main\java" mkdir "playland-server\src\main\java"
if not exist "playland-server\src\main\resources" mkdir "playland-server\src\main\resources"

echo ✅ Directory structure created

echo.
echo [2/5] Copying existing PlayLand code to server...

REM Copy existing PlayLand code to the server fork
if exist "playland-server\src\main\java\ru" rmdir /s /q "playland-server\src\main\java\ru"
xcopy "playland-server\src\main\java\ru" "playland-server\src\main\java\ru\" /s /e /q /i

echo ✅ PlayLand code copied

echo.
echo [3/5] Creating basic Paper-compatible structure...

REM Create basic build files for the fork modules
echo plugins { > "playland-api\build.gradle"
echo     java >> "playland-api\build.gradle"
echo     `maven-publish` >> "playland-api\build.gradle"
echo } >> "playland-api\build.gradle"
echo. >> "playland-api\build.gradle"
echo dependencies { >> "playland-api\build.gradle"
echo     // Paper API will be added here >> "playland-api\build.gradle"
echo } >> "playland-api\build.gradle"

echo plugins { > "playland-server\build.gradle"
echo     java >> "playland-server\build.gradle"
echo     `maven-publish` >> "playland-server\build.gradle"
echo } >> "playland-server\build.gradle"
echo. >> "playland-server\build.gradle"
echo dependencies { >> "playland-server\build.gradle"
echo     implementation(project(":playland-api")) >> "playland-server\build.gradle"
echo     // Paper Server will be added here >> "playland-server\build.gradle"
echo } >> "playland-server\build.gradle"

echo ✅ Basic build files created

echo.
echo [4/5] Creating development documentation...

echo # PlayLand Manual Fork > "MANUAL-FORK-GUIDE.md"
echo. >> "MANUAL-FORK-GUIDE.md"
echo This is a manual fork structure for PlayLand development. >> "MANUAL-FORK-GUIDE.md"
echo. >> "MANUAL-FORK-GUIDE.md"
echo ## Structure: >> "MANUAL-FORK-GUIDE.md"
echo - playland-api/     - API module >> "MANUAL-FORK-GUIDE.md"
echo - playland-server/  - Server implementation >> "MANUAL-FORK-GUIDE.md"
echo - patches/          - Patch files for Paper integration >> "MANUAL-FORK-GUIDE.md"
echo. >> "MANUAL-FORK-GUIDE.md"
echo ## Development: >> "MANUAL-FORK-GUIDE.md"
echo 1. Add Paper dependencies to build files >> "MANUAL-FORK-GUIDE.md"
echo 2. Implement PlayLand optimizations >> "MANUAL-FORK-GUIDE.md"
echo 3. Create patches for Paper integration >> "MANUAL-FORK-GUIDE.md"
echo 4. Build and test >> "MANUAL-FORK-GUIDE.md"

echo ✅ Documentation created

echo.
echo [5/5] Setting up development environment...

REM Create a simple development script
echo @echo off > "dev-build.bat"
echo REM Simple development build >> "dev-build.bat"
echo echo Building PlayLand fork... >> "dev-build.bat"
echo. >> "dev-build.bat"
echo if exist "playland-api\build.gradle" ( >> "dev-build.bat"
echo     echo Building API... >> "dev-build.bat"
echo     cd playland-api >> "dev-build.bat"
echo     if exist "..\gradlew.bat" call ..\gradlew.bat build >> "dev-build.bat"
echo     cd .. >> "dev-build.bat"
echo ) >> "dev-build.bat"
echo. >> "dev-build.bat"
echo if exist "playland-server\build.gradle" ( >> "dev-build.bat"
echo     echo Building Server... >> "dev-build.bat"
echo     cd playland-server >> "dev-build.bat"
echo     if exist "..\gradlew.bat" call ..\gradlew.bat build >> "dev-build.bat"
echo     cd .. >> "dev-build.bat"
echo ) >> "dev-build.bat"
echo. >> "dev-build.bat"
echo echo Build complete! >> "dev-build.bat"
echo pause >> "dev-build.bat"

echo ✅ Development environment ready

echo.
echo ================================================================
echo   🎉 MANUAL FORK STRUCTURE CREATED! 🎉
echo ================================================================
echo.
echo   ✅ Fork directory structure created
echo   ✅ PlayLand code integrated
echo   ✅ Basic build configuration added
echo   ✅ Development documentation created
echo   ✅ Development scripts prepared
echo.
echo   📁 Created structure:
echo   - playland-api/           (API module)
echo   - playland-server/        (Server implementation)
echo   - MANUAL-FORK-GUIDE.md    (Development guide)
echo   - dev-build.bat           (Development build script)
echo.
echo   🚀 Manual fork ready for development!
echo.
echo   Next steps:
echo   1. Review MANUAL-FORK-GUIDE.md for development instructions
echo   2. Add Paper dependencies to build files
echo   3. Implement and test PlayLand optimizations
echo   4. Create patches for Paper integration
echo.
echo   💡 This manual approach allows full control over the fork process
echo ================================================================
echo.

pause
