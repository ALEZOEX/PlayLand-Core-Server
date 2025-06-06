@echo off
echo Testing clean Paper 1.21.1 build 131...
echo.

mkdir test-server 2>nul
cd test-server

copy ..\paper-test.jar paper.jar

echo eula=true > eula.txt

echo Testing Paper startup...
java -Xmx2G -Xms1G -jar paper.jar --nogui

pause
