# Gradle задачи для создания рабочего PlayLand JAR

## 🎯 Приоритетные задачи (попробуйте в IDEA):

### 1. Paperclip задачи (высокий приоритет):
```
paper-server → Tasks → paperweight → createMojmapPaperclipJar
paper-server → Tasks → paperweight → createReobfPaperclipJar
```

### 2. Bundler задачи (средний приоритет):
```
paper-server → Tasks → paperweight → createMojmapBundlerJar
paper-server → Tasks → paperweight → createReobfBundlerJar
```

### 3. Development задачи (для тестирования):
```
paper-server → Tasks → paperweight → runServer
paper-server → Tasks → paperweight → runDevServer
```

### 4. Shadow JAR (если доступно):
```
paper-server → Tasks → shadow → shadowJar
```

## 🔧 Если задачи не работают:

### Попробуйте с параметрами:
1. Откройте Terminal в IDEA (Alt+F12)
2. Выполните:
```bash
./gradlew createMojmapPaperclipJar --stacktrace --info
./gradlew createReobfPaperclipJar --stacktrace --info
./gradlew build --stacktrace --info
```

### Проверьте build.gradle.kts:
Убедитесь что в `paper-server/build.gradle.kts` есть:
```kotlin
plugins {
    id("io.papermc.paperweight.patcher") version "1.7.7"
}
```

## 🎯 Цель:
Найти задачу, которая создает JAR файл содержащий:
- ✅ Все зависимости (включая joptsimple)
- ✅ Наш PlayLand код
- ✅ Main-Class для запуска

## 📋 Как проверить результат:
После выполнения задачи проверьте:
1. `paper-server/build/libs/` - новые JAR файлы
2. Размер JAR > 40MB (признак fat JAR)
3. `jar -tf [jar-name].jar | grep joptsimple` - есть зависимости
4. `jar -tf [jar-name].jar | grep ru/playland` - есть наш код
