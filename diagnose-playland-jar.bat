@echo off
REM PlayLand Core Server - Диагностика проблем запуска
REM Проверяем почему JAR не запускается

echo.
echo ================================================================
echo   🔧 ДИАГНОСТИКА PLAYLAND CORE SERVER
echo ================================================================
echo   Проверяем почему сервер не запускается...
echo ================================================================
echo.

REM Проверяем наличие JAR файла
echo [1/8] Проверяем JAR файл...
if not exist "paper-source\paper-server\build\libs\paper-server-1.21.5-R0.1-SNAPSHOT.jar" (
    echo ❌ JAR файл не найден!
    echo Путь: paper-source\paper-server\build\libs\paper-server-1.21.5-R0.1-SNAPSHOT.jar
    pause
    exit /b 1
)

cd paper-source\paper-server\build\libs
echo ✅ JAR файл найден

echo.
echo [2/8] Проверяем размер и целостность JAR...
for %%f in (paper-server-1.21.5-R0.1-SNAPSHOT.jar) do (
    echo 📦 Размер: %%~zf байт
    if %%~zf LSS 20000000 (
        echo ⚠️  JAR слишком маленький! Возможно поврежден.
    ) else (
        echo ✅ Размер нормальный
    )
)

echo.
echo [3/8] Проверяем Java версию...
java -version
echo.

echo [4/8] Проверяем JAR на корректность...
jar -tf paper-server-1.21.5-R0.1-SNAPSHOT.jar | findstr "META-INF/MANIFEST.MF" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ JAR поврежден - нет MANIFEST.MF
) else (
    echo ✅ JAR структура корректна
)

echo.
echo [5/8] Проверяем Main-Class в манифесте...
jar -xf paper-server-1.21.5-R0.1-SNAPSHOT.jar META-INF/MANIFEST.MF
if exist "META-INF\MANIFEST.MF" (
    echo 📄 Содержимое MANIFEST.MF:
    type "META-INF\MANIFEST.MF"
    echo.
    findstr "Main-Class" "META-INF\MANIFEST.MF" >nul
    if %ERRORLEVEL% neq 0 (
        echo ❌ Main-Class не найден в манифесте!
    ) else (
        echo ✅ Main-Class найден
    )
    rmdir /s /q META-INF
) else (
    echo ❌ Не удалось извлечь MANIFEST.MF
)

echo.
echo [6/8] Проверяем наличие основных классов...
jar -tf paper-server-1.21.5-R0.1-SNAPSHOT.jar | findstr "net/minecraft/server/Main.class" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Основной класс Main.class не найден!
) else (
    echo ✅ Main.class найден
)

jar -tf paper-server-1.21.5-R0.1-SNAPSHOT.jar | findstr "net/minecraft/server/MinecraftServer.class" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ MinecraftServer.class не найден!
) else (
    echo ✅ MinecraftServer.class найден
)

echo.
echo [7/8] Проверяем наш PlayLand код...
jar -tf paper-server-1.21.5-R0.1-SNAPSHOT.jar | findstr "ru/playland" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ PlayLand классы не найдены!
) else (
    echo ✅ PlayLand классы найдены
)

echo.
echo [8/8] Пробуем запустить с диагностикой...
echo Попытка запуска с подробным выводом ошибок:
echo.

java -Xms1G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar paper-server-1.21.5-R0.1-SNAPSHOT.jar --nogui

echo.
echo ================================================================
echo   📋 РЕЗУЛЬТАТЫ ДИАГНОСТИКИ
echo ================================================================
echo.
echo Если сервер не запустился, возможные причины:
echo.
echo 1. ❌ Проблемы компиляции:
echo    - Наш код содержит ошибки
echo    - Неправильные импорты
echo    - Конфликты зависимостей
echo.
echo 2. ❌ Проблемы JAR файла:
echo    - Поврежденный манифест
echo    - Отсутствующие классы
echo    - Неправильная структура
echo.
echo 3. ❌ Проблемы Java:
echo    - Неправильная версия Java
echo    - Недостаточно памяти
echo    - Конфликты JVM
echo.
echo 4. ❌ Проблемы PlayLand кода:
echo    - Ошибки в наших классах
echo    - Проблемы инициализации
echo    - Конфликты с Paper кодом
echo.
echo 💡 Для исправления нужно посмотреть на конкретную ошибку!
echo ================================================================

cd ..\..\..\..
pause
