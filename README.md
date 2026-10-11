# Estadísticas RPG

Mod de Fabric para **Minecraft Java 26.3** para mejorar a tu personaje estilo RPG. Pagás **1 nivel de experiencia** y subís **1 punto** de la estadística que quieras: más vida, más daño, más armadura, más velocidad… Los puntos son tuyos para siempre: no se pierden al morir.

> **Estado:** las 9 estadísticas, el menú, la config y los puntos compartidos con Inventario Compartido están hechos. Falta probarlo a fondo en un servidor real ([docs/plan.md](docs/plan.md)).

## En el juego

Valores por defecto (se cambian en la [config](#configuración)):

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

- **Cada punto cuesta 1 nivel** (por defecto), igual que encantar: un nivel vale más experiencia cuanto más alto estás, así que conviene comprar cuando te sobra uno, no acumular.
- **Se compra lo que entra:** si pedís 5 y te faltan niveles o llegás al máximo antes, se compran los que se pueda y se cobran sólo esos.
- **Los puntos son para siempre:** se conservan al morir, al reconectar y al volver del End, y reaparecés con la vida llena contando los corazones extra.
- Al comprar vida, el corazón nuevo aparece lleno.
- Comprar suena como subir de nivel y suelta unas chispas verdes; si no te alcanza o ya estás al máximo, suena el "no" del aldeano.
- **El modo creativo paga igual**: si no, alcanzaría con pasar a creativo, comprar todo y volver.

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
| `/estadisticas recargar` | Vuelve a leer `config/estadisticasrpg.json` y lo aplica a los que están conectados | op |

`/stats` sirve en lugar de `/estadisticas` en todos.

## Configuración

Al arrancar por primera vez, el servidor crea `config/estadisticasrpg.json` con los valores por defecto:

```json
{
  "costoPorPunto": 1,
  "estadisticas": {
    "vida": { "porPunto": 2.0, "tope": 10 },
    "danio": { "porPunto": 0.5, "tope": 10 },
    ...
  }
}
```

| Campo | Qué es |
|---|---|
| `costoPorPunto` | Niveles que cuesta cada punto (entero, 0 o más; con 0 es gratis) |
| `porPunto` | Cuánto suma cada punto, en las unidades del atributo de Minecraft: la vida va en medios corazones (`2.0` = 1 corazón), y los porcentajes como fracción (`0.02` = 2 %). Mayor que 0 |
| `tope` | Máximo de puntos de esa estadística (entero, 0 o más; con 0 no se puede comprar) |

- Después de editarlo, `/estadisticas recargar` lo aplica sin reiniciar el servidor.
- **Un valor inválido no rompe nada:** se usa el de por defecto para ese campo y `recargar` (o el log, al arrancar) dice cuál. Si el JSON está roto, se sigue usando la config anterior y el archivo no se toca.
- Si le falta una estadística (por ejemplo, después de actualizar el mod), se agrega sola al archivo con sus valores por defecto.
- **Bajar un tope no le quita puntos a nadie:** los de más se guardan pero no suman, y vuelven a contar si lo subís de nuevo.

## Con Inventario Compartido

Si en el servidor también está Inventario Compartido (el mod que comparte inventario, vida y experiencia entre todos los jugadores), **los puntos también son de todos**: lo que compra uno lo tienen todos, al momento.

- Se activa solo al detectar el mod; no hay que configurar nada. El menú lo avisa en la botella de experiencia.
- **Los niveles salen de la experiencia compartida**, así que comprar gasta los niveles del grupo, como cualquier otro gasto.
- Todos tienen la misma vida máxima, así que la vida compartida no se corta.
- `ver`, `dar` y `reiniciar` hablan del grupo: `reiniciar` con cualquier jugador pone en 0 los puntos de todos.
- **Los puntos que cada uno tenía antes se suman al grupo** la primera vez que entra: por estadística, el máximo entre lo del grupo y lo suyo (no la suma, para no pasar el tope). Pasa una sola vez por jugador.
- Los puntos individuales no se borran: si se saca Inventario Compartido, cada uno vuelve a tener los suyos.

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
- los comandos `subir`, el alias `/stats`, `dar` y `recargar` funcionan, y `dar` sólo para op;
- en el menú, un click compra 1 y shift + click compra 5, y ningún ítem de muestra se puede sacar (doble click, arrastrar, teclas numéricas, tirar) ni se puede meter nada;
- las 9 estadísticas al máximo dan los valores de la tabla, y las que tiene que ver el cliente (velocidad, velocidad de ataque, minado y alcance) se le mandan al cliente vanilla;
- toda estadística tiene sus números en la config y su lugar en el menú;
- con Inventario Compartido, lo que compra uno lo tienen todos, los puntos individuales se suman al grupo una sola vez y se conservan al reaparecer (las pruebas fuerzan el modo: no cargan Inventario Compartido de verdad);
- la config se crea con los valores por defecto, respeta lo que cambiaste, avisa de lo inválido sin pisarlo, completa lo que falta, no usa un JSON roto, y un cambio se aplica a los que están conectados.

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
