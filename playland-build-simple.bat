@echo off
REM PlayLand Core Server - Simple Build Master
REM Systematic JAR compilation fix

echo.
echo ================================================================
echo   PlayLand Core Server - Build Master
echo ================================================================
echo   Fixing JAR compilation systematically...
echo ================================================================
echo.

if not exist "paper-source" (
    echo ERROR: Paper source not found!
    echo Please run clone-paper-source.bat first
    pause
    exit /b 1
)

cd paper-source

echo [STEP 1/5] System Diagnostics
echo ================================================================
echo.

echo Checking Java version...
java -version
echo.

echo Checking available Paper tasks...
gradlew.bat tasks --all | findstr -i "jar bundle paperclip mojmap"
echo.

echo Checking PlayLand code location...
if exist "paper-server\src\main\java\ru\playland" (
    echo SUCCESS: PlayLand code found in paper-server
    dir "paper-server\src\main\java\ru\playland" /b
) else (
    echo ERROR: PlayLand code NOT found!
)
echo.

echo [STEP 2/5] Clean Previous Builds
echo ================================================================
gradlew.bat clean --no-daemon --console=plain
echo Clean completed.
echo.

echo [STEP 3/5] Testing Paper Build Methods
echo ================================================================
echo.

echo METHOD 1: createMojmapPaperclipJar
echo ----------------------------------------------------------------
gradlew.bat createMojmapPaperclipJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo SUCCESS: createMojmapPaperclipJar worked!
    goto check_results
)

echo METHOD 1 failed, trying METHOD 2...
echo.

echo METHOD 2: createReobfPaperclipJar
echo ----------------------------------------------------------------
gradlew.bat createReobfPaperclipJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo SUCCESS: createReobfPaperclipJar worked!
    goto check_results
)

echo METHOD 2 failed, trying METHOD 3...
echo.

echo METHOD 3: createMojmapBundlerJar
echo ----------------------------------------------------------------
gradlew.bat createMojmapBundlerJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo SUCCESS: createMojmapBundlerJar worked!
    goto check_results
)

echo METHOD 3 failed, trying METHOD 4...
echo.

echo METHOD 4: reobfJar with debug
echo ----------------------------------------------------------------
gradlew.bat :paper-server:reobfJar -Ppaperweight.debug=true --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo SUCCESS: reobfJar with debug worked!
    goto check_results
)

echo METHOD 4 failed, trying METHOD 5...
echo.

echo METHOD 5: Standard build
echo ----------------------------------------------------------------
gradlew.bat build --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo SUCCESS: Standard build worked!
    goto check_results
)

echo All build methods failed!
goto build_failed

:check_results
echo.
echo [STEP 4/5] Checking Resulting JAR Files
echo ================================================================
echo.

echo Searching for JAR files...
for /r . %%f in (*.jar) do (
    if %%~zf GTR 10000000 (
        echo.
        echo Found JAR: %%~nxf
        echo Path: %%~dpf
        echo Size: %%~zf bytes
        
        REM Check for joptsimple
        jar -tf "%%f" | findstr "joptsimple" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo Contains joptsimple: YES
        ) else (
            echo Contains joptsimple: NO
        )
        
        REM Check for PlayLand code
        jar -tf "%%f" | findstr "ru/playland" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo Contains PlayLand: YES
        ) else (
            echo Contains PlayLand: NO
        )
        
        REM Check size
        if %%~zf GTR 40000000 (
            echo Status: LARGE JAR - Good candidate!
        ) else if %%~zf GTR 25000000 (
            echo Status: Medium size - Possible candidate
        ) else (
            echo Status: Too small
        )
    )
)

echo.
echo [STEP 5/5] Testing Server Launch
echo ================================================================
echo.

REM Find the largest JAR file
set LARGEST_JAR=
set LARGEST_SIZE=0

for /r . %%f in (*.jar) do (
    if %%~zf GTR !LARGEST_SIZE! (
        set LARGEST_JAR=%%f
        set LARGEST_SIZE=%%~zf
    )
)

if defined LARGEST_JAR (
    echo Testing largest JAR: !LARGEST_JAR!
    echo Size: !LARGEST_SIZE! bytes
    echo.
    
    REM Create test directory
    if not exist "test-launch" mkdir "test-launch"
    copy "!LARGEST_JAR!" "test-launch\playland-test.jar" >nul
    cd test-launch
    
    echo eula=true > eula.txt
    
    echo Attempting server launch test...
    timeout /t 3 >nul
    
    java -Xms1G -Xmx2G -jar playland-test.jar nogui --help
    
    if !ERRORLEVEL! equ 0 (
        echo SUCCESS: JAR launches without dependency errors!
    ) else (
        echo FAILED: JAR has dependency issues
    )
    
    cd ..
) else (
    echo No suitable JAR found for testing
)

goto end

:build_failed
echo.
echo Build Failed - Diagnostics
echo ================================================================
echo.
echo Possible issues:
echo 1. Java version incompatibility
echo 2. Paper build system changes
echo 3. Missing dependencies
echo 4. PlayLand code integration issues
echo.
echo Recommendations:
echo - Check Java version (needs 21+)
echo - Update Paper source
echo - Check build errors above
echo - Consider using ready Paper JAR + plugin approach

:end
echo.
echo ================================================================
echo   Build Process Complete
echo ================================================================
cd ..
pause
