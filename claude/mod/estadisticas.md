# Estadísticas: puntos, compra y aplicación

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el jugador reaparece con 10 corazones aunque compró 20; el corazón nuevo aparece vacío y "no anduvo"; la barra de experiencia no baja hasta reconectar; después de una actualización del mod, un jugador pierde **todos** sus puntos.

## La regla: los puntos mandan, los modificadores se recalculan

**`PuntosDelJugador` (el attachment `EstadisticasRPG.PUNTOS`) es la fuente de verdad. Los `AttributeModifier` se derivan de ahí con `Mejoras.aplicar`, nunca al revés.** Vanilla también guarda los modificadores permanentes en el NBT, pero no se confía en ellos: se pierden al morir (ver [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)) y quedan viejos si cambia la config.

| Cuándo | Dónde | Qué hace |
|---|---|---|
| Comprar o dar puntos | `Mejoras.sumar` | `setAttached` + `aplicar` (+ `heal` si es vida) |
| Reiniciar | `Mejoras.reiniciar` | `setAttached(vacio())` + `aplicar` |
| Entrar al servidor | `ServerPlayerEvents.JOIN` (al final de `PlayerList.placeNewPlayer`) | `aplicar`: ajusta los modificadores a la config actual |
| Reaparecer o volver del End | `ServerPlayerEvents.COPY_FROM` (al final de `ServerPlayer.restoreFrom`) → `Mejoras.alReaparecer` | copia los puntos del viejo, `aplicar`, y si murió `setHealth(getMaxHealth())` |

`aplicar` es **idempotente**: un modificador por estadística con id fijo (`Estadistica.idModificador()` = `estadisticasrpg:<id>`), puesto con `addOrReplacePermanentModifier` y quitado con `removeModifier(Identifier)` cuando el efecto es 0. Llamarlo de más no apila.

## Respawn

Al morir, `restoreFrom(viejo, false)` hace sólo `assignBaseValues` y después `setHealth(getMaxHealth())` **con el máximo sin los puntos**. Por eso `alReaparecer`:

1. **Lee los puntos del jugador viejo** y los pone en el nuevo. El attachment tiene `copyOnDeath()`, pero así no se depende del orden en que Fabric lo copia respecto de `COPY_FROM`.
2. `aplicar`.
3. **Si murió (`vivo == false`), `setHealth(getMaxHealth())` otra vez**, ahora con los puntos. Sin esto reaparece con 20 de vida sobre 40.

Al volver del End (`vivo == true`) vanilla copia modificadores y vida; `aplicar` no rompe nada y la vida actual se respeta.

Comprobado con el gametest `alReaparecerConservaPuntosYVida`: sin el registro de `COPY_FROM`, los puntos sobreviven (por `copyOnDeath`) pero la vida máxima vuelve a 20.

## Vida

- **Subir `MAX_HEALTH` no sube la vida actual.** Al comprar o dar vida, `sumar` cura `porPunto × cantidad`; si no, el corazón nuevo aparece vacío.
- **Bajar `MAX_HEALTH` tampoco baja la vida en el momento:** vanilla la recorta en `LivingEntity.onAttributeUpdated`, que corre desde `refreshDirtyAttributes` en el **próximo tick**. `aplicar` la recorta él mismo al final, así `reiniciar` (o bajar el tope en la config) no deja al jugador ni un tick con 40 de vida sobre 20.
- Al cargar el jugador, `LivingEntity.readAdditionalSaveData` lee `attributes` **antes** que `Health` (verificado con `javap`): alguien con 33 de vida sobre 40 entra con 33, no cortado a 20.

## Comprar (`Mejoras.comprar`)

**Se compra lo que entra:** `comprados = min(cantidad, tope − puntos, experienceLevel / costoPorPunto)`. Lo que se cobra es exactamente lo que se sube.

| `Compra` | Significa |
|---|---|
| `comprados == pedidos`, `resultado == OK` | Compró todo |
| `0 < comprados < pedidos` | Compró una parte; `resultado` dice qué la cortó (`TOPE` o `SIN_NIVELES`) |
| `comprados == 0` | No compró nada ni cobró; `resultado` dice por qué |

- Si el tope y los niveles cortan en el mismo número, gana `TOPE` (`margen <= alcanzan`): es lo que el jugador no puede arreglar juntando más niveles.
- **Los niveles se descuentan con `ServerPlayer.giveExperienceLevels`**, que pone `lastSentExp = -1`. Si se tocara `experienceLevel` directo, la barra del cliente no bajaría: vanilla sólo la reenvía cuando cambia `totalExperience`, y bajar niveles no lo cambia.
- **El modo creativo paga igual.** Si fuera gratis, alcanzaría con pasar a creativo, comprar todo y volver.
- `Mejoras.dar` (comando `dar`) no cobra pero **respeta el tope**.
- `Mejoras.mensaje(Compra)` arma el texto para el jugador; lo usan el comando y el menú (ver [Menú de estadísticas](menu.md)).

## Config y topes

`Configuracion.POR_DEFECTO` tiene el costo y una `Mejora(porPunto, tope)` por estadística. **Ningún número de balance va fuera de `Configuracion`.**

- **Si se baja un tope, los puntos de más se conservan pero no suman:** `Mejora.efecto` usa `min(puntos, tope)`. Si se vuelve a subir el tope, vuelven a contar sin que el jugador pague de nuevo.
- `Configuracion.de(estadistica)` devuelve `null` si falta en el mapa, y entonces `aplicar` y `comprar` revientan con NPE. **Cada valor de `Estadistica` tiene que estar en `POR_DEFECTO`.**

## Formato en disco

El attachment va en el NBT del jugador (`playerdata/<uuid>.dat`), bajo los attachments de Fabric, como `estadisticasrpg:puntos` → `{vida: 3, danio: 7, ...}`. Sólo se guardan las estadísticas con puntos.

**El codec es `Map<String, Integer>` y no un codec del enum, a propósito.** Con un codec de enum, un id que no existe (una estadística renombrada, sacada o de una versión más nueva del mod) haría que el attachment decodifique con error. Fabric (`AttachmentSerializingImpl`) saltea los attachments que fallan, con un `promotePartial` que loguea "Skipping invalid attachments". Que se salven o no los demás puntos depende del resultado parcial de DFU, y no vale la pena apostar los puntos de un jugador a eso. `PuntosDelJugador.desdeIds` decodifica siempre bien e ignora los ids desconocidos (gametest `unaEstadisticaDesconocidaNoBorraLasDemas`). Por eso **un id de `Estadistica` no se renombra nunca**: si hay que cambiarle el nombre visible, se cambia `nombre`, no `id`.

`PuntosDelJugador` es **inmutable**: Fabric sólo registra un cambio con `setAttached`. Mutar el mapa adentro no se guardaría.

## Comando

`ComandoEstadisticas` registra `/estadisticas` (`ver`, `subir`, `dar`, `reiniciar`) y el alias `/stats`.

- **El alias es un `redirect`, y un redirect no hereda el `executes` del nodo destino:** `/stats` solo tiene su propio `.executes(ver)`. Sin eso, `/stats` sin argumentos daría error de sintaxis.
- Los permisos van con `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`. Con `ver`, el permiso de op está en el argumento `jugador`, así cualquiera puede ver lo suyo.
- La estadística es un `StringArgumentType.word()` con sugerencias de los ids, no un argumento propio: un tipo de argumento nuevo obligaría a tener el mod en el cliente.
- Todos los textos son `Component.literal` en castellano: sin el mod en el cliente no hay archivo de idioma.

## Qué atributo toca cada una y quién lo usa

| Estadística (`id`) | Atributo(s) | Operación | Lo calcula |
|---|---|---|---|
| Vida (`vida`) | `MAX_HEALTH` | `ADD_VALUE` | servidor (el cliente recibe la vida) |
| Daño (`danio`) | `ATTACK_DAMAGE` | `ADD_VALUE` | servidor |
| Armadura (`armadura`) | `ARMOR` | `ADD_VALUE` | servidor (el cliente la dibuja: es sincronizable) |
| Velocidad (`velocidad`) | `MOVEMENT_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (predice el movimiento) |
| Velocidad de ataque (`velocidad_ataque`) | `ATTACK_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (la barra de recarga del golpe) y servidor |
| Minado (`minado`) | `BLOCK_BREAK_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (el progreso de romper) y servidor |
| Alcance (`alcance`) | `BLOCK_INTERACTION_RANGE` **y** `ENTITY_INTERACTION_RANGE` | `ADD_VALUE` | **cliente** (a qué apunta) y servidor (valida la distancia) |
| Resistencia al empuje (`empuje`) | `KNOCKBACK_RESISTANCE` | `ADD_VALUE` | servidor (manda el empuje ya calculado) |
| Suerte (`suerte`) | `LUCK` | `ADD_VALUE` | servidor |

- **Lo que calcula el cliente tiene que ser un atributo `isClientSyncable()`.** Si no, el servidor tendría los puntos pero el cliente vanilla minaría, caminaría o apuntaría como siempre. Los cinco de la tabla lo son en 26.3, y lo verifica el gametest `losAtributosQueUsaElClienteSeSincronizan`.
- **El alcance usa los dos rangos a la vez.** Con sólo el de bloques, el jugador llegaría a un bloque lejano pero no a un mob a la misma distancia. Como `Estadistica.atributos()` es una lista, `aplicar` pone el mismo modificador en los dos.
- **La suerte se nota sobre todo al pescar.** `FishingHook` usa la suerte de Suerte marina más `Player.getLuck()`, que es el atributo `LUCK`: con +3 es como tener otra Suerte marina III. Las tablas de botín sólo la usan en las entradas con `quality`, y **no está verificado** que alguna tabla de cofres de vanilla las tenga. Por eso el README dice "al pescar" y no promete nada de los cofres.

## Agregar una estadística

1. Un valor en `Estadistica`: `id` (para siempre, ver Formato en disco), nombre, ítem del menú (`icono`), operación, atributo(s) y cómo se describe el efecto.
2. Su `Mejora` en `Configuracion.POR_DEFECTO`, o NPE (lo atrapa el gametest `cadaEstadisticaTieneConfigYSlot`).
3. Si es vida, o algo que necesite un efecto inmediato al comprar, se agrega en `Mejoras.sumar`.
4. Si el cliente usa el atributo, agregalo a `losAtributosQueUsaElClienteSeSincronizan` (ver la tabla de arriba).
5. Una fila en la tabla del `README.md` y un gametest con el valor del atributo. El menú tiene lugar para 9 en la fila del medio: la décima pide cambiar `MenuDeEstadisticas.slotDe` (ver [Menú de estadísticas](menu.md)).

Con `ADD_MULTIPLIED_BASE`, el porcentaje se aplica sobre la base **más todos los `ADD_VALUE`**, incluido el del arma (ver [docs/plan.md](../../docs/plan.md)).

## Ver también

- [Tests de estadísticas](../tests/estadisticas.md)
- [Menú de estadísticas](menu.md)
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
