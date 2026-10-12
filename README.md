# Hero's Forge

<p align="center"><img src="src/main/resources/assets/herosforge/icon.png" alt="Hero's Forge" width="256"></p>

Mod de Fabric para **Minecraft Java 26.3** para mejorar a tu personaje estilo RPG. Pagás **experiencia** y subís **puntos** de la estadística que quieras, hasta 100 en cada una: más vida, más daño (también con arco y ballesta), más armadura, regeneración, robo de vida, más salto… Cada punto cuesta **30 puntos de experiencia**, y si te arrepentís, bajarlo te devuelve la mitad. Los puntos son tuyos para siempre: no se pierden al morir.

## Estadísticas

Valores por defecto (se cambian en la [config](#configuración)):

| Estadística | Id | Por punto | Con 100 puntos |
|---|---|---|---|
| Vida | `vida` | +0,3 corazones | +30 corazones |
| Daño (cuerpo a cuerpo) | `danio` | +0,25 | +25 |
| Daño a distancia (arco, ballesta, tridente lanzado) | `danio_distancia` | +1 % | +100 % |
| Armadura | `armadura` | +0,08 | +8 |
| Velocidad de ataque | `velocidad_ataque` | +0,3 % | +30 % |
| Regeneración | `regeneracion` | +0,09 corazones por minuto | +9 corazones por minuto |
| Robo de vida | `robo_de_vida` | +0,1 % del daño que hacés | +10 % |
| Velocidad | `velocidad` | +0,2 % | +20 % |
| Minado | `minado` | +1 % | +100 % |
| Alcance (a bloques y a mobs) | `alcance` | +0,02 bloques | +2 bloques |
| Resistencia al empuje | `empuje` | +0,5 % | +50 % |
| Suerte (al pescar) | `suerte` | +0,03 | +3 |
| Salto | `salto` | +0,2 % de fuerza | +20 % (1,8 bloques) |
| Caída segura | `caida` | +0,1 bloques | +10 bloques sin daño |
| Respiración | `respiracion` | +0,03 | +3 (como Respiración III) |

- **Se cobra en puntos de experiencia, no en niveles:** un punto cuesta lo mismo en el nivel 2 que en el 40. La compra baja la barra y, si hace falta, el nivel.
- **Se compra lo que entra:** si pedís 5 y te alcanza para 3, se compran y se cobran 3.
- **Bajar un punto devuelve la mitad** de lo que costó: sirve para cambiar de idea, pero mover los puntos según la pelea sale caro.
- **Los puntos son para siempre:** se conservan al morir, al reconectar y al volver del End.
- El daño a distancia multiplica el daño final de la flecha o el tridente (con Poder y el crítico). La regeneración no depende del hambre. El robo de vida cura una parte del daño que hacés, también con flechas, y el golpe que mata no cura.
- El modo creativo paga igual.

## Cómo se suben

### El menú (sin instalar nada)

`/estadisticas` (o `/stats`) abre un cofre con un ítem por estadística: arriba las de combate y abajo las demás.

- **Click:** compra 1 punto. **Shift + click:** compra 5.
- **Click derecho:** baja 1 y devuelve la mitad. **Shift + click derecho:** baja 5.
- Cada ítem muestra tus puntos, lo que te dan, lo que suma el próximo y cuánto cuesta (en verde si te alcanza).
- Funciona con un cliente vanilla: es un cofre armado por el servidor.

### La pantalla propia (opcional, en el cliente)

Si también instalás el mod **en tu cliente**, tenés una pantalla propia, más cómoda que el cofre:

- Se abre con la **K** (se cambia en *Opciones → Controles → Hero's Forge*) o con el botón de la botella de experiencia del inventario.
- Botones **+1**, **+5** y **-1** en cada estadística, con lo que suma el próximo punto, el costo y la devolución al pasar el mouse. Se actualiza sola.
- **Ver como cofre** abre el menú de siempre. Si preferís el cofre, poné `"usarPantalla": false` en `config/herosforge-cliente.json` de tu cliente.
- Hace falta que el servidor tenga la misma versión del mod (o una más nueva).

## Comandos

| Comando | Qué hace | Quién |
|---|---|---|
| `/estadisticas` o `/stats` | Abre el menú | todos |
| `/estadisticas ver [jugador]` | Tus puntos (o los de otro) en el chat | todos (otro jugador: op) |
| `/estadisticas subir <estadística> [cantidad]` | Compra puntos | todos |
| `/estadisticas bajar <estadística> [cantidad]` | Baja puntos y devuelve la mitad | todos |
| `/estadisticas dar <jugador> <estadística> <cantidad>` | Da puntos sin cobrar | op |
| `/estadisticas reiniciar <jugador>` | Pone todo en 0 sin devolver la experiencia | op |
| `/estadisticas recargar` | Vuelve a leer la config | op |

## Configuración

`config/herosforge.json` se crea en el primer arranque. Después de editarlo, `/estadisticas recargar` lo aplica sin reiniciar.

| Campo | Qué es |
|---|---|
| `experienciaPorPunto` | Experiencia que cuesta cada punto (0 = gratis) |
| `devolucionAlBajar` | Qué parte del costo devuelve bajar un punto, de 0 a 1 |
| `estadisticas.<id>.porPunto` | Cuánto suma cada punto, en las unidades de Minecraft: la vida en medios corazones (`0.6` = 0,3 corazones) y los porcentajes como fracción (`0.002` = 0,2 %) |
| `estadisticas.<id>.tope` | Máximo de puntos de esa estadística |

Un valor inválido usa el de por defecto y lo avisa; un JSON roto se ignora y sigue la config anterior. Bajar un tope no le quita puntos a nadie: los de más dejan de sumar.

## Funciona con

- **Bloodmoon & Bosses:** los valores por defecto están balanceados contra sus mobs de nivel 1 a 100. Con el equipo y los puntos que se esperan en cada zona, un zombi se mata en 5 o 6 golpes en todo el mundo.
- **Soul Link:** los puntos son de todo el grupo. Lo que compra uno lo tienen todos, la compra gasta la experiencia compartida y la regeneración cura una sola vez para todos. Los puntos que cada uno tenía antes se suman al grupo (el máximo por estadística) la primera vez que entra.
- **Mythic Loot:** el robo de vida y el daño a distancia se suman a los afijos de sus armas.

## Requisitos e instalación

Minecraft Java 26.3, [Fabric Loader](https://fabricmc.net/use/) 0.19.5 o más nuevo, [Fabric API](https://modrinth.com/mod/fabric-api) y Java 25.

- **Servidor:** copiá `herosforge-<versión>.jar` y Fabric API a `mods/`. Los jugadores no necesitan nada.
- **Singleplayer o LAN:** en el cliente del que abre el mundo, junto con Fabric API.
- **Pantalla propia:** el mismo jar en `mods/` de tu cliente. Es opcional.

### Actualizar desde una versión anterior a la 0.12.0

Hasta la 0.11.1 el id era `estadisticasrpg` (el mod antes se llamaba Estadísticas RPG). Borrá el jar viejo y poné el nuevo: **los puntos de cada jugador, los del grupo y la config se pasan solos** al id nuevo. Quien use la pantalla propia tiene que actualizar también el jar de su cliente, y si había cambiado la tecla, volver a asignarla. Por las dudas, hacé una copia del mundo antes.

## Desarrollo

```bash
./gradlew build                # compila y corre las pruebas; el jar queda en build/libs/
./gradlew runGameTest          # pruebas automáticas en un servidor sin ventana
./gradlew runClientGameTest    # prueba la pantalla en un cliente de verdad (abre una ventana)
./gradlew runGameTest -PsoulLinkJar=../SoulLink/build/libs/soullink-2.0.0.jar   # integración con Soul Link real
```

Pide Java 25: si Gradle no encuentra tu JDK, agregá `org.gradle.java.installations.paths=<ruta a tu JDK 25>` a tu `~/.gradle/gradle.properties`. La documentación técnica está en [CLAUDE.md](CLAUDE.md).

## Licencia

[MIT](LICENSE).
