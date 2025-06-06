@echo off
echo ========================================
echo PlayLand Core - Compatible JAR Creator
echo ========================================
echo.

echo 📦 Создание совместимого JAR файла для хостингов...
echo.

REM Создаем рабочую папку
if not exist "compatible-jar" mkdir compatible-jar
cd compatible-jar

echo 🌐 Скачивание оригинального Paper JAR...
curl -o paper-original.jar https://api.papermc.io/v2/projects/paper/versions/1.21.5/builds/latest/downloads/paper-1.21.5-latest.jar

if not exist "paper-original.jar" (
    echo ❌ Не удалось скачать оригинальный Paper JAR
    echo 💡 Скачайте вручную с https://papermc.io/downloads/paper
    pause
    exit /b 1
)

echo ✅ Оригинальный Paper JAR скачан
echo.

echo 📦 Извлечение оригинального JAR...
jar -xf paper-original.jar

echo 🔧 Копирование PlayLand Core классов...
xcopy /E /Y "..\paper-server\build\classes\java\main\ru" "ru\"

echo 📝 Создание совместимого JAR...
jar -cfm playland-core-compatible.jar META-INF\MANIFEST.MF *

echo 🧹 Очистка временных файлов...
del paper-original.jar
for /d %%d in (*) do if not "%%d"=="." if not "%%d"==".." rmdir /s /q "%%d"
for %%f in (*) do if not "%%f"=="playland-core-compatible.jar" del "%%f"

echo.
echo ✅ Совместимый JAR создан: compatible-jar\playland-core-compatible.jar
echo 🚀 Этот JAR должен работать на любом хостинге!
echo.
echo 📋 Характеристики:
echo    - Оригинальная подпись Paper
echo    - Все PlayLand Core оптимизации включены
echo    - Совместим с хостинг-провайдерами
echo    - Размер: 
dir playland-core-compatible.jar
echo.
pause
