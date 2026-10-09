# Plan — Mod "Estadísticas RPG" (subir estadísticas pagando niveles de experiencia)

## Contexto

Querés un mod de Fabric, aparte de Inventario Compartido y Mobs Poderosos, para mejorar las estadísticas del jugador **estilo RPG**: pagás **1 nivel de experiencia** y subís **1 punto** de una estadística (vida, daño, armadura, velocidad…). Los puntos son permanentes y cada estadística tiene un tope.

Decisiones tomadas por defecto (se pueden cambiar antes de la fase 1; todas quedan en `Configuracion`):

- **Costo** = 1 nivel por punto, fijo. Igual que al encantar, un nivel "vale" más puntos de experiencia cuanto más alto estás (de 1 a 0 son 7 puntos; de 30 a 29, 107). Es lo esperable en Minecraft y no se compensa.
- **Interfaz** = un **menú tipo cofre armado en el servidor** (`/estadisticas`): un ítem por estadística, click = comprar un punto. Además, comandos para todo. **No hace falta el mod en el cliente**, igual que en los otros dos mods.
- **Al morir** = los puntos se conservan (es progresión, no un buff).
- **Creativo** = paga igual. Si fuera gratis, alcanzaría con pasar a creativo, comprar todo y volver a supervivencia. Para probar está `/estadisticas dar` (op).
- **Repo** = `C:\ReposGit\ModsMinecraft\EstadisticasRPG`, con la misma base que Mobs Poderosos.

## Paso 0 — Repo (hecho)

- Base copiada de `MobsPoderosos`: `build.gradle`, `settings.gradle`, `gradle/` (con `gradle-daemon-jvm.properties` para Java 25), `gradlew`, `.gitignore`, `.gitattributes`, `LICENSE`, `.claude/commands/docs.md`, `scripts/verificar-docs.mjs`.
- `rootProject.name = 'estadisticasrpg'`, `group=com.estadisticasrpg`, `version=0.0.1` (el esqueleto no hace nada en el juego; la fase 1 sube el minor a `0.1.0`, como pide la regla del `CLAUDE.md`), `modId = "estadisticasrpg-test"`.
- `CLAUDE.md`, notas transversales en `claude/general/`, índices vacíos en `claude/mod/` y `claude/tests/`.
- Esqueleto que compila: entrypoint vacío, `fabric.mod.json`, `estadisticasrpg.mixins.json` sin mixins, un gametest de humo.
- `git init` sin commit.
- **Falta:** `assets/estadisticasrpg/icon.png` (el `fabric.mod.json` todavía no declara ícono).

## Arquitectura

Paquete `com.estadisticasrpg`. **En principio no hace falta ningún mixin**: los atributos, los eventos de Fabric y el menú de cofre alcanzan. Si aparece uno, va en `com.estadisticasrpg.mixin` con el prefijo `estadisticasrpg$`.

| Archivo | Responsabilidad |
|---|---|
| `EstadisticasRPG.java` | Entrypoint `main`: registra el attachment, los eventos (`JOIN`, `COPY_FROM`) y el comando |
| `Configuracion.java` | Record con costo, valor por punto y tope de cada estadística, con valores por defecto. En la fase 4 se carga de `config/estadisticasrpg.json` con Gson |
| `Estadistica.java` | Enum con cada estadística: id (`vida`, `danio`…), atributo(s), operación, ítem que la representa en el menú y nombre en castellano |
| `PuntosDelJugador.java` | Record **inmutable** `Map<Estadistica, Integer>` con su `Codec`. Es el valor del attachment |
| `Mejoras.java` | `comprar(ServerPlayer, Estadistica, cantidad)` → `Compra(estadistica, pedidos, comprados, resultado)`: `resultado` es `OK`, o `TOPE`/`SIN_NIVELES` según qué cortó la compra (con `comprados == 0` no se compró nada) · `aplicar(ServerPlayer)` pone o quita los modificadores según los puntos · `reiniciar` |
| `MenuDeEstadisticas.java` | Subclase de `ChestMenu` (3 filas) que no deja mover ítems: un click compra y redibuja el menú |
| `ComandoEstadisticas.java` | Comandos (ver §5) |

Recursos: `fabric.mod.json`, `estadisticasrpg.mixins.json`, `assets/estadisticasrpg/icon.png`. Sin el mod en el cliente no hay archivo de idioma, así que **los textos que ve el jugador son `Component.literal` en castellano escrito en el servidor**. `Component.translatable` sólo con claves que ya trae vanilla (como el nombre de un atributo o de un ítem); nunca con claves propias, porque el cliente vanilla mostraría la clave cruda. Por eso el mod no tiene `lang/`.

### 1. Dónde se guardan los puntos

- **Attachment de Fabric** `PUNTOS` (`fabric-data-attachment-api-v1`): `AttachmentRegistry.create(id, b -> b.persistent(PuntosDelJugador.CODEC).copyOnDeath().initializer(PuntosDelJugador::vacio))`. Viaja en el NBT del jugador (`playerdata/<uuid>.dat`) y sobrevive a la muerte. `copyOnDeath` existe en 26.3 (verificado con `javap`).
- **El valor es inmutable** y se reemplaza con `setAttached`: si se mutara un mapa adentro, Fabric no se entera del cambio.
- **Los puntos son la fuente de verdad, no los modificadores.** Vanilla también guarda los modificadores permanentes en el NBT, pero se recalculan desde los puntos en cada `JOIN` y respawn. Así un cambio de `porPunto` en la config se aplica solo al reconectar.

### 2. Cómo se aplican (`Mejoras.aplicar`)

Un modificador **permanente** por estadística con un `Identifier` fijo (`estadisticasrpg:vida`, `estadisticasrpg:danio`…), con `addOrReplacePermanentModifier` (existe en 26.3). Aplicar dos veces no apila. Con 0 puntos se quita con `removeModifier(Identifier)`.

| Estadística | Atributo | Operación | Por punto | Tope | Máximo |
|---|---|---|---|---|---|
| Vida | `MAX_HEALTH` | `ADD_VALUE` | +2 (1 corazón) | 10 | +10 corazones (20 en total) |
| Daño | `ATTACK_DAMAGE` | `ADD_VALUE` | +0,5 | 10 | +5 (más que Filo V, que da +3: la espada de netherita pasa de 8 a 13, y con Filo V además a 16) |
| Armadura | `ARMOR` | `ADD_VALUE` | +1 | 10 | +10 (vanilla corta el atributo en 30) |
| Velocidad | `MOVEMENT_SPEED` | `ADD_MULTIPLIED_BASE` | +2 % | 10 | +20 % |
| Velocidad de ataque | `ATTACK_SPEED` | `ADD_MULTIPLIED_BASE` | +3 % | 10 | +30 % |
| Minado | `BLOCK_BREAK_SPEED` | `ADD_MULTIPLIED_BASE` | +5 % | 10 | +50 % |
| Alcance | `BLOCK_INTERACTION_RANGE` + `ENTITY_INTERACTION_RANGE` | `ADD_VALUE` | +0,25 bloques | 8 | +2 bloques |
| Resistencia al empuje | `KNOCKBACK_RESISTANCE` | `ADD_VALUE` | +0,05 | 10 | +0,5 |
| Suerte | `LUCK` | `ADD_VALUE` | +0,5 | 6 | +3 (lo mismo que Suerte marina III al pescar, y se suma: la pesca usa el encantamiento + `Player.getLuck()`, verificado). No se verificó si alguna tabla de cofres de vanilla usa la suerte (`quality`) |

Los nombres de los atributos están verificados en 26.3 (sin el prefijo `GENERIC_`/`PLAYER_` de 1.21.1). Las fases 1 y 3 se reparten la tabla (ver Fases).

**Ojo con los porcentajes:** `ADD_MULTIPLIED_BASE` multiplica la base **más todos los `ADD_VALUE`**, incluido el del arma. En velocidad de ataque eso es lo que se quiere (la espada pasa de 1,6 a 2,08 golpes por segundo con +30 %).

**Velocidad:** el cliente agranda el campo de visión con la velocidad. +20 % se nota pero no marea; si se sube el tope, avisarlo en el README.

**Vida al comprar:** subir `MAX_HEALTH` no sube la vida actual. Al comprar un punto de vida se cura lo que se agregó (`heal(porPunto)`), si no el corazón nuevo aparece vacío y parece que no funcionó.

### 3. Comprar (`Mejoras.comprar`)

1. **Se compra lo que entra:** `comprados = min(cantidad, tope − puntos, experienceLevel / costo)`. Con shift + click (5) y 3 de margen se compran 3: devolver `TOPE` sin comprar nada se siente roto desde el menú.
2. Si `comprados == 0` → `TOPE` si ya está en el tope, si no `SIN_NIVELES`. No se cobra nada.
3. `player.giveExperienceLevels(-costo × comprados)`. **Tiene que ser el override de `ServerPlayer`**, que pone `lastSentExp = -1` (verificado): sin eso la barra de experiencia del cliente no se actualiza, porque vanilla sólo la reenvía cuando cambia `totalExperience`, y bajar niveles no lo cambia.
4. `setAttached` con los puntos nuevos, `aplicar`, y si es vida, `heal(porPunto × comprados)`.
5. Mensaje al jugador (si `comprados < cantidad`, decir por qué: tope o niveles) y sonido (`SoundEvents.PLAYER_LEVELUP`).

Es todo atómico: lo que se cobra es exactamente lo que se sube.

### 4. Entrar y reaparecer

- **`ServerPlayerEvents.JOIN`:** `aplicar`. Los modificadores ya vienen en el NBT, pero así se ajustan a la config actual.
- **Respawn (`ServerPlayerEvents.COPY_FROM`, dispara en el `TAIL` de `ServerPlayer.restoreFrom`):** verificado con `javap` en 26.3 que, al morir (`alive == false`), `restoreFrom` hace sólo `assignBaseValues`: **los modificadores permanentes se pierden**, y después pone `setHealth(getMaxHealth())` **con la vida máxima sin los puntos**. Así que en `COPY_FROM`:
  1. leer los puntos del jugador **viejo** (no depender del orden en que Fabric copia el attachment respecto de este evento);
  2. `setAttached` en el nuevo, `aplicar`;
  3. `setHealth(getMaxHealth())`, o reaparece con 10 corazones aunque tenga 20.
- **Volver del End (`alive == true`):** vanilla copia los modificadores (`assignPermanentModifiers`) y la vida. `aplicar` es idempotente, no rompe nada.
- `keepInventory` no cambia nada de esto: los atributos se reinician igual.

### 5. Comandos (`/estadisticas`, alias `/stats`)

| Comando | Qué hace | Permiso |
|---|---|---|
| `/estadisticas` | Abre el menú (fase 2; en la fase 1, muestra lo mismo que `ver`) | todos |
| `/estadisticas ver [jugador]` | Puntos de cada estadística, efecto actual y costo | todos (otro jugador: op) |
| `/estadisticas subir <estadistica> [cantidad]` | Compra puntos | todos |
| `/estadisticas dar <jugador> <estadistica> <cantidad>` | Da puntos sin cobrar (para probar) | op |
| `/estadisticas reiniciar <jugador>` | Pone todo en 0 sin devolver niveles | op |
| `/estadisticas recargar` | Vuelve a leer la config (fase 4) | op |

Permisos con `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)` (en 26.3 no existe `src.hasPermission(2)`).

### 6. Menú de cofre (`MenuDeEstadisticas`)

- `player.openMenu(new SimpleMenuProvider((id, inv, p) -> new MenuDeEstadisticas(id, inv, p), titulo))` con un `SimpleContainer` de 27 slots. Una estadística por slot, con un ítem representativo (manzana dorada = vida, espada de hierro = daño, peto = armadura, azúcar = velocidad, pluma = velocidad de ataque, pico = minado, caña de pescar = alcance, yunque = empuje, trébol/conejo = suerte) y como lore: puntos `3/10`, efecto actual, efecto del próximo punto y costo. Gris (vidrio) si llegó al tope.
- `clicked(int slotId, int button, ContainerInput input, Player player)` **no llama a `super`** para los slots del menú: compra (click izquierdo = 1, shift = 5) y redibuja. `quickMoveStack` devuelve `ItemStack.EMPTY`. En 26.3 el enum se llama **`ContainerInput`**, no `ClickType` (verificado).
- El cliente vanilla **predice** el movimiento del ítem antes de que el servidor responda: después de cada click hay que reenviar el estado completo (`sendAllDataToRemote()`, público en 26.3) o el ítem queda "fantasma" en el cursor.
- Los slots del inventario del jugador (abajo) se dejan funcionar normal, **pero interceptar `clicked` en los slots del menú no alcanza**: hay acciones que arrancan en un slot del jugador y tocan los del menú.
  - **Doble click** (`PICKUP_ALL`) en el inventario con una manzana dorada en el cursor: vanilla junta todas las iguales del menú también, y el jugador se lleva el ítem de muestra.
  - **Arrastrar** (`QUICK_CRAFT`) por varios slots: puede repartir ítems dentro del menú.
  - Por eso los slots del menú son una subclase de `Slot` con **`mayPickup` y `mayPlace` en `false`**, y el menú además sobrescribe **`canTakeItemForPickAll` y `canDragTo`** para que devuelvan `false` en esos slots.

## Riesgos

**Ya verificados con `javap` en 26.3:**
- Nombres de `Attributes` (`MAX_HEALTH`, `ATTACK_DAMAGE`, `ARMOR`, `MOVEMENT_SPEED`, `ATTACK_SPEED`, `BLOCK_BREAK_SPEED`, `BLOCK_INTERACTION_RANGE`, `ENTITY_INTERACTION_RANGE`, `KNOCKBACK_RESISTANCE`, `LUCK`; son `Holder<Attribute>`).
- `AttributeInstance.addOrReplacePermanentModifier` y `removeModifier(Identifier)`.
- `ServerPlayer.restoreFrom`: al morir sólo `assignBaseValues` + `setHealth(getMaxHealth())`; al volver del End, `assignPermanentModifiers`.
- `ServerPlayer.giveExperienceLevels` pone `lastSentExp = -1`.
- `ServerPlayerEvents.COPY_FROM` en el `TAIL` de `restoreFrom`, `ServerPlayerEvents.JOIN`, `AttachmentRegistry.Builder.copyOnDeath()`.
- **Orden en la carga del jugador:** `LivingEntity.readAdditionalSaveData` lee `attributes` **antes** que `Health`, así que alguien con 20 corazones entra con 20 (si fuera al revés, la vida se cortaría contra el máximo sin los puntos).
- **Menú:** `AbstractContainerMenu.clicked(int, int, ContainerInput, Player)` (el enum ya no se llama `ClickType`); `sendAllDataToRemote()`, `canTakeItemForPickAll(ItemStack, Slot)` y `canDragTo(Slot)` son públicos; `Slot.mayPickup(Player)` y `Slot.mayPlace(ItemStack)` existen.
- **Sincronización con el cliente:** `MOVEMENT_SPEED`, `ATTACK_SPEED`, `BLOCK_BREAK_SPEED`, `BLOCK_INTERACTION_RANGE` y `ENTITY_INTERACTION_RANGE` son `isClientSyncable()` (lo verifica el gametest `losAtributosQueUsaElClienteSeSincronizan`).

**Junto con Inventario Compartido** (los dos son tuyos, así que es probable que se instalen juntos):
- La **experiencia es compartida**: cualquiera que compra gasta los niveles del grupo. Funciona, pero conviene decirlo en el README.
- La **vida es compartida por deltas** y cada uno tendría una `MAX_HEALTH` distinta: el que tiene menos máximo corta la vida del grupo. Hay que decidir si, con los dos mods, los puntos pasan a ser **del grupo** (guardados en un `SavedData` del mundo en vez del attachment). Queda para la fase 5; hasta entonces, documentarlo como incompatibilidad conocida.

## Fases (cada una termina con `gradlew runGameTest` en verde, la versión subida y `/docs`)

1. **Base** (0.1.0) — **hecha**: `Configuracion`, `Estadistica` con **vida, daño, armadura y velocidad**, attachment `PUNTOS`, `comprar`, `aplicar`, `JOIN` y respawn, comandos `ver`, `subir`, `dar` y `reiniciar`.
2. **Menú** (0.2.0) — **hecha**: `MenuDeEstadisticas` y `/estadisticas` sin argumentos lo abre.
3. **Más estadísticas** (0.3.0) — **hecha**: velocidad de ataque, minado, alcance, empuje y suerte.
4. **Config y pulido** (0.4.0): `config/estadisticasrpg.json`, `recargar`, sonidos y partículas al comprar, README completo.
5. **Opcional — con Inventario Compartido** (0.5.0): puntos del grupo si los dos mods están cargados (`FabricLoader.isModLoaded("inventariocompartido")`).

## Verificación

**GameTests** (`src/gametest`). No hay estado global, así que pueden ser varios tests independientes, cada uno con su jugador. El jugador sale de un `joinSurvivalPlayer` como el de Inventario Compartido: **no** `makeMockServerPlayerInLevel`, que fuerza creativo.

- Comprar con 5 niveles: queda en 4, `vida = 1`, `MAX_HEALTH = 22`, la vida subió 2.
- Comprar con 0 niveles: no cambia nada y devuelve `SIN_NIVELES`.
- Comprar en el tope: no cobra y devuelve `TOPE`.
- Comprar 5 de una con 3 de margen: compra 3 y cobra 3 niveles. Con 5 de margen y 2 niveles: compra 2.
- `aplicar` dos veces no apila (`MAX_HEALTH` sigue en 22).
- Respawn (`server.getPlayerList().respawn(...)`): los puntos siguen, los atributos también, y la vida es la máxima con los puntos (40 con 10 de vida).
- Guardar y cargar el jugador (`TagValueOutput`/`TagValueInput`, sacando `"UUID"`): los puntos y la vida se conservan.
- `reiniciar`: todo en 0 y sin modificadores.
- Menú: un click en el slot de vida compra; el cursor queda vacío y el contenedor intacto.
- Menú: con una manzana dorada en el cursor, doble click (`PICKUP_ALL`) en el inventario no saca la del slot de vida; arrastrar (`QUICK_CRAFT`) no pone nada en los slots del menú.

**A mano** (`gradlew runServer` + `runClient`, ver `claude/general/probar-en-el-juego.md`): `/xp add @s 30 levels`, comprar desde el menú, ver los corazones nuevos, morir y confirmar que siguen. Probar también con un **cliente vanilla sin el mod**: el menú y los mensajes tienen que verse bien.
