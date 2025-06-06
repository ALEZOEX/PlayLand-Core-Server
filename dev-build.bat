@echo off 
REM Simple development build 
echo Building PlayLand fork... 
 
if exist "playland-api\build.gradle" ( 
    echo Building API... 
    cd playland-api 
    if exist "..\gradlew.bat" call ..\gradlew.bat build 
    cd .. 
) 
 
if exist "playland-server\build.gradle" ( 
    echo Building Server... 
    cd playland-server 
    if exist "..\gradlew.bat" call ..\gradlew.bat build 
    cd .. 
) 
 
echo Build complete! 
pause 
