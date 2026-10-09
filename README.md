# Estadísticas RPG

Mod de Fabric para **Minecraft Java 26.3** para mejorar a tu personaje estilo RPG. Pagás **1 nivel de experiencia** y subís **1 punto** de la estadística que quieras: más vida, más daño, más armadura, más velocidad… Los puntos son tuyos para siempre: no se pierden al morir.

> **Estado:** en desarrollo, todavía no hace nada en el juego. El diseño y las fases están en [docs/plan.md](docs/plan.md).

## Cómo va a funcionar en el juego

| Estadística | Por punto | Máximo |
|---|---|---|
| Vida | +1 corazón | +10 corazones |
| Daño | +0,5 | +5 |
| Armadura | +1 | +10 |
| Velocidad | +2 % | +20 % |
| Velocidad de ataque | +3 % | +30 % |
| Minado | +5 % | +50 % |
| Alcance | +0,25 bloques | +2 bloques |
| Resistencia al empuje | +5 % | +50 % |
| Suerte | +0,5 | +3 |

- **Cada punto cuesta 1 nivel**, igual que encantar: conviene comprar cuando te sobra un nivel, no acumular.
- `/estadisticas` abre un menú con un ítem por estadística: click para comprar un punto, shift + click para comprar 5.
- Los números son los de por defecto y se van a poder cambiar en `config/estadisticasrpg.json`.

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
