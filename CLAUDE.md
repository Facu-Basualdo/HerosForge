# Estadísticas RPG — mod de Minecraft

Mod de Fabric para **Minecraft Java 26.3** para subir las estadísticas del jugador estilo RPG: se paga **1 nivel de experiencia** por **1 punto** de vida, daño, armadura, velocidad u otra estadística, con un tope por cada una. Los puntos son permanentes (sobreviven a la muerte) y se compran desde un menú tipo cofre (`/estadisticas`) o por comando. Java 25, Gradle 9.7 + Fabric Loom 1.18, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3. Toda la lógica corre en el servidor: los jugadores no necesitan instalar nada.

> **Estado:** recién creado. Hay un esqueleto que compila y un gametest de humo; el diseño y las fases están en [docs/plan.md](docs/plan.md). Leelo antes de escribir la primera feature.

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
| Mod | `src/main/` | Java 25 · Fabric API (attachments, eventos de jugador, comandos) · nombres oficiales de Mojang | [claude/mod/INDICE.md](claude/mod/INDICE.md) |
| Tests | `src/gametest/` | Fabric GameTest API · servidor sin ventana | [claude/tests/INDICE.md](claude/tests/INDICE.md) |

## Notas transversales

| Nota | De qué trata |
|---|---|
| [Entorno Java y Gradle](claude/general/entorno-java-gradle.md) | `~/.gradle/gradle.properties` fuerza JDK 17 (React Native) y gana sobre el proyecto · Java 25 se fija con `gradle/gradle-daemon-jvm.properties` + `org.gradle.java.installations.paths` en el `gradle.properties` **global** (la ruta es de cada máquina) · sin eso Loom falla con "requires JVM runtime version 25" |
| [Minecraft 26.x sin ofuscar](claude/general/minecraft-26-sin-ofuscar.md) | Desde 26.1 se usan los nombres de Mojang (Yarn está muerto: los tutoriales viejos tienen otros nombres) · antes de escribir un mixin o usar una API de vanilla, verificá firmas con `javap` sobre el jar de `.gradle/loom-cache` · los atributos ya no llevan `GENERIC_`/`PLAYER_` · al morir, `restoreFrom` pierde los modificadores permanentes |
| [Probar en el juego](claude/general/probar-en-el-juego.md) | `runServer` necesita `eula=true` y `online-mode=false` en `run/` · `/xp add @s 30 levels` para tener con qué comprar · probá también con un cliente vanilla: el mod no tiene que hacer falta del lado del cliente |

## Features que cruzan áreas

Se agregan con `/docs` a medida que se implementan las fases de [docs/plan.md](docs/plan.md).

| Feature | Mod | Tests |
|---|---|---|
| Comprar y aplicar estadísticas (fase 1) | [estadisticas](claude/mod/estadisticas.md) | [estadisticas](claude/tests/estadisticas.md) |

## Reglas de workflow

- **Después de cada feature corré `/docs`** para actualizar las notas según el diff, y `node scripts/verificar-docs.mjs` tiene que dar OK.
- **Si la feature se nota en el juego, actualizá el `README.md`** (qué estadísticas hay, cuánto dan, comandos y lo que verifican las pruebas): es lo que lee quien instala el mod, y `/docs` sólo toca `claude/`.
- **Antes de dar algo por terminado corré `gradlew runGameTest`**: compilar no alcanza, los mixins y los eventos recién se aplican en runtime.
- **Con cada cambio que se pueda notar en el juego, subí `version` en `gradle.properties`**: minor (`0.1.0` → `0.2.0`) para una feature nueva, patch (`0.1.0` → `0.1.1`) para un arreglo. El esqueleto es `0.0.1`, así que la fase 1 queda en `0.1.0`, como en el plan.
- **No commitees ni pushees salvo que te lo pidan.**
- Mensajes, comentarios y docs en **español rioplatense** (vos), como el resto del proyecto.

## Convenciones de código

- Paquete `com.estadisticasrpg`; mixins (si hacen falta) en `com.estadisticasrpg.mixin`, registrados en `src/main/resources/estadisticasrpg.mixins.json`.
- **Métodos y accessors agregados por mixins llevan prefijo `estadisticasrpg$`** para no chocar con otros mods.
- **Ningún número de balance escrito a mano en el código** (costo, valor por punto, topes): van en `Configuracion`.
- **Los puntos son la fuente de verdad, no los modificadores:** los atributos se recalculan desde el attachment, nunca al revés.
- **Textos al jugador en castellano armados en el servidor:** sin el mod en el cliente no hay archivo de idioma, así que una clave de traducción propia se vería cruda.
- Indentación con tabs, imports agrupados: java/terceros → `net.fabricmc` → `net.minecraft`.
- Comentarios sólo para el porqué (qué rompería vanilla si no estuviera), no para parafrasear.

## Qué NO va en el grafo

Planes, investigaciones, auditorías y borradores van en `docs/` y se leen a demanda; `claude/` es sólo contexto permanente. El plan del mod por fases está en [docs/plan.md](docs/plan.md).
