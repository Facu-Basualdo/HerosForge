# Estadísticas RPG

Mod de Fabric para **Minecraft Java 26.3** para mejorar a tu personaje estilo RPG. Pagás **1 nivel de experiencia** y subís **1 punto** de la estadística que quieras: más vida, más daño, más armadura, más velocidad… Los puntos son tuyos para siempre: no se pierden al morir.

> **Estado:** en desarrollo. Ya se pueden comprar las 9 estadísticas desde el menú o por comando; falta poder cambiar los números en un archivo de config ([docs/plan.md](docs/plan.md)).

## En el juego

| Estadística | Id | Por punto | Máximo |
|---|---|---|---|
| Vida | `vida` | +1 corazón | +10 corazones |
| Daño | `danio` | +0,5 | +5 |
| Armadura | `armadura` | +1 | +10 |
| Velocidad | `velocidad` | +2 % | +20 % |
| Velocidad de ataque | `velocidad_ataque` | +3 % | +30 % |
| Minado | `minado` | +5 % | +50 % |
| Alcance (a bloques y a mobs) | `alcance` | +0,25 bloques | +2 bloques |
| Resistencia al empuje | `empuje` | +5 % | +50 % |
| Suerte (al pescar) | `suerte` | +0,5 | +3 |

- **Cada punto cuesta 1 nivel**, igual que encantar: conviene comprar cuando te sobra un nivel, no acumular.
- **Se compra lo que entra:** si pedís 5 y te faltan niveles o llegás al máximo antes, se compran los que se pueda y se cobran sólo esos.
- **Los puntos son para siempre:** se conservan al morir, al reconectar y al volver del End, y reaparecés con la vida llena contando los corazones extra.
- Al comprar vida, el corazón nuevo aparece lleno.
- Los números son los de por defecto; se van a poder cambiar en `config/estadisticasrpg.json`.

### El menú

`/estadisticas` (o `/stats`) abre un cofre con un ítem por estadística: manzana dorada (vida), espada (daño), peto (armadura), azúcar (velocidad), pluma (velocidad de ataque), pico (minado), caña de pescar (alcance), yunque (empuje) y pata de conejo (suerte). Arriba, una botella de experiencia muestra cuántos niveles tenés.

- **Click:** compra 1 punto. **Shift + click:** compra 5 (o los que entren).
- Cada ítem muestra tus puntos, el efecto que te dan, lo que suma el próximo y cuánto cuesta: en verde si te alcanza, en rojo si no.
- Una estadística al máximo se ve como vidrio gris.
- Los ítems del menú son de muestra: no se pueden sacar, ni con doble click, arrastrando o con las teclas numéricas.
- Funciona con un cliente sin el mod: es un cofre común armado por el servidor.

### Comandos

| Comando | Qué hace | Quién |
|---|---|---|
| `/estadisticas` o `/stats` | Abre el menú | todos |
| `/estadisticas ver [jugador]` | Tus puntos (o los de otro jugador) en el chat: efecto de cada estadística y cuánto cuesta | todos (otro jugador: op) |
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
- en el menú, un click compra 1 y shift + click compra 5, y ningún ítem de muestra se puede sacar (doble click, arrastrar, teclas numéricas, tirar) ni se puede meter nada.
- las 9 estadísticas al máximo dan los valores de la tabla, y las que tiene que ver el cliente (velocidad, velocidad de ataque, minado y alcance) se le mandan al cliente vanilla;
- toda estadística tiene sus números en la config y su lugar en el menú.

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
