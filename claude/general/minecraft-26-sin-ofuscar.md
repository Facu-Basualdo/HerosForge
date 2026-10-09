# Minecraft 26.x sin ofuscar

[← Hub](../../CLAUDE.md)

**Desde Minecraft 26.1 el jar viene sin ofuscar y Fabric usa los nombres oficiales de Mojang.** Yarn dejó de mantenerse. Casi todos los tutoriales y respuestas de foros anteriores a 2026 usan nombres de Yarn (`PlayerInventory`, `ServerPlayerEntity`, `EntityAttributes`…) que acá no existen.

| Yarn (viejo) | Mojang (este proyecto) |
|---|---|
| `ServerPlayerEntity` | `ServerPlayer` |
| `EntityAttributes` | `Attributes` |
| `EntityAttributeModifier` | `AttributeModifier` |
| `PersistentState` | `SavedData` |
| `Identifier` (Yarn) / `ResourceLocation` (Mojang viejo) | `Identifier` |

**Tampoco confíes en lo que "sabés" de 1.21.x:** entre versiones cambian firmas. Lo que importa para este mod:

| Lo que esperarías | Cómo es en 26.3 |
|---|---|
| `Attributes.GENERIC_MAX_HEALTH`, `PLAYER_BLOCK_BREAK_SPEED` | **Sin prefijo**: `MAX_HEALTH`, `ATTACK_DAMAGE`, `ARMOR`, `MOVEMENT_SPEED`, `ATTACK_SPEED`, `BLOCK_BREAK_SPEED`, `BLOCK_INTERACTION_RANGE`, `ENTITY_INTERACTION_RANGE`, `KNOCKBACK_RESISTANCE`, `LUCK`. Son `Holder<Attribute>` |
| Un modificador identificado por `UUID` | Por `Identifier`: `addOrReplacePermanentModifier(modificador)` y `removeModifier(Identifier)` en `AttributeInstance` |
| Que los modificadores permanentes sobrevivan a la muerte | **No sobreviven**: `ServerPlayer.restoreFrom` al morir hace sólo `assignBaseValues` y después `setHealth(getMaxHealth())` con el máximo sin modificadores. Al volver del End sí copia todo (`assignPermanentModifiers`) |
| Que bajar niveles actualice la barra sola | Sólo con el override de `ServerPlayer.giveExperienceLevels`, que pone `lastSentExp = -1`: vanilla reenvía la barra cuando cambia `totalExperience`, y bajar niveles no lo cambia |
| `.requires(src -> src.hasPermission(2))` | `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))` |
| `Items.GRAY_STAINED_GLASS_PANE`, `Items.RED_WOOL`… | **Los ítems de color están agrupados en una `ColorCollection`:** `Items.STAINED_GLASS_PANE.gray()`, `.black()`, `.pick(DyeColor)` |
| `AbstractContainerMenu.clicked(int, int, ClickType, Player)` | El enum se llama **`ContainerInput`**: `clicked(int, int, ContainerInput, Player)` |
| `GameTestHelper.assertValueEqual(esperado, actual, …)` | Al revés: **`assertValueEqual(actual, esperado, …)`**. El mensaje dice "Expected … to be `<esperado>`: was `<actual>`" |

- La carga y el guardado usan `ValueInput` / `ValueOutput`, no `CompoundTag` directo.
- Fabric: `ServerPlayerEvents.COPY_FROM` dispara en el `TAIL` de `restoreFrom` (después de todo lo de arriba) y `AttachmentRegistry.Builder.copyOnDeath()` existe.

## Verificar antes de escribir un mixin o usar una API

Un mixin con un nombre o descriptor mal puesto **compila igual y explota recién al cargar la clase** en runtime. Antes de escribir uno (o de apoyarte en el orden de algo de vanilla), mirá la clase real:

```bash
J=.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-*/26.3/minecraft-merged-*-26.3.jar
JP="/c/Program Files/Microsoft/jdk-25.0.4.101-hotspot/bin/javap"

"$JP" -p -cp $J net.minecraft.world.entity.ai.attributes.Attributes   # campos y firmas
"$JP" -c -p -cp $J net.minecraft.server.level.ServerPlayer \
  | awk '/public void restoreFrom\(/,/^$/'                            # bytecode de un método
```

El jar aparece después del primer `gradlew compileJava` (también está el de los otros mods en su `.gradle/loom-cache`). Con `-c` se ve el bytecode, que alcanza para saber a quién llama un método y en qué orden: así se vio lo de `restoreFrom` de arriba.

Para las clases de Fabric API (eventos, attachments, comandos, gametest), los jars están en `~/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/`. El jar `-sources` de cada módulo trae el código de sus mixins: así se ve **dónde** dispara cada evento.

## Ver también

- [Entorno Java y Gradle](entorno-java-gradle.md)
- [Probar en el juego](probar-en-el-juego.md)
- [Estadísticas (mod)](../mod/estadisticas.md): dónde se usa lo de `restoreFrom` y `giveExperienceLevels`
