@echo off
echo ========================================
echo PlayLand Core - Simple Server Start
echo ========================================
echo.

echo 🚀 Запуск PlayLand Core сервера...
echo.

REM Переходим в папку с JAR файлами
cd paper-server\build\libs

echo 📦 Доступные JAR файлы:
dir *.jar

echo.
echo 🎯 Попытка запуска разных версий JAR...
echo.

REM Пробуем разные JAR файлы
if exist "paper-bundler-1.21.5-R0.1-SNAPSHOT-mojmap.jar" (
    echo 📦 Запуск Bundler версии...
    java -Xmx4G -Xms2G -jar paper-bundler-1.21.5-R0.1-SNAPSHOT-mojmap.jar nogui
    if %ERRORLEVEL% equ 0 goto success
)

if exist "paper-paperclip-1.21.5-R0.1-SNAPSHOT-mojmap.jar" (
    echo 📦 Запуск Paperclip версии...
    java -Xmx4G -Xms2G -jar paper-paperclip-1.21.5-R0.1-SNAPSHOT-mojmap.jar nogui
    if %ERRORLEVEL% equ 0 goto success
)

if exist "paper-server-1.21.5-R0.1-SNAPSHOT.jar" (
    echo 📦 Запуск основной версии...
    java -Xmx4G -Xms2G -jar paper-server-1.21.5-R0.1-SNAPSHOT.jar nogui
    if %ERRORLEVEL% equ 0 goto success
)

echo ❌ Не удалось запустить ни один JAR файл
echo 💡 Возможные причины:
echo    - Отсутствуют зависимости
echo    - Неправильная версия Java
echo    - Поврежденные JAR файлы
echo.
echo 🔧 Попробуйте:
echo    1. Установить Java 17+
echo    2. Пересобрать проект: gradlew build
echo    3. Скачать оригинальный Paper JAR
goto end

:success
echo ✅ Сервер запущен успешно!

:end
pause
