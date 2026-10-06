# mc-inject

Szablon injectowalnego klienta do Minecrafta w czystej Javie (Attach API + ASM), bez C++/JNI.

## Wymagania
- JDK 17+ (musi byc JDK, nie JRE) - do budowania i do injectora
- Gradle 8.x (albo wbudowany w IntelliJ)
- Minecraft Forge 1.8.9 (domyslny profil mappingow)

## Build
    gradle wrapper --gradle-version 8.5     # raz, tworzy gradlew
    ./gradlew build                          # Windows: gradlew.bat build

Wyniki:
- injector/build/libs/injector.jar
- payload/build/libs/payload.jar

## Uruchomienie
1. Odpal Minecrafta (Forge 1.8.9), wejdz do swiata (single).
2. Wstrzyknij:  inject.bat (Windows) | ./inject.sh | python inject.py
   lub: java -jar injector/build/libs/injector.jar payload/build/libs/payload.jar [pid] [profil]
3. Pojawi sie okno "Client" z modulami i przyciskiem Eject.

## Rozbudowa
- nowy modul: klasa w client/module/impl + register(...) w ModuleManager
- nowe pole/metoda MC: wpis w .properties + wrapper w Mc.java
- nowy hook: wpis w HookManager.HOOKS + metoda w Hooks + dispatch do modulow
- inna wersja MC: nowy plik w resources/mappings, nazwa profilu jako 3. argument
