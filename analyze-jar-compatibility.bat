@echo off
echo ========================================
echo PlayLand Core - JAR Compatibility Analyzer
echo ========================================
echo.

set JAR_FILE=paper-source\paper-server\build\libs\paper-server-1.21.5-R0.1-SNAPSHOT.jar

echo 🔍 Анализ JAR файла: %JAR_FILE%
echo.

echo 📋 Проверка MANIFEST.MF...
jar -tf "%JAR_FILE%" | findstr "META-INF/MANIFEST.MF"
echo.

echo 📋 Содержимое MANIFEST.MF:
jar -xf "%JAR_FILE%" META-INF/MANIFEST.MF
type META-INF\MANIFEST.MF
echo.

echo 🔍 Проверка основных классов Minecraft сервера...
echo.

echo ✅ Проверка Main класса:
jar -tf "%JAR_FILE%" | findstr "org/bukkit/craftbukkit/Main.class"
if %ERRORLEVEL% equ 0 (
    echo    ✅ Main класс найден
) else (
    echo    ❌ Main класс НЕ найден
)

echo ✅ Проверка CraftServer:
jar -tf "%JAR_FILE%" | findstr "org/bukkit/craftbukkit/CraftServer.class"
if %ERRORLEVEL% equ 0 (
    echo    ✅ CraftServer найден
) else (
    echo    ❌ CraftServer НЕ найден
)

echo ✅ Проверка Paper классов:
jar -tf "%JAR_FILE%" | findstr "io/papermc" | head -3
if %ERRORLEVEL% equ 0 (
    echo    ✅ Paper классы найдены
) else (
    echo    ❌ Paper классы НЕ найдены
)

echo ✅ Проверка PlayLand классов:
jar -tf "%JAR_FILE%" | findstr "ru/playland" | head -5
if %ERRORLEVEL% equ 0 (
    echo    ✅ PlayLand классы найдены
) else (
    echo    ❌ PlayLand классы НЕ найдены
)

echo.
echo 📊 Размер JAR файла:
dir "%JAR_FILE%"

echo.
echo 🔍 Проверка версии:
jar -tf "%JAR_FILE%" | findstr "version.json"

echo.
echo 📋 Структура JAR:
jar -tf "%JAR_FILE%" | findstr "^[^/]*/$" | head -10

echo.
echo 🎯 РЕКОМЕНДАЦИИ:
echo.
if exist "META-INF\MANIFEST.MF" (
    findstr "Main-Class" META-INF\MANIFEST.MF >nul
    if %ERRORLEVEL% equ 0 (
        echo ✅ Main-Class указан правильно
    ) else (
        echo ❌ Main-Class отсутствует или неправильный
    )
    
    findstr "Paper" META-INF\MANIFEST.MF >nul
    if %ERRORLEVEL% equ 0 (
        echo ✅ Paper брендинг присутствует
    ) else (
        echo ❌ Paper брендинг отсутствует
    )
)

echo.
echo 💡 Если хостинг не принимает JAR:
echo    1. Используйте оригинальный Paper JAR
echo    2. Добавьте PlayLand классы как плагин
echo    3. Или используйте скрипт create-compatible-jar.bat
echo.

REM Очистка
if exist "META-INF" rmdir /s /q "META-INF"

pause
