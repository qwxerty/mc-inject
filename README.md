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
3. Pojawi sie okno Client z modulami i przyciskiem Eject.

## Diagnostyka
Po kazdym injectcie payload zapisuje szczegolowy log do:

    %USERPROFILE%\\mc-inject\\debug.log

Dodatkowo krytyczne etapy agenta i wywolan hookow sa wypisywane do konsoli Minecrafta. Log obejmuje:
- start Agent.agentmain i wykryty ClassLoader Minecrafta,
- zaladowanie client.Client i mappingow,
- znalezienie klas/metod/pol przez reflection,
- rejestracje transformera i wynik retransformatowania,
- faktyczne wejscie BootstrapHooks.onTick, onRender, onPlayerUpdate, onClickMouse,
- wywolanie Reach, liczbe encji w zasiegu/FOV, wybrany target i bledy reflection.

Przy testowaniu najlepiej zrobic pelny restart Minecrafta przed injectem nowej wersji payloadu, a po probie wyslac zawartosc debug.log.

## Rozbudowa
- nowy modul: klasa w client/module/impl + register(...) w ModuleManager
- nowe pole/metoda MC: wpis w .properties + wrapper w Mc.java
- nowy hook: wpis w HookManager.HOOKS + metoda w BootstrapHooks + dispatch do modulow
- BootstrapHooks jest celowo JDK-only i ma osobna nazwe, zeby unikac konfliktu ze starym client.Hooks w LaunchWrapper
- Reach aktualizuje target bezposrednio przed clickMouse() i zachowuje vanilla target przy normalnym zasiegu
- inna wersja MC: nowy plik w resources/mappings, nazwa profilu jako 3. argument
