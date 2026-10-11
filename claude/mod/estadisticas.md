# Estadísticas: puntos, compra y aplicación

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el jugador reaparece con 10 corazones aunque compró 20; el corazón nuevo aparece vacío y "no anduvo"; la barra de experiencia no baja hasta reconectar; después de una actualización del mod, un jugador pierde **todos** sus puntos.

## La regla: los puntos mandan, los modificadores se recalculan

**`PuntosDelJugador` es la fuente de verdad. Los `AttributeModifier` se derivan de ahí con `Mejoras.aplicar`, nunca al revés.** Vanilla también guarda los modificadores permanentes en el NBT, pero no se confía en ellos: se pierden al morir (ver [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)) y quedan viejos si cambia la config.

**Los puntos se leen con `Mejoras.puntos(jugador)` y se escriben con `Mejoras.guardar`, nunca con el attachment directo:** sin Inventario Compartido van al attachment `EstadisticasRPG.PUNTOS` del jugador, pero con él van a `PuntosDelGrupo`, que es de todo el servidor (ver [Puntos del grupo](grupo.md)).

| Cuándo | Dónde | Qué hace |
|---|---|---|
| Comprar o dar puntos | `Mejoras.sumar` | `guardar` (que hace `aplicar`) (+ `heal` si es vida) |
| Reiniciar | `Mejoras.reiniciar` | `guardar(vacio())` |
| Entrar al servidor | `ServerPlayerEvents.JOIN` (al final de `PlayerList.placeNewPlayer`) → `Mejoras.alEntrar` | `aplicar`: ajusta los modificadores a la config actual (en modo grupo, antes suma los puntos individuales del que entra) |
| Reaparecer o volver del End | `ServerPlayerEvents.COPY_FROM` (al final de `ServerPlayer.restoreFrom`) → `Mejoras.alReaparecer` | copia los puntos del viejo, `aplicar`, y si murió `setHealth(getMaxHealth())` |

`aplicar` es **idempotente**: un modificador por estadística con id fijo (`Estadistica.idModificador()` = `estadisticasrpg:<id>`), puesto con `addOrReplacePermanentModifier` y quitado con `removeModifier(Identifier)` cuando el efecto es 0. Llamarlo de más no apila.

## Respawn

Al morir, `restoreFrom(viejo, false)` hace sólo `assignBaseValues` y después `setHealth(getMaxHealth())` **con el máximo sin los puntos**. Por eso `alReaparecer`:

1. **Lee los puntos del jugador viejo** y los pone en el nuevo. El attachment tiene `copyOnDeath()`, pero así no se depende del orden en que Fabric lo copia respecto de `COPY_FROM`. En modo grupo se saltea este paso: los puntos no están en el jugador.
2. `aplicar`.
3. **Si murió (`vivo == false`), `setHealth(getMaxHealth())` otra vez**, ahora con los puntos. Sin esto reaparece con 20 de vida sobre 40.

Al volver del End (`vivo == true`) vanilla copia modificadores y vida; `aplicar` no rompe nada y la vida actual se respeta.

Comprobado con el gametest `alReaparecerConservaPuntosYVida`: sin el registro de `COPY_FROM`, los puntos sobreviven (por `copyOnDeath`) pero la vida máxima vuelve a 20.

## Vida

- **Subir `MAX_HEALTH` no sube la vida actual.** Al comprar o dar vida, `sumar` cura `porPunto × cantidad`; si no, el corazón nuevo aparece vacío.
- **Bajar `MAX_HEALTH` tampoco baja la vida en el momento:** vanilla la recorta en `LivingEntity.onAttributeUpdated`, que corre desde `refreshDirtyAttributes` en el **próximo tick**. `aplicar` la recorta él mismo al final, así `reiniciar` (o bajar el tope en la config) no deja al jugador ni un tick con 40 de vida sobre 20.
- Al cargar el jugador, `LivingEntity.readAdditionalSaveData` lee `attributes` **antes** que `Health` (verificado con `javap`): alguien con 33 de vida sobre 40 entra con 33, no cortado a 20.

## Comprar (`Mejoras.comprar`)

**Se cobra en puntos de experiencia, siempre los mismos** (`experienciaPorPunto`), no en niveles. Un nivel bajo vale 7 puntos y uno alto más de 100: cobrando niveles, al principio se subía un punto cada dos o tres mobs, y el que juntaba niveles pagaba de más.

**Se compra lo que entra:** `comprados = min(cantidad, tope − puntos, experiencia / experienciaPorPunto)`. Lo que se cobra es exactamente lo que se sube.

**La experiencia del jugador es `Mejoras.experiencia(jugador)`, no `totalExperience`.** `totalExperience` es el puntaje de la pantalla de muerte: encantar no lo baja. `experiencia` suma la de los niveles enteros (`puntosHastaNivel`, la forma cerrada de sumar `Player.getXpNeededForNextLevel`, con las fórmulas que cambian en los niveles 16 y 31) más la barra (`experienceProgress × necesaria`, redondeado porque la barra es `float`). Es `long` por dentro: con `/xp` se llega a niveles cuya experiencia no entra en un `int`.

| `Compra` | Significa |
|---|---|
| `comprados == pedidos`, `resultado == OK` | Compró todo |
| `0 < comprados < pedidos` | Compró una parte; `resultado` dice qué la cortó (`TOPE` o `SIN_EXPERIENCIA`) |
| `comprados == 0` | No compró nada ni cobró; `resultado` dice por qué |

- Si el tope y la experiencia cortan en el mismo número, gana `TOPE` (`margen <= alcanzan`): es lo que el jugador no puede arreglar juntando más experiencia.
- **Se descuenta con `ServerPlayer.giveExperiencePoints(-costo)`.** Con un número negativo, vanilla baja de nivel cada vez que la barra pasa de 0 (verificado con `javap`), y el override de `ServerPlayer` pone `lastSentExp = -1` para que el cliente redibuje la barra. Tocar `experienceLevel` o `experienceProgress` directo no le avisaría al cliente.
- Con Inventario Compartido no hay nada aparte: ese mod reparte la diferencia de experiencia (en puntos) de cada jugador al final del tick.
- **El modo creativo paga igual.** Si fuera gratis, alcanzaría con pasar a creativo, comprar todo y volver.
- `Mejoras.dar` (comando `dar`) no cobra pero **respeta el tope**.
- `Mejoras.mensaje(Compra)` arma el texto para el jugador; lo usan el comando y el menú (ver [Menú de estadísticas](menu.md)).
- **Los efectos van dentro de `comprar`** (`Mejoras.efectos`): si compró, sonido de subir de nivel y partículas `HAPPY_VILLAGER`; si no, el "no" del aldeano. Desde el menú, la barra de acción queda opacada detrás del cofre, y el sonido es lo que avisa que no se compró. `dar` no tiene efectos. Se usa `level.playSound(null, …)`, que lo escuchan todos los que están cerca, incluido el que compra: con el jugador como primer argumento, vanilla se lo saltearía justo a él.

## Config y topes

`Configuracion.POR_DEFECTO` tiene el costo (`experienciaPorPunto`) y una `Mejora(porPunto, tope)` por estadística. **Ningún número de balance va fuera de `Configuracion`.** El archivo `config/estadisticasrpg.json` pisa esos valores: cómo se lee, valida y recarga está en [Configuración](configuracion.md).

**Los valores por defecto están balanceados contra los de Mobs Poderosos** (el mod hermano, con mobs de nivel 1 a 100 según la distancia al spawn): las 9 estadísticas topean en 100 puntos y cada uno cuesta 30 de experiencia, así que todo al máximo son 900 puntos y 27.000 de experiencia. El objetivo es que, con el equipo y los puntos que se esperan en cada zona, un zombi se mate en 5 o 6 golpes en todo el mundo y lo que aguanta el jugador baje de a poco hacia el nivel 100 (tabla en el README). La tabla es sólo por distancia: Mobs Poderosos además suma hasta +20 niveles por los días del mundo y un 5 % de campeones con +10. **Si se cambia un número de acá, hay que mirar el otro mod**, y al revés. Lo que más desbalancea:

- **La armadura.** Vanilla frena como máximo el 80 % del daño, y con netherita (20) ya se está cerca: +10 llevaba al tope y volvía al jugador casi inmortal. Por eso +8.
- **Cobrar en niveles.** Con 1 nivel por punto se compraba todo de a un nivel, a 7 de experiencia cada uno, y se llegaba al máximo cerca del spawn, antes de que los mobs se pusieran difíciles.

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
- [Configuración](configuracion.md)
- [Puntos del grupo](grupo.md): con Inventario Compartido los puntos van a `data/estadisticasrpg/puntos_del_grupo.dat`, no al attachment
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
