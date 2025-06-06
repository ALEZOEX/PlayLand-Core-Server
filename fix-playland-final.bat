@echo off
REM PlayLand Core Server - Финальное исправление
REM Используем правильную Paper систему сборки

echo.
echo ================================================================
echo   🔧 ФИНАЛЬНОЕ ИСПРАВЛЕНИЕ PLAYLAND
echo ================================================================
echo   Используем современную Paper систему сборки...
echo ================================================================
echo.

cd paper-source

echo [1/6] Очищаем сборку...
gradlew.bat clean --no-daemon

echo.
echo [2/6] Проверяем доступные задачи Paper...
gradlew.bat tasks | findstr -i "jar\|bundle\|mojmap"

echo.
echo [3/6] Собираем с mojmap (современный Paper)...
gradlew.bat :paper-server:mojangMappedServerJar --no-daemon --console=plain

if %ERRORLEVEL% neq 0 (
    echo ❌ mojangMappedServerJar не удался!
    echo.
    echo [4/6] Пробуем createMojmapBundlerJar...
    gradlew.bat :paper-server:createMojmapBundlerJar --no-daemon --console=plain
    
    if %ERRORLEVEL% neq 0 (
        echo ❌ createMojmapBundlerJar не удался!
        echo.
        echo [5/6] Пробуем createMojmapPaperclipJar...
        gradlew.bat :paper-server:createMojmapPaperclipJar --no-daemon --console=plain
        
        if %ERRORLEVEL% neq 0 (
            echo ❌ Все специальные задачи не удались!
            echo Пробуем обычную сборку...
            gradlew.bat build --no-daemon --console=plain
        )
    )
)

echo.
echo [6/6] Проверяем все созданные JAR файлы...

echo.
echo 📦 JAR файлы в paper-server/build/libs/:
if exist "paper-server\build\libs" (
    dir "paper-server\build\libs\*.jar" /b 2>nul
    echo.
    
    for %%f in ("paper-server\build\libs\*.jar") do (
        echo 🔍 Анализируем %%~nxf:
        echo    Размер: %%~zf байт
        
        REM Проверяем joptsimple
        jar -tf "%%f" | findstr "joptsimple" >nul
        if %ERRORLEVEL% equ 0 (
            echo    ✅ Содержит joptsimple
        ) else (
            echo    ❌ НЕ содержит joptsimple
        )
        
        REM Проверяем PlayLand код
        jar -tf "%%f" | findstr "ru/playland" >nul
        if %ERRORLEVEL% equ 0 (
            echo    ✅ Содержит PlayLand код
        ) else (
            echo    ❌ НЕ содержит PlayLand код
        )
        
        REM Проверяем размер (должен быть больше 40MB для полного JAR)
        if %%~zf GTR 40000000 (
            echo    ✅ Полный JAR (%%~zf байт)
            echo    🎯 ЭТОТ JAR ДОЛЖЕН РАБОТАТЬ!
        ) else (
            echo    ⚠️  Возможно неполный JAR (%%~zf байт)
        )
        echo.
    )
) else (
    echo ❌ Папка build/libs не найдена
)

echo.
echo 📦 JAR файлы в других местах:
if exist "paper-server\build" (
    for /r "paper-server\build" %%f in (*.jar) do (
        echo 📄 %%~nxf в %%~dpf
        echo    Размер: %%~zf байт
        if %%~zf GTR 40000000 (
            echo    🎯 Потенциально рабочий JAR!
        )
        echo.
    )
)

echo.
echo ================================================================
echo   📋 ИНСТРУКЦИИ ПО ЗАПУСКУ
echo ================================================================
echo.
echo 1. 🔍 Найдите JAR файл размером больше 40MB
echo 2. 📋 Скопируйте его в отдельную папку
echo 3. 🚀 Запустите командой:
echo    java -Xms2G -Xmx4G -jar [имя_файла].jar nogui
echo.
echo 4. 💡 Если все еще не работает:
echo    - Попробуйте разные JAR файлы
echo    - Проверьте логи ошибок
echo    - Возможно нужен готовый Paper JAR + наши патчи
echo.
echo 🎯 Цель: Найти JAR с PlayLand кодом И всеми зависимостями!
echo ================================================================

cd ..
pause
