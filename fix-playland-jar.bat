@echo off
REM PlayLand Core Server - Исправление проблем JAR
REM Пересобираем с правильными зависимостями

echo.
echo ================================================================
echo   🔧 ИСПРАВЛЕНИЕ PLAYLAND JAR
echo ================================================================
echo   Пересобираем сервер с правильными зависимостями...
echo ================================================================
echo.

if not exist "paper-source" (
    echo ❌ Paper source не найден!
    pause
    exit /b 1
)

cd paper-source

echo [1/5] Очищаем предыдущую сборку...
if exist "paper-server\build" (
    rmdir /s /q "paper-server\build"
    echo ✅ Предыдущая сборка очищена
)

echo.
echo [2/5] Проверяем Gradle задачи...
gradlew.bat tasks --group="build" | findstr "shadowJar\|reobfJar"

echo.
echo [3/5] Пересобираем с правильной задачей...
echo Используем reobfJar для создания полного JAR с зависимостями...

gradlew.bat :paper-server:reobfJar --no-daemon --console=plain

if %ERRORLEVEL% neq 0 (
    echo ❌ Сборка reobfJar не удалась!
    echo Пробуем альтернативный метод...
    echo.
    echo [4/5] Пробуем shadowJar...
    gradlew.bat :paper-server:shadowJar --no-daemon --console=plain
    
    if %ERRORLEVEL% neq 0 (
        echo ❌ И shadowJar не удался!
        echo Пробуем обычную сборку...
        gradlew.bat :paper-server:build --no-daemon --console=plain
    )
)

echo.
echo [5/5] Проверяем результат...
if exist "paper-server\build\libs\*.jar" (
    echo ✅ JAR файлы созданы:
    dir "paper-server\build\libs\*.jar" /b
    echo.
    
    REM Проверяем размеры
    for %%f in ("paper-server\build\libs\*.jar") do (
        echo 📦 %%~nxf - %%~zf байт
        
        REM Проверяем наличие joptsimple в JAR
        jar -tf "%%f" | findstr "joptsimple" >nul
        if %ERRORLEVEL% equ 0 (
            echo ✅ %%~nxf содержит joptsimple - должен работать!
        ) else (
            echo ❌ %%~nxf НЕ содержит joptsimple - не будет работать
        )
        echo.
    )
) else (
    echo ❌ JAR файлы не созданы!
)

echo.
echo ================================================================
echo   📋 РЕЗУЛЬТАТ ИСПРАВЛЕНИЯ
echo ================================================================
echo.
echo Если проблема не решена, возможные причины:
echo.
echo 1. 🔧 Paper использует специальную систему сборки
echo 2. 📦 Нужен полный Paper build process
echo 3. 🛠️ Возможно нужно использовать Paper toolchain
echo.
echo 💡 Альтернативное решение:
echo   - Скачать готовый Paper JAR
echo   - Добавить наш код как патч
echo   - Использовать Paper build system правильно
echo ================================================================

cd ..
pause
