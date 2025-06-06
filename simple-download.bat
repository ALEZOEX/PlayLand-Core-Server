@echo off
echo Downloading Paper 1.21.1...

if not exist "downloads" mkdir "downloads"

echo Using PowerShell to download Paper...
powershell -Command "Invoke-WebRequest -Uri 'https://api.papermc.io/v2/projects/paper/versions/1.21.1/builds/131/downloads/paper-1.21.1-131.jar' -OutFile 'downloads\paper-1.21.1-131.jar'"

if exist "downloads\paper-1.21.1-131.jar" (
    echo Paper downloaded successfully!
    copy "downloads\paper-1.21.1-131.jar" "paper-reference.jar"
    echo Reference copy created.
) else (
    echo Download failed. Please download manually from https://papermc.io/downloads/paper
)

pause
