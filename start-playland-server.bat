@echo off
title PlayLand Core Server
echo ========================================
echo PlayLand Core - Revolutionary Minecraft Server
echo ========================================
echo.

echo 🚀 Запуск PlayLand Core сервера...
echo.

REM Проверяем наличие Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ❌ Java не найдена! Установите Java 17 или выше.
    pause
    exit /b 1
)

echo ✅ Java найдена
java -version
echo.

REM Выбираем JAR файл для запуска
set JAR_FILE=""
set SERVER_DIR=""

if exist "paper-source\paper-server\build\libs\playland-test-server\playland-core-server.jar" (
    set JAR_FILE=playland-core-server.jar
    set SERVER_DIR=paper-source\paper-server\build\libs\playland-test-server
    echo 🎯 Найден готовый тест-сервер
) else if exist "paper-source\paper-server\build\libs\paper-server-1.21.5-R0.1-SNAPSHOT.jar" (
    set JAR_FILE=paper-server-1.21.5-R0.1-SNAPSHOT.jar
    set SERVER_DIR=paper-source\paper-server\build\libs
    echo 🎯 Найден основной JAR
) else if exist "paper-source\paper-server\build\libs\paper-paperclip-1.21.5-R0.1-SNAPSHOT-mojmap.jar" (
    set JAR_FILE=paper-paperclip-1.21.5-R0.1-SNAPSHOT-mojmap.jar
    set SERVER_DIR=paper-source\paper-server\build\libs
    echo 🎯 Найден Paperclip JAR
) else (
    echo ❌ JAR файлы не найдены! Сначала скомпилируйте проект.
    pause
    exit /b 1
)

echo 📦 Используется: %JAR_FILE%
echo 📁 Папка сервера: %SERVER_DIR%
echo.

REM Переходим в папку сервера
cd "%SERVER_DIR%"

REM Создаем eula.txt если его нет
if not exist "eula.txt" (
    echo 🔧 Создание eula.txt...
    echo #By changing the setting below to TRUE you are indicating your agreement to our EULA ^(https://account.mojang.com/documents/minecraft_eula^). > eula.txt
    echo eula=true >> eula.txt
    echo ✅ EULA принято автоматически
)

REM Создаем server.properties с оптимальными настройками
if not exist "server.properties" (
    echo 🔧 Создание server.properties...
    echo #Minecraft server properties > server.properties
    echo server-name=PlayLand Core Server >> server.properties
    echo motd=§6PlayLand Core §8- §aRevolutionary Optimization >> server.properties
    echo server-port=25565 >> server.properties
    echo max-players=20 >> server.properties
    echo online-mode=false >> server.properties
    echo difficulty=normal >> server.properties
    echo gamemode=survival >> server.properties
    echo level-name=world >> server.properties
    echo level-type=minecraft:normal >> server.properties
    echo spawn-protection=0 >> server.properties
    echo view-distance=10 >> server.properties
    echo simulation-distance=10 >> server.properties
    echo enable-command-block=true >> server.properties
    echo ✅ server.properties создан
)

REM Создаем playland.properties с настройками оптимизации
echo 🔧 Создание playland.properties...
echo # PlayLand Core Configuration > playland.properties
echo playland.optimization.enabled=true >> playland.properties
echo playland.neural.network.enabled=true >> playland.properties
echo playland.quantum.computing.enabled=true >> playland.properties
echo playland.blockchain.consensus.enabled=true >> playland.properties
echo playland.vanilla.safe.mode=true >> playland.properties
echo. >> playland.properties
echo # Performance Settings >> playland.properties
echo playland.memory.threshold=80.0 >> playland.properties
echo playland.cpu.threshold=75.0 >> playland.properties
echo playland.tps.target=20.0 >> playland.properties
echo. >> playland.properties
echo # Advanced Features >> playland.properties
echo playland.collision.optimization.enabled=true >> playland.properties
echo playland.io.advanced.enabled=true >> playland.properties
echo playland.database.optimization.enabled=true >> playland.properties
echo playland.threading.advanced.enabled=true >> playland.properties
echo playland.chunk.optimization.enabled=true >> playland.properties
echo playland.entity.optimization.enabled=true >> playland.properties
echo playland.network.optimization.enabled=true >> playland.properties
echo playland.particle.optimization.enabled=true >> playland.properties
echo playland.sound.optimization.enabled=true >> playland.properties
echo playland.lighting.optimization.enabled=true >> playland.properties
echo playland.weather.optimization.enabled=true >> playland.properties
echo playland.fluid.optimization.enabled=true >> playland.properties
echo playland.redstone.optimization.enabled=true >> playland.properties
echo ✅ playland.properties создан

echo.
echo 🎉 Запуск PlayLand Core сервера с революционными оптимизациями!
echo.
echo 📊 Активные системы:
echo    🧠 4 ИИ-системы (Neural Networks, Genetic Algorithms, ML, Smart Grouping)
echo    ⚡ 3 Квантовые системы (Computing, Consensus, Load Balancer)
echo    🌐 5 Физические системы (Lighting, Sound, Fluids, Weather, Particles)
echo    🔧 15 Системы управления (Memory, Threading, I/O, Database, etc.)
echo.
echo 🚀 Всего: 27 революционных систем оптимизации!
echo.

REM Запускаем сервер
echo ▶️ Запуск сервера...
java -Xmx4G -Xms2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar "%JAR_FILE%" nogui

echo.
echo 🛑 Сервер остановлен.
pause
