# Hero's Forge — mod de Minecraft

Mod de Fabric para **Minecraft Java 26.3** para subir las estadísticas del jugador estilo RPG: se pagan **puntos de experiencia** (30 por punto, siempre los mismos; no niveles) por **puntos** de vida, daño, crítico, esquiva, metabolismo, imán u otra de las **40 estadísticas en 5 ramas** (guerrero, guardián, superviviente, explorador, artesano; sin bloqueos), hasta 100 por cada una. Bajar un punto devuelve la mitad. Los valores por defecto están balanceados contra Bloodmoon & Bosses (mobs de nivel 1 a 100). Los puntos son permanentes (sobreviven a la muerte) y se compran desde un menú tipo cofre (`/estadisticas`) o por comando. Java 25, Gradle 9.7 + Fabric Loom 1.18, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3. Toda la lógica corre en el servidor: los jugadores no necesitan instalar nada. Quien instala el mismo jar en su cliente suma una pantalla propia opcional (tecla K o botón en el inventario) que habla con el servidor por paquetes de red.

> **Estado:** las 5 fases de [docs/plan.md](docs/plan.md) hechas, más el daño a distancia (0.8.0), la pantalla propia (0.9.0), las 5 estadísticas nuevas con bajar puntos (0.10.0) y las ramas con 25 estadísticas más (0.13.0). La 0.9.1 se probó a mano en un servidor con Soul Link y Bloodmoon & Bosses y anda; lo de la 0.10.0 en adelante todavía no se probó así.

## Cómo obtener contexto

1. Ubicá el **área** en la tabla de abajo.
2. Abrí su `INDICE.md` y leé las descripciones: muchas veces la respuesta ya está ahí.
3. Abrí **sólo** la nota que corresponde. No leas un área entera.

Si no sabés en qué nota está algo, buscá por contenido antes de abrir archivos:

```bash
grep -ril "<palabra clave>" claude/
```

El mismo tema en distintas áreas usa el mismo nombre de archivo (`claude/mod/estadisticas.md` ↔ `claude/tests/estadisticas.md`), así que el grep devuelve el clúster completo.

## Áreas

| Área | Carpeta | Stack | Índice |
|---|---|---|---|
| Mod | `src/main/` | Java 25 · Fabric API (attachments, eventos de jugador y de daño, comandos) · 9 mixins (`ProyectilMixin` y los de las ramas) con MixinExtras · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
| Mod (cliente) | `src/client/` | Pantalla propia opcional · `splitEnvironmentSourceSets` · Fabric screen, key mapping y networking API · `GuiGraphicsExtractor` | [claude/mod/INDICE.md](claude/mod/INDICE.md) (nota [pantalla](claude/mod/pantalla.md)) |
| Tests | `src/gametest/` | Fabric GameTest API · servidor sin ventana · client gametest con ventana para la pantalla | [claude/tests/INDICE.md](claude/tests/INDICE.md) |

## Notas transversales

| Nota | De qué trata |
|---|---|
| [Entorno Java y Gradle](claude/general/entorno-java-gradle.md) | `~/.gradle/gradle.properties` fuerza JDK 17 (React Native) y gana sobre el proyecto · Java 25 se fija con `gradle/gradle-daemon-jvm.properties` + `org.gradle.java.installations.paths` en el `gradle.properties` **global** (la ruta es de cada máquina) · sin eso Loom falla con "requires JVM runtime version 25" |
| [Minecraft 26.x sin ofuscar](claude/general/minecraft-26-sin-ofuscar.md) | Desde 26.1 se usan los nombres de Mojang (Yarn está muerto: los tutoriales viejos tienen otros nombres) · antes de escribir un mixin o usar una API de vanilla, verificá firmas con `javap` sobre el jar de `.gradle/loom-cache` · los atributos ya no llevan `GENERIC_`/`PLAYER_` · al morir, `restoreFrom` pierde los modificadores permanentes · las constantes de entidades están en `EntityTypes` y las flechas en `projectile.arrow` |
| [Probar en el juego](claude/general/probar-en-el-juego.md) | `runServer` necesita `eula=true` y `online-mode=false` en `run/` · `/xp add @s 30 levels` para tener con qué comprar · probá también con un cliente vanilla: el mod no tiene que hacer falta del lado del cliente |

## Features que cruzan áreas

Se agregan con `/docs` a medida que se implementan las fases de [docs/plan.md](docs/plan.md).

| Feature | Mod | Tests |
|---|---|---|
| Comprar y aplicar estadísticas (fase 1) | [estadisticas](claude/mod/estadisticas.md) | [estadisticas](claude/tests/estadisticas.md) |
| Menú de cofre (fase 2) | [menu](claude/mod/menu.md) | [menu](claude/tests/menu.md) |
| Config y recargar (fase 4) | [configuracion](claude/mod/configuracion.md) | [configuracion](claude/tests/configuracion.md) |
| Puntos del grupo con Soul Link (fase 5) | [grupo](claude/mod/grupo.md) | [grupo](claude/tests/grupo.md) |
| Daño a distancia: flechas y tridente (0.8.0) | [estadisticas](claude/mod/estadisticas.md#daño-a-distancia-un-mixin-no-un-atributo) | [estadisticas](claude/tests/estadisticas.md) |
| Pantalla propia del cliente, opcional (0.9.0) | [pantalla](claude/mod/pantalla.md) | [pantalla](claude/tests/pantalla.md) |
| Migración del id `estadisticasrpg` → `herosforge` (0.12.0) | [migracion-de-id](claude/mod/migracion-de-id.md) | [estadisticas](claude/tests/estadisticas.md) |
| Bajar puntos con devolución (0.10.0): comando, click derecho del menú y **-1** de la pantalla | [estadisticas](claude/mod/estadisticas.md#bajar-mejorasbajar) | [estadisticas](claude/tests/estadisticas.md) |
| Regeneración, robo de vida, salto, caída segura y respiración (0.10.0) | [estadisticas](claude/mod/estadisticas.md#regeneración-y-robo-de-vida-sin-atributo) | [estadisticas](claude/tests/estadisticas.md) |
| Ramas y 25 estadísticas más (0.13.0): menú por páginas, pestañas en la pantalla, crítico, esquiva, escudo, metabolismo, imán… | [ramas](claude/mod/ramas.md) | [ramas](claude/tests/ramas.md) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`** (qué estadísticas hay, cuánto dan, comandos y lo que verifican las pruebas): es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compilar no alcanza, los mixins y los eventos recién se aplican en runtime. **Si tocaste `src/client`, `Paquetes` o `InterfazDelCliente`, también `gradlew runClientGameTest`** (abre una ventana) y mirá las capturas de `build/run/clientGameTest/screenshots`.
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`**: minor (`0.1.0` → `0.2.0`) para una feature nueva, patch (`0.1.0` → `0.1.1`) para un arreglo. El esqueleto es `0.0.1`, así que la fase 1 queda en `0.1.0`, como en el plan.
- **No commitees ni pushees salvo que te lo pidan.**
- Mensajes, comentarios y docs en **español rioplatense** (vos), como el resto del proyecto.

## Convenciones de código

- **El id es `herosforge` desde la 0.12.0 (antes `estadisticasrpg`)**: mod id, paquete, jar, config, attachment, `SavedData` y canales de red. Lo guardado con el id viejo se pasa solo (ver [migración del id](claude/mod/migracion-de-id.md)). No lo vuelvas a cambiar sin una migración: le borraría los puntos a todos los mundos y cortaría a los clientes de otra versión.
- Paquete `com.herosforge`; mixins (si hacen falta) en `com.herosforge.mixin`, registrados en `src/main/resources/herosforge.mixins.json`.
- **Métodos y accessors agregados por mixins llevan prefijo `herosforge$`** para no chocar con otros mods.
- **Ningún número de balance escrito a mano en el código** (costo, valor por punto, topes): van en `Configuracion`.
- **Los puntos son la fuente de verdad, no los modificadores:** los atributos se recalculan desde el attachment, nunca al revés.
- Lo del cliente va en `src/client`, paquete `com.herosforge.cliente` (mixins en `com.herosforge.cliente.mixin`, registrados en `herosforge.client.mixins.json`). El servidor no puede depender de nada de ahí.
- **Textos al jugador en castellano armados en el servidor:** sin el mod en el cliente no hay archivo de idioma, así que una clave de traducción propia se vería cruda.
- Indentación con tabs, imports agrupados: java/terceros → `net.fabricmc` → `net.minecraft`.
- Comentarios sólo para el porqué (qué rompería vanilla si no estuviera), no para parafrasear.

## Qué NO va en el grafo

Planes, investigaciones, auditorías y borradores van en `docs/` y se leen a demanda; `claude/` es sólo contexto permanente. El plan del mod por fases está en [docs/plan.md](docs/plan.md).
