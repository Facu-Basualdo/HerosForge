# Estadísticas RPG

Mod de Fabric para **Minecraft Java 26.3** para mejorar a tu personaje estilo RPG. Pagás **1 nivel de experiencia** y subís **1 punto** de la estadística que quieras: más vida, más daño, más armadura, más velocidad… Los puntos son tuyos para siempre: no se pierden al morir.

> **Estado:** en desarrollo. Ya se pueden comprar vida, daño, armadura y velocidad por comando; el menú y el resto de las estadísticas vienen en las próximas fases ([docs/plan.md](docs/plan.md)).

## En el juego

| Estadística | Id | Por punto | Máximo | |
|---|---|---|---|---|
| Vida | `vida` | +1 corazón | +10 corazones | ✅ |
| Daño | `danio` | +0,5 | +5 | ✅ |
| Armadura | `armadura` | +1 | +10 | ✅ |
| Velocidad | `velocidad` | +2 % | +20 % | ✅ |
| Velocidad de ataque | | +3 % | +30 % | próximamente |
| Minado | | +5 % | +50 % | próximamente |
| Alcance | | +0,25 bloques | +2 bloques | próximamente |
| Resistencia al empuje | | +5 % | +50 % | próximamente |
| Suerte | | +0,5 | +3 | próximamente |

- **Cada punto cuesta 1 nivel**, igual que encantar: conviene comprar cuando te sobra un nivel, no acumular.
- **Se compra lo que entra:** si pedís 5 y te faltan niveles o llegás al máximo antes, se compran los que se pueda y se cobran sólo esos.
- **Los puntos son para siempre:** se conservan al morir, al reconectar y al volver del End, y reaparecés con la vida llena contando los corazones extra.
- Al comprar vida, el corazón nuevo aparece lleno.
- Los números son los de por defecto; se van a poder cambiar en `config/estadisticasrpg.json`.

### Comandos

| Comando | Qué hace | Quién |
|---|---|---|
| `/estadisticas` o `/stats` | Muestra tus puntos, el efecto de cada estadística y cuánto cuesta (más adelante, abre el menú) | todos |
| `/estadisticas ver [jugador]` | Lo mismo, de otro jugador | todos (otro jugador: op) |
| `/estadisticas subir <estadística> [cantidad]` | Compra puntos (por defecto, 1) | todos |
| `/estadisticas dar <jugador> <estadística> <cantidad>` | Da puntos sin cobrar, hasta el máximo | op |
| `/estadisticas reiniciar <jugador>` | Pone todo en 0 sin devolver los niveles | op |

`/stats` sirve en lugar de `/estadisticas` en todos.

## Requisitos

| | Versión |
|---|---|
| Minecraft Java | 26.3 |
| [Fabric Loader](https://fabricmc.net/use/) | 0.19.5 o más nuevo |
| [Fabric API](https://modrinth.com/mod/fabric-api) | para 26.3 |
| Java | 25 |

## Instalación

Toda la lógica corre en el servidor.

- **Servidor dedicado:** copiá `estadisticasrpg-<versión>.jar` y Fabric API a la carpeta `mods/` del servidor. Los jugadores no necesitan instalar nada.
- **Singleplayer o LAN:** instalalo en el cliente del que abre el mundo, junto con Fabric API.

## Compilar

```bash
./gradlew build          # Windows: gradlew build
```

El jar queda en `build/libs/`.

El proyecto pide Java 25 (`gradle/gradle-daemon-jvm.properties`). Si Gradle no encuentra tu JDK 25, agregá a tu `~/.gradle/gradle.properties` (el global, no el del repo) la línea `org.gradle.java.installations.paths=<ruta a tu JDK 25>`.

## Probar

```bash
./gradlew runGameTest    # pruebas automáticas en un servidor sin ventana
```

Las pruebas verifican que:

- comprar cobra los niveles justos, sube el atributo y llena el corazón nuevo;
- sin niveles o en el máximo no se cobra nada, y si pedís de más se compra lo que entra;
- aplicar los puntos dos veces no los apila;
- al morir y reaparecer se conservan los puntos, la vida máxima y la vida llena; al volver del End, también la vida que tenías;
- guardar y cargar al jugador conserva los puntos y la vida (aunque sea más de 20);
- una estadística desconocida en el archivo guardado no borra las demás;
- `reiniciar` deja todo en 0 y la vida no queda por encima del máximo;
- los comandos `subir`, el alias `/stats` y el permiso de op de `dar` funcionan.

Para probar a mano, mirá [claude/general/probar-en-el-juego.md](claude/general/probar-en-el-juego.md).

## Estructura

```
src/main/       el mod
src/gametest/   pruebas automáticas (Fabric GameTest)
claude/         documentación técnica para agentes de IA (invariantes, decisiones y bugs ya resueltos)
docs/           el plan del mod por fases
```

Para entender por qué el código está hecho así, empezá por [CLAUDE.md](CLAUDE.md) y [claude/mod/INDICE.md](claude/mod/INDICE.md).

## Licencia

[MIT](LICENSE): podés usarlo, modificarlo y redistribuirlo (también en modpacks) siempre que mantengas el aviso de copyright.
