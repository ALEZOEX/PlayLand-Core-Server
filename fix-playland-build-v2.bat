@echo off
REM PlayLand Core Server - Исправление сборки v2
REM Используем все доступные методы Paper сборки

echo.
echo ================================================================
echo   🔧 ИСПРАВЛЕНИЕ PLAYLAND СБОРКИ V2
echo ================================================================
echo   Пробуем все возможные методы Paper сборки...
echo ================================================================
echo.

if not exist "paper-source" (
    echo ❌ Paper source не найден!
    pause
    exit /b 1
)

cd paper-source

echo [1/8] Проверяем доступные Gradle задачи...
echo.
echo 📋 Все доступные задачи сборки:
gradlew.bat tasks --all | findstr -i "jar\|bundle\|paperclip\|mojmap\|reobf"

echo.
echo [2/8] Очищаем предыдущую сборку...
gradlew.bat clean --no-daemon --console=plain

echo.
echo [3/8] Метод 1: createMojmapPaperclipJar...
echo Это должно создать полный JAR с зависимостями...
gradlew.bat createMojmapPaperclipJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo ✅ createMojmapPaperclipJar успешно!
    goto :check_results
)

echo ❌ createMojmapPaperclipJar не удался, пробуем следующий метод...

echo.
echo [4/8] Метод 2: createReobfPaperclipJar...
gradlew.bat createReobfPaperclipJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo ✅ createReobfPaperclipJar успешно!
    goto :check_results
)

echo ❌ createReobfPaperclipJar не удался, пробуем следующий метод...

echo.
echo [5/8] Метод 3: createMojmapBundlerJar...
gradlew.bat createMojmapBundlerJar --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo ✅ createMojmapBundlerJar успешно!
    goto :check_results
)

echo ❌ createMojmapBundlerJar не удался, пробуем следующий метод...

echo.
echo [6/8] Метод 4: Включаем debug режим и пробуем reobfJar...
echo Включаем paperweight debug режим...
gradlew.bat :paper-server:reobfJar -Ppaperweight.debug=true --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo ✅ reobfJar с debug режимом успешно!
    goto :check_results
)

echo ❌ reobfJar с debug не удался, пробуем следующий метод...

echo.
echo [7/8] Метод 5: Обычная сборка с включением зависимостей...
gradlew.bat build -x test --no-daemon --console=plain

if %ERRORLEVEL% equ 0 (
    echo ✅ Обычная сборка успешна!
    goto :check_results
)

echo ❌ Все методы сборки не удались!

:check_results
echo.
echo [8/8] Анализируем результаты сборки...
echo.

echo 🔍 Поиск всех JAR файлов в проекте...
for /r . %%f in (*.jar) do (
    echo.
    echo 📦 Найден JAR: %%~nxf
    echo    📁 Путь: %%~dpf
    echo    📊 Размер: %%~zf байт
    
    REM Анализируем каждый JAR
    if %%~zf GTR 10000000 (
        echo    🔍 Анализируем содержимое...
        
        REM Проверяем joptsimple
        jar -tf "%%f" | findstr "joptsimple" >nul 2>&1
        if %ERRORLEVEL% equ 0 (
            echo    ✅ Содержит joptsimple
            set HAS_JOPTSIMPLE=1
        ) else (
            echo    ❌ НЕ содержит joptsimple
            set HAS_JOPTSIMPLE=0
        )
        
        REM Проверяем PlayLand код
        jar -tf "%%f" | findstr "ru/playland" >nul 2>&1
        if %ERRORLEVEL% equ 0 (
            echo    ✅ Содержит PlayLand код
            set HAS_PLAYLAND=1
        ) else (
            echo    ❌ НЕ содержит PlayLand код
            set HAS_PLAYLAND=0
        )
        
        REM Проверяем Main-Class
        jar -xf "%%f" META-INF/MANIFEST.MF 2>nul
        if exist "META-INF\MANIFEST.MF" (
            findstr "Main-Class" "META-INF\MANIFEST.MF" >nul 2>&1
            if %ERRORLEVEL% equ 0 (
                echo    ✅ Содержит Main-Class
                set HAS_MAIN=1
            ) else (
                echo    ❌ НЕ содержит Main-Class
                set HAS_MAIN=0
            )
            rmdir /s /q META-INF 2>nul
        )
        
        REM Оценка JAR файла
        if %%~zf GTR 40000000 (
            echo    🎯 ПОТЕНЦИАЛЬНО РАБОЧИЙ JAR! (большой размер)
            if defined HAS_JOPTSIMPLE if defined HAS_PLAYLAND if defined HAS_MAIN (
                echo    🏆 ИДЕАЛЬНЫЙ КАНДИДАТ ДЛЯ ЗАПУСКА!
                echo    📋 Команда для тестирования:
                echo    java -Xms2G -Xmx4G -jar "%%f" nogui
            )
        ) else if %%~zf GTR 25000000 (
            echo    ⚠️  Средний размер - возможно рабочий
        ) else (
            echo    ❌ Слишком маленький - скорее всего не рабочий
        )
    ) else (
        echo    ❌ Слишком маленький JAR (%%~zf байт)
    )
)

echo.
echo ================================================================
echo   📋 РЕЗУЛЬТАТЫ ИСПРАВЛЕНИЯ СБОРКИ
echo ================================================================
echo.
echo 🎯 Ищите JAR файлы с такими характеристиками:
echo   ✅ Размер больше 40MB
echo   ✅ Содержит joptsimple (зависимости)
echo   ✅ Содержит ru/playland (наш код)
echo   ✅ Содержит Main-Class (запускаемый)
echo.
echo 🚀 Для тестирования найденного JAR:
echo   java -Xms2G -Xmx4G -jar [путь_к_jar] nogui
echo.
echo 💡 Если ни один JAR не подходит:
echo   - Paper система сборки очень сложная
echo   - Возможно нужны дополнительные настройки
echo   - Рассмотрите использование готового Paper + плагин
echo ================================================================

cd ..
pause
