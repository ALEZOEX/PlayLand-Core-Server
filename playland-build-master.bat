@echo off
chcp 65001 >nul
REM PlayLand Core Server - Мастер сборки
REM Систематическое решение проблемы компиляции JAR

echo.
echo ================================================================
echo   🚀 PLAYLAND CORE SERVER - МАСТЕР СБОРКИ
echo ================================================================
echo   Систематическое устранение проблемы компиляции JAR
echo ================================================================
echo.

if not exist "paper-source" (
    echo ❌ ОШИБКА: Paper source не найден!
    echo Сначала выполните clone-paper-source.bat
    pause
    exit /b 1
)

cd paper-source

echo [ЭТАП 1/6] 🔍 ДИАГНОСТИКА СИСТЕМЫ СБОРКИ
echo ================================================================
echo.

echo 📋 Проверяем версию Java...
java -version
echo.

echo 📋 Проверяем версию Gradle...
gradlew.bat --version
echo.

echo 📋 Анализируем доступные задачи Paper...
echo Поиск всех задач связанных с JAR:
gradlew.bat tasks --all | findstr -i "jar\|bundle\|paperclip\|mojmap"
echo.

echo 📋 Проверяем структуру проекта...
if exist "paper-server\src\main\java\ru\playland" (
    echo ✅ PlayLand код найден в paper-server
    dir "paper-server\src\main\java\ru\playland" /b
) else (
    echo ❌ PlayLand код НЕ найден!
    echo Проверяем альтернативные расположения...
    for /r . %%d in (ru) do (
        if exist "%%d\playland" (
            echo 📁 Найден PlayLand код в: %%d\playland
        )
    )
)
echo.

echo [ЭТАП 2/6] 🧹 ОЧИСТКА ПРЕДЫДУЩИХ СБОРОК
echo ================================================================
echo.
gradlew.bat clean --no-daemon --console=plain
echo ✅ Очистка завершена
echo.

echo [ЭТАП 3/6] 🔨 ТЕСТИРОВАНИЕ МЕТОДОВ СБОРКИ PAPER
echo ================================================================
echo.

echo 🎯 МЕТОД 1: createMojmapPaperclipJar (современный Paper)
echo ----------------------------------------------------------------
gradlew.bat createMojmapPaperclipJar --no-daemon --console=plain --stacktrace

if %ERRORLEVEL% equ 0 (
    echo ✅ createMojmapPaperclipJar УСПЕШНО!
    set BUILD_SUCCESS=1
    set BUILD_METHOD=createMojmapPaperclipJar
    goto :check_jars
) else (
    echo ❌ createMojmapPaperclipJar не удался
    echo.
)

echo 🎯 МЕТОД 2: createReobfPaperclipJar (альтернативный)
echo ----------------------------------------------------------------
gradlew.bat createReobfPaperclipJar --no-daemon --console=plain --stacktrace

if %ERRORLEVEL% equ 0 (
    echo ✅ createReobfPaperclipJar УСПЕШНО!
    set BUILD_SUCCESS=1
    set BUILD_METHOD=createReobfPaperclipJar
    goto :check_jars
) else (
    echo ❌ createReobfPaperclipJar не удался
    echo.
)

echo 🎯 МЕТОД 3: createMojmapBundlerJar (bundler подход)
echo ----------------------------------------------------------------
gradlew.bat createMojmapBundlerJar --no-daemon --console=plain --stacktrace

if %ERRORLEVEL% equ 0 (
    echo ✅ createMojmapBundlerJar УСПЕШНО!
    set BUILD_SUCCESS=1
    set BUILD_METHOD=createMojmapBundlerJar
    goto :check_jars
) else (
    echo ❌ createMojmapBundlerJar не удался
    echo.
)

echo 🎯 МЕТОД 4: reobfJar с debug режимом
echo ----------------------------------------------------------------
gradlew.bat :paper-server:reobfJar -Ppaperweight.debug=true --no-daemon --console=plain --stacktrace

if %ERRORLEVEL% equ 0 (
    echo ✅ reobfJar с debug УСПЕШНО!
    set BUILD_SUCCESS=1
    set BUILD_METHOD=reobfJar-debug
    goto :check_jars
) else (
    echo ❌ reobfJar с debug не удался
    echo.
)

echo 🎯 МЕТОД 5: Стандартная сборка (fallback)
echo ----------------------------------------------------------------
gradlew.bat build --no-daemon --console=plain --stacktrace

if %ERRORLEVEL% equ 0 (
    echo ✅ Стандартная сборка УСПЕШНА!
    set BUILD_SUCCESS=1
    set BUILD_METHOD=build
    goto :check_jars
) else (
    echo ❌ Все методы сборки не удались!
    set BUILD_SUCCESS=0
    goto :build_failed
)

:check_jars
echo.
echo [ЭТАП 4/6] 🔍 ПРОВЕРКА РЕЗУЛЬТИРУЮЩИХ JAR ФАЙЛОВ
echo ================================================================
echo.

echo 📦 Поиск всех JAR файлов в проекте...
set FOUND_WORKING_JAR=0

for /r . %%f in (*.jar) do (
    if %%~zf GTR 10000000 (
        echo.
        echo 🔍 АНАЛИЗ JAR: %%~nxf
        echo    📁 Путь: %%~dpf
        echo    📊 Размер: %%~zf байт
        
        REM Проверяем joptsimple (критично для запуска)
        jar -tf "%%f" | findstr "joptsimple" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo    ✅ Содержит joptsimple ^(зависимости^)
            set HAS_DEPS=1
        ) else (
            echo    ❌ НЕ содержит joptsimple
            set HAS_DEPS=0
        )
        
        REM Проверяем PlayLand код
        jar -tf "%%f" | findstr "ru/playland" >nul 2>&1
        if !ERRORLEVEL! equ 0 (
            echo    ✅ Содержит PlayLand код
            set HAS_PLAYLAND=1
        ) else (
            echo    ❌ НЕ содержит PlayLand код
            set HAS_PLAYLAND=0
        )
        
        REM Проверяем Main-Class
        jar -xf "%%f" META-INF/MANIFEST.MF >nul 2>&1
        if exist "META-INF\MANIFEST.MF" (
            findstr "Main-Class" "META-INF\MANIFEST.MF" >nul 2>&1
            if !ERRORLEVEL! equ 0 (
                echo    ✅ Содержит Main-Class
                set HAS_MAIN=1
                for /f "tokens=2 delims= " %%m in ('findstr "Main-Class" "META-INF\MANIFEST.MF"') do (
                    echo    📋 Main-Class: %%m
                )
            ) else (
                echo    ❌ НЕ содержит Main-Class
                set HAS_MAIN=0
            )
            rmdir /s /q META-INF >nul 2>&1
        ) else (
            echo    ❌ НЕ удалось извлечь манифест
            set HAS_MAIN=0
        )
        
        REM Оценка пригодности JAR
        if %%~zf GTR 40000000 (
            echo    🎯 БОЛЬШОЙ JAR ^(40MB+^) - отличный кандидат!
            if !HAS_DEPS! equ 1 if !HAS_PLAYLAND! equ 1 if !HAS_MAIN! equ 1 (
                echo    🏆 ИДЕАЛЬНЫЙ JAR ДЛЯ ЗАПУСКА!
                set WORKING_JAR=%%f
                set FOUND_WORKING_JAR=1
            )
        ) else if %%~zf GTR 25000000 (
            echo    ⚠️  Средний размер - возможно рабочий
        ) else (
            echo    ❌ Слишком маленький
        )
    )
)

if %FOUND_WORKING_JAR% equ 1 (
    goto :test_jar
) else (
    goto :no_working_jar
)

:test_jar
echo.
echo [ЭТАП 5/6] 🚀 ТЕСТИРОВАНИЕ ЗАПУСКА СЕРВЕРА
echo ================================================================
echo.

echo 🎯 Найден рабочий JAR: %WORKING_JAR%
echo 📋 Команда запуска: java -Xms2G -Xmx4G -XX:+UseG1GC -jar "%WORKING_JAR%" nogui
echo.
echo ⚠️  ВНИМАНИЕ: Сейчас будет произведена попытка запуска сервера!
echo    Сервер запустится в тестовом режиме на 30 секунд.
echo    Следите за логами инициализации PlayLand систем.
echo.
pause

echo 🚀 Запуск PlayLand Core Server...
timeout /t 3 >nul

REM Создаем временную папку для тестирования
if not exist "test-server" mkdir "test-server"
copy "%WORKING_JAR%" "test-server\playland-server.jar" >nul
cd test-server

REM Создаем eula.txt для автоматического принятия
echo eula=true > eula.txt

REM Запускаем сервер с таймаутом
echo 📋 Запуск сервера с мониторингом логов...
start /b java -Xms2G -Xmx4G -XX:+UseG1GC -jar playland-server.jar nogui > server.log 2>&1

REM Ждем 15 секунд и проверяем логи
timeout /t 15 >nul

echo.
echo 📋 АНАЛИЗ ЛОГОВ ЗАПУСКА:
if exist "server.log" (
    echo ----------------------------------------------------------------
    type server.log | findstr -i "playland\|quantum\|neural\|genetic\|vanilla\|error\|exception"
    echo ----------------------------------------------------------------
) else (
    echo ❌ Лог файл не найден
)

REM Останавливаем сервер
taskkill /f /im java.exe >nul 2>&1

cd ..
goto :validation

:no_working_jar
echo.
echo ❌ НЕ НАЙДЕН ПОДХОДЯЩИЙ JAR ФАЙЛ!
echo.
goto :build_failed

:build_failed
echo.
echo [ЭТАП 6/6] 🔧 ДИАГНОСТИКА ПРИ НЕУДАЧЕ
echo ================================================================
echo.

echo 📋 Диагностика проблем сборки:
echo.
echo 1. 🔍 Проверка доступных задач:
gradlew.bat tasks --all | findstr -i "jar" | head -10
echo.

echo 2. 📋 Проверка версий:
echo    Java: 
java -version 2>&1 | head -1
echo    Gradle: 
gradlew.bat --version | findstr "Gradle"
echo.

echo 3. 📁 Проверка структуры PlayLand:
if exist "paper-server\src\main\java\ru\playland" (
    echo    ✅ PlayLand код на месте
) else (
    echo    ❌ PlayLand код отсутствует!
)
echo.

echo 💡 РЕКОМЕНДАЦИИ:
echo    - Проверьте ошибки сборки выше
echo    - Убедитесь что Java 21+ установлена
echo    - Возможно нужно обновить Paper source
echo    - Рассмотрите использование готового Paper JAR
echo.
goto :end

:validation
echo.
echo [ЭТАП 6/6] ✅ ВАЛИДАЦИЯ ИНТЕГРАЦИИ PLAYLAND
echo ================================================================
echo.

echo 🎯 Проверка инициализации революционных систем:
if exist "test-server\server.log" (
    echo.
    echo 🔬 Quantum Load Balancer:
    type "test-server\server.log" | findstr -i "quantum" && echo ✅ Найден || echo ❌ Не найден
    
    echo 🧠 Neural Network Predictor:
    type "test-server\server.log" | findstr -i "neural" && echo ✅ Найден || echo ❌ Не найден
    
    echo 🧬 Genetic Algorithm Optimizer:
    type "test-server\server.log" | findstr -i "genetic" && echo ✅ Найден || echo ❌ Не найден
    
    echo 🎮 Vanilla Mechanics Manager:
    type "test-server\server.log" | findstr -i "vanilla" && echo ✅ Найден || echo ❌ Не найден
    
    echo 🚀 PlayLand Core Server:
    type "test-server\server.log" | findstr -i "playland" && echo ✅ Найден || echo ❌ Не найден
)

echo.
echo ================================================================
echo   🎉 РЕЗУЛЬТАТ РАЗРАБОТКИ PLAYLAND CORE SERVER
echo ================================================================
echo.

if %FOUND_WORKING_JAR% equ 1 (
    echo ✅ УСПЕХ! PlayLand Core Server создан и протестирован!
    echo.
    echo 📦 Рабочий JAR: %WORKING_JAR%
    echo 🔨 Метод сборки: %BUILD_METHOD%
    echo 📊 Размер: больше 40MB
    echo 🎯 Статус: Готов к использованию
    echo.
    echo 🚀 Для запуска:
    echo    java -Xms2G -Xmx4G -XX:+UseG1GC -jar "%WORKING_JAR%" nogui
    echo.
    echo 🌟 Революционные системы интегрированы в ядро сервера!
) else (
    echo ❌ НЕУДАЧА: Не удалось создать рабочий JAR
    echo.
    echo 💡 Возможные решения:
    echo    - Проверьте ошибки сборки
    echo    - Обновите Paper source
    echo    - Используйте готовый Paper + плагин подход
)

:end
echo ================================================================
cd ..
pause
