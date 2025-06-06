# 🚀 Запуск PlayLand Core Server в IntelliJ IDEA

## Метод 1: runDevServer (РЕКОМЕНДУЕТСЯ)

### Шаги:
1. Откройте проект в IDEA: `paper-source`
2. Откройте Gradle панель (справа)
3. Найдите: `paper-server → Tasks → runs → runDevServer`
4. Двойной клик на `runDevServer`

### Что произойдет:
- ✅ Сервер запустится БЕЗ создания JAR
- ✅ Использует скомпилированные классы напрямую
- ✅ Включает все зависимости автоматически
- ✅ НАШ PlayLand код будет работать!

### Ожидаемый результат:
```
[INFO] Starting minecraft server version 1.21.5
[INFO] 🚀 Initializing PlayLand Core Server 2.2.0-REVOLUTIONARY
[INFO] ⚛️ Initializing Quantum Load Balancer...
[INFO] 🧠 Initializing Neural Network Predictor...
[INFO] 🧬 Initializing Genetic Algorithm Optimizer...
[INFO] 🎮 Initializing Vanilla Mechanics Manager...
[INFO] ✅ PlayLand Core Server initialized successfully!
```

## Метод 2: runPaperclip (для JAR)

### Шаги:
1. Сначала создайте Paperclip JAR:
   `paper-server → Tasks → paperweight → createMojmapPaperclipJar`
2. Затем запустите:
   `paper-server → Tasks → runs → runPaperclip`

## Метод 3: Создание Run Configuration

### Шаги:
1. Run → Edit Configurations
2. Add New → Application
3. Настройки:
   - Name: PlayLand Dev Server
   - Main class: org.bukkit.craftbukkit.Main
   - Program arguments: nogui
   - VM options: -Xms2G -Xmx4G -XX:+UseG1GC
   - Working directory: создайте папку `run` в корне проекта
   - Use classpath of module: paper-server.main

## 🔧 Если возникают проблемы:

### Проблема: "Cannot find main class"
**Решение:** 
- Убедитесь что проект полностью проиндексирован
- File → Invalidate Caches and Restart

### Проблема: "ClassNotFoundException"
**Решение:**
- Используйте `runDevServer` вместо создания JAR
- Проверьте что все зависимости загружены в Gradle

### Проблема: "PlayLand код не инициализируется"
**Решение:**
- Проверьте что в MinecraftServer.java есть вызов PlayLandCoreServer.initialize()
- Убедитесь что код скомпилирован без ошибок

## 🎯 Цель:

Получить работающий сервер с сообщениями:
```
✅ PlayLand Core Server initialized!
⚛️ Quantum efficiency: 95.0%
🧠 Neural accuracy: 0.0%
🧬 Genetic fitness: 0.0000
🎮 Vanilla compatibility: 100.0%
```

## 📋 Проверка успеха:

1. **Сервер запускается** без ошибок зависимостей
2. **PlayLand сообщения** появляются в логах
3. **Можно подключиться** к серверу (localhost:25565)
4. **Все ванильные механики** работают
5. **Оптимизации активны** (видно в логах)

**runDevServer - это самый надежный способ протестировать наше ядро!**
