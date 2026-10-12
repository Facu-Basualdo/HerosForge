# Estadísticas: puntos, compra y aplicación

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el jugador reaparece con 10 corazones aunque compró 20; el corazón nuevo aparece vacío y "no anduvo"; la barra de experiencia no baja hasta reconectar; después de una actualización del mod, un jugador pierde **todos** sus puntos; con los mobs de nivel alto, el arco no sirve para nada aunque el jugador tenga el daño al máximo.

## La regla: los puntos mandan, los modificadores se recalculan

**`PuntosDelJugador` es la fuente de verdad. Los `AttributeModifier` se derivan de ahí con `Mejoras.aplicar`, nunca al revés.** Vanilla también guarda los modificadores permanentes en el NBT, pero no se confía en ellos: se pierden al morir (ver [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)) y quedan viejos si cambia la config.

**Los puntos se leen con `Mejoras.puntos(jugador)` y se escriben con `Mejoras.guardar`, nunca con el attachment directo:** sin Soul Link van al attachment `HerosForge.PUNTOS` del jugador, pero con él van a `PuntosDelGrupo`, que es de todo el servidor (ver [Puntos del grupo](grupo.md)).

| Cuándo | Dónde | Qué hace |
|---|---|---|
| Comprar o dar puntos | `Mejoras.sumar` | `guardar` (que hace `aplicar`) (+ `heal` si es vida) |
| Bajar puntos | `Mejoras.bajar` | `guardar` con menos puntos y devuelve experiencia (ver Bajar) |
| Reiniciar | `Mejoras.reiniciar` | `guardar(vacio())` |
| Entrar al servidor | `ServerPlayerEvents.JOIN` (al final de `PlayerList.placeNewPlayer`) → `Mejoras.alEntrar` | `aplicar`: ajusta los modificadores a la config actual (en modo grupo, antes suma los puntos individuales del que entra) |
| Reaparecer o volver del End | `ServerPlayerEvents.COPY_FROM` (al final de `ServerPlayer.restoreFrom`) → `Mejoras.alReaparecer` | copia los puntos del viejo, `aplicar`, y si murió `setHealth(getMaxHealth())` |

`aplicar` es **idempotente**: un modificador por estadística con id fijo (`Estadistica.idModificador()` = `herosforge:<id>`), puesto con `addOrReplacePermanentModifier` y quitado con `removeModifier(Identifier)` cuando el efecto es 0. Llamarlo de más no apila.

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
- Con Soul Link no hay nada aparte: ese mod reparte la diferencia de experiencia (en puntos) de cada jugador al final del tick.
- **El modo creativo paga igual.** Si fuera gratis, alcanzaría con pasar a creativo, comprar todo y volver.
- `Mejoras.dar` (comando `dar`) no cobra pero **respeta el tope**.
- `Mejoras.mensaje(Compra)` arma el texto para el jugador; lo usan el comando y el menú (ver [Menú de estadísticas](menu.md)).
- **Los efectos van dentro de `comprar`** (`Mejoras.efectos`): si compró, sonido de subir de nivel y partículas `HAPPY_VILLAGER`; si no, el "no" del aldeano. Desde el menú, la barra de acción queda opacada detrás del cofre, y el sonido es lo que avisa que no se compró. `dar` no tiene efectos. Se usa `level.playSound(null, …)`, que lo escuchan todos los que están cerca, incluido el que compra: con el jugador como primer argumento, vanilla se lo saltearía justo a él.

## Bajar (`Mejoras.bajar`)

**Bajar un punto devuelve `Configuracion.devolucionPorPunto()` de experiencia: `floor(experienciaPorPunto × devolucionAlBajar)`, 15 de 30 por defecto.** Se baja lo que hay (`min(cantidad, puntos)`), y la devolución es por los bajados, no por los pedidos.

- **La devolución es menos que el costo a propósito.** Con 1 (o más) bajar y volver a subir sería gratis (o fabricaría experiencia): los puntos se moverían según la pelea. Por eso la config no acepta más de 1 (ver [Configuración](configuracion.md)).
- Se devuelve con `giveExperiencePoints` positivo, el mismo camino que el cobro: el cliente redibuja la barra.
- **Bajar vida no deja vida de más:** `guardar` → `aplicar`, que recorta la vida contra el máximo nuevo en el momento (ver Vida).
- Con los puntos del grupo baja los de todos, y la experiencia la recibe el que bajó: Soul Link la reparte como cualquier otra.
- `Devolucion(estadistica, pedidos, bajados, experiencia)` y `Mejoras.mensaje(Devolucion)` ("Vida −2 puntos: te devolvió 30 de experiencia"), como `Compra`. Con 0 bajados suena el "no" del aldeano; si bajó, el sonido de levantar un orbe.
- La cuenta va en `long`: con un tope enorme y la devolución alta no entra en un `int`.

## Config y topes

`Configuracion.POR_DEFECTO` tiene el costo (`experienciaPorPunto`) y una `Mejora(porPunto, tope)` por estadística. **Ningún número de balance va fuera de `Configuracion`.** El archivo `config/herosforge.json` pisa esos valores: cómo se lee, valida y recarga está en [Configuración](configuracion.md).

**Los valores por defecto están balanceados contra los de Bloodmoon & Bosses** (el mod hermano, con mobs de nivel 1 a 100 según la distancia al spawn): las 40 estadísticas topean en 100 puntos y cada uno cuesta 30 de experiencia, así que todo al máximo son 4000 puntos y 120.000 de experiencia: nadie llena todo y hay que elegir. El objetivo es que, con el equipo y los puntos que se esperan en cada zona, un zombi se mate en 5 o 6 golpes en todo el mundo y lo que aguanta el jugador baje de a poco hacia el nivel 100 (tabla en el README). La tabla es sólo por distancia: Bloodmoon & Bosses además suma hasta +20 niveles por los días del mundo y un 5 % de campeones con +10. **Si se cambia un número de acá, hay que mirar el otro mod**, y al revés. Lo que más desbalancea:

- **La armadura.** Vanilla frena como máximo el 80 % del daño, y con netherita (20) ya se está cerca: +10 llevaba al tope y volvía al jugador casi inmortal. Por eso +8.
- **El daño a distancia.** Las flechas no escalan con `Daño`, y un zombi de nivel 100 (180 de vida) pedía ~11 flechas de Poder V. Con +1 % por punto, al máximo son 5 o 6, como con la espada.
- **La regeneración y el robo de vida** curan en plena pelea: al máximo, 0,3 de vida por segundo (9 corazones por minuto) y el 10 % del daño. Un zombi de nivel 100 pega 15: ninguna de las dos le gana a eso, pero sí acortan la espera entre peleas.
- **El salto:** +20 % de fuerza salta 1,8 bloques. Con más de ~+25 % se pasa de 2 y se saltean las paredes de 2 bloques de cualquier base.
- **Cobrar en niveles.** Con 1 nivel por punto se compraba todo de a un nivel, a 7 de experiencia cada uno, y se llegaba al máximo cerca del spawn, antes de que los mobs se pusieran difíciles.
- **La esquiva** topea en 15 %: contra hordas, más que eso ya es invulnerabilidad. **La sabiduría** en +25 %: la experiencia es la moneda de los puntos, así que se retroalimenta. Las razones de las demás de la 0.13.0 están en los comentarios de `POR_DEFECTO` (ver [Ramas](ramas.md)).

- **Si se baja un tope, los puntos de más se conservan pero no suman:** `Mejora.efecto` usa `min(puntos, tope)`. Si se vuelve a subir el tope, vuelven a contar sin que el jugador pague de nuevo.
- `Configuracion.de(estadistica)` devuelve `null` si falta en el mapa, y entonces `aplicar` y `comprar` revientan con NPE. **Cada valor de `Estadistica` tiene que estar en `POR_DEFECTO`.**

## Formato en disco

El attachment va en el NBT del jugador (`playerdata/<uuid>.dat`), bajo los attachments de Fabric, como `herosforge:puntos` → `{vida: 3, danio: 7, ...}`. Sólo se guardan las estadísticas con puntos.

**El codec es `Map<String, Integer>` y no un codec del enum, a propósito.** Con un codec de enum, un id que no existe (una estadística renombrada, sacada o de una versión más nueva del mod) haría que el attachment decodifique con error. Fabric (`AttachmentSerializingImpl`) saltea los attachments que fallan, con un `promotePartial` que loguea "Skipping invalid attachments". Que se salven o no los demás puntos depende del resultado parcial de DFU, y no vale la pena apostar los puntos de un jugador a eso. `PuntosDelJugador.desdeIds` decodifica siempre bien e ignora los ids desconocidos (gametest `unaEstadisticaDesconocidaNoBorraLasDemas`). Por eso **un id de `Estadistica` no se renombra nunca**: si hay que cambiarle el nombre visible, se cambia `nombre`, no `id`.

`PuntosDelJugador` es **inmutable**: Fabric sólo registra un cambio con `setAttached`. Mutar el mapa adentro no se guardaría.

## Comando

`ComandoEstadisticas` registra `/estadisticas` (`ver`, `subir`, `bajar`, `dar`, `reiniciar`, `recargar`) y el alias `/stats`. `bajar` es para todos, como `subir`.

- **El alias es un `redirect`, y un redirect no hereda el `executes` del nodo destino:** `/stats` solo tiene su propio `.executes(ver)`. Sin eso, `/stats` sin argumentos daría error de sintaxis.
- Los permisos van con `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`. Con `ver`, el permiso de op está en el argumento `jugador`, así cualquiera puede ver lo suyo.
- La estadística es un `StringArgumentType.word()` con sugerencias de los ids, no un argumento propio: un tipo de argumento nuevo obligaría a tener el mod en el cliente.
- Todos los textos son `Component.literal` en castellano: sin el mod en el cliente no hay archivo de idioma.

## Qué atributo toca cada una y quién lo usa

| Estadística (`id`) | Atributo(s) | Operación | Lo calcula |
|---|---|---|---|
| Vida (`vida`) | `MAX_HEALTH` | `ADD_VALUE` | servidor (el cliente recibe la vida) |
| Daño (`danio`) | `ATTACK_DAMAGE` | `ADD_VALUE` | servidor |
| Daño a distancia (`danio_distancia`) | **ninguno**: `ProyectilMixin` | multiplica el daño final | servidor |
| Armadura (`armadura`) | `ARMOR` | `ADD_VALUE` | servidor (el cliente la dibuja: es sincronizable) |
| Velocidad (`velocidad`) | `MOVEMENT_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (predice el movimiento) |
| Velocidad de ataque (`velocidad_ataque`) | `ATTACK_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (la barra de recarga del golpe) y servidor |
| Minado (`minado`) | `BLOCK_BREAK_SPEED` | `ADD_MULTIPLIED_BASE` | **cliente** (el progreso de romper) y servidor |
| Alcance (`alcance`) | `BLOCK_INTERACTION_RANGE` **y** `ENTITY_INTERACTION_RANGE` | `ADD_VALUE` | **cliente** (a qué apunta) y servidor (valida la distancia) |
| Resistencia al empuje (`empuje`) | `KNOCKBACK_RESISTANCE` | `ADD_VALUE` | servidor (manda el empuje ya calculado) |
| Suerte (`suerte`) | `LUCK` | `ADD_VALUE` | servidor |
| Regeneración (`regeneracion`) | **ninguno**: `Curacion.regenerar` | cura `efecto` cada segundo | servidor |
| Robo de vida (`robo_de_vida`) | **ninguno**: `Curacion.alPegar` | cura `daño × efecto` | servidor |
| Salto (`salto`) | `JUMP_STRENGTH` | `ADD_MULTIPLIED_BASE` | **cliente** (predice el salto) |
| Caída segura (`caida`) | `SAFE_FALL_DISTANCE` | `ADD_VALUE` | servidor (el daño de caída) |
| Respiración (`respiracion`) | `OXYGEN_BONUS` | `ADD_VALUE` | servidor (el aire; el cliente recibe las burbujas) |

Las 25 de la 0.13.0 (dureza, escudo, paso, ligereza, crítico, imán…) tienen su propia tabla en [Ramas](ramas.md): 11 son atributos y 14 las aplican `Combate`, `Supervivencia` y `Oficios` con mixins.

- **Lo que calcula el cliente tiene que ser un atributo `isClientSyncable()`.** Si no, el servidor tendría los puntos pero el cliente vanilla minaría, caminaría o apuntaría como siempre. Los de la tabla lo son en 26.3 (también `JUMP_STRENGTH`, y los de paso, nado, sigilo, ligereza y minado submarino), y lo verifica el gametest `losAtributosQueUsaElClienteSeSincronizan`.
- **El alcance usa los dos rangos a la vez.** Con sólo el de bloques, el jugador llegaría a un bloque lejano pero no a un mob a la misma distancia. Como `Estadistica.atributos()` es una lista, `aplicar` pone el mismo modificador en los dos.
- **La suerte se nota sobre todo al pescar.** `FishingHook` usa la suerte de Suerte marina más `Player.getLuck()`, que es el atributo `LUCK`: con +3 es como tener otra Suerte marina III. Las tablas de botín sólo la usan en las entradas con `quality`, y **no está verificado** que alguna tabla de cofres de vanilla las tenga. Por eso el README dice "al pescar" y no promete nada de los cofres.

## Daño a distancia: un mixin, no un atributo

**Las flechas y el tridente lanzado no usan `ATTACK_DAMAGE`**, y vanilla no tiene un atributo para el daño de proyectiles ni Fabric un evento que deje cambiar el daño. Por eso `DANIO_DISTANCIA` se construye **sin atributos** (`atributos()` vacía, `operacion()` `null`): `aplicar` no le pone nada, y los puntos se leen al pegar.

`ProyectilMixin` es un `@ModifyArg` sobre el `float` de `Entity.hurtOrSimulate(DamageSource, float)` dentro de `onHitEntity`, y lo multiplica por `Mejoras.multiplicadorADistancia(getOwner())`:

| Clase | Por qué |
|---|---|
| `AbstractArrow` | Flechas de arco y de ballesta (también las espectrales). `onHitEntity` hace `ceil(velocidad × modifyDamage(baseDamage))` + el crítico y llama **una sola vez** a `hurtOrSimulate` (verificado con `javap`) |
| `ThrownTrident` | **Sobrescribe `onHitEntity` sin llamar al de `AbstractArrow`** (8 + encantamientos): sin su propio target, el tridente lanzado no escalaría |

- **Se multiplica el daño final**, con velocidad, Poder y crítico ya sumados: el arco, la ballesta y el tridente escalan en la misma proporción. Los fuegos artificiales de la ballesta no pasan por acá.
- **Sólo si el dueño es un `ServerPlayer`.** Esqueletos, dispensadores o una flecha cuyo dueño se desconectó (`getOwner()` `null`) pegan como en vanilla.
- `multiplicadorADistancia` usa `Mejoras.puntos`: con Soul Link toma los del grupo sin nada aparte.
- `defaultRequire: 1` en `herosforge.mixins.json`: si un cambio de Minecraft mueve la llamada, el juego no arranca en vez de dejar la estadística sin efecto en silencio.

## Regeneración y robo de vida: sin atributo

Vanilla no tiene un atributo que cure: la regeneración natural depende del hambre, y nada cura al pegar. Las dos se construyen como el daño a distancia, sin atributos, y las aplica `Curacion` (registrada en `HerosForge.onInitialize`):

| Estadística | Cuándo | Qué hace |
|---|---|---|
| Regeneración | `END_SERVER_TICK`, cada 20 ticks (`getTickCount() % 20`) | `heal(efecto)` a cada jugador vivo, no creativo ni espectador, con la vida no llena |
| Robo de vida | `ServerLivingEntityEvents.AFTER_DAMAGE` | si `fuente.getEntity()` es un `ServerPlayer` vivo distinto del herido y el golpe no se bloqueó, `heal(danio × efecto)` |

- **`porPunto` de la regeneración es vida por segundo en medios corazones**, como `MAX_HEALTH`, pero `describir` lo muestra **por minuto** (`× 30` corazones): 0,003 por segundo con `numero` (dos decimales) daría "+0 corazones".
- **`getEntity()` y no `getDirectEntity()`:** con flechas y tridentes, el directo es el proyectil y el dueño es el jugador. Así el robo de vida vale también a distancia.
- **`AFTER_DAMAGE` no corre si el golpe mató** (lo dice Fabric): el golpe final no cura. Por eso el test usa un gólem, que no muere de un golpe.
- **Con los puntos del grupo, la regeneración cura a uno solo** (el primero de la lista que puede): Soul Link suma la curación de cada jugador a la vida compartida, y si curaran todos, el grupo se regeneraría una vez por jugador conectado. El robo de vida cura sólo al que pegó, y eso Soul Link lo reparte bien.

## Agregar una estadística

1. Un valor en `Estadistica`, en el bloque de su rama: `id` (para siempre, ver Formato en disco), nombre, `Rama` (la página del menú y la pestaña de la pantalla; hasta 9 por rama), ítem del menú (`icono`), operación, atributo(s) y cómo se describe el efecto. Si el nombre no entra en media pantalla (más de ~85 px), un `nombreCorto`. Si no es un atributo, el constructor corto (sin operación ni atributos) y el efecto se lee con `Mejoras.efecto` o `Mejoras.fraccion` donde haga falta (ver [Ramas](ramas.md)). Si mejora al bajar el atributo, el constructor con `signo` −1.
2. Su `Mejora` en `Configuracion.POR_DEFECTO`, o NPE (lo atrapa el gametest `cadaEstadisticaTieneConfigYSlot`).
3. Si es vida, o algo que necesite un efecto inmediato al comprar, se agrega en `Mejoras.sumar`.
4. Si el cliente usa el atributo, agregalo a `losAtributosQueUsaElClienteSeSincronizan` (ver la tabla de arriba).
5. Una fila en la tabla del `README.md` y un gametest con el valor del atributo. El orden del enum es el del menú (ver [Menú de estadísticas](menu.md)): reordenarlo no rompe nada guardado, porque el codec es por id. `POR_DEFECTO` usa `Map.ofEntries`: `Map.of` no pasa de 10 pares.

Con `ADD_MULTIPLIED_BASE`, el porcentaje se aplica sobre la base **más todos los `ADD_VALUE`**, incluido el del arma (ver [docs/plan.md](../../docs/plan.md)).

## Ver también

- [Tests de estadísticas](../tests/estadisticas.md)
- [Ramas y las estadísticas sin atributo](ramas.md): las 25 de la 0.13.0
- [Menú de estadísticas](menu.md)
- [Configuración](configuracion.md)
- [Puntos del grupo](grupo.md): con Soul Link los puntos van a `data/herosforge/puntos_del_grupo.dat`, no al attachment, y la regeneración cura a uno solo
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
