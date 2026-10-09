# Índice — Mod (`src/main`)

Java 25 · Fabric API 0.162.0+26.3 (attachments, eventos de jugador, comandos) · Minecraft 26.3 (nombres de Mojang).

[← Hub](../../CLAUDE.md)

## Protocolo antes de tocar el código

1. Si todavía no hay notas abajo, el diseño está en [docs/plan.md](../../docs/plan.md): leé la sección de la fase que vas a implementar y sus riesgos.
2. Verificá la firma real de lo que uses de vanilla con `javap` (ver [minecraft-26-sin-ofuscar](../general/minecraft-26-sin-ofuscar.md)) en vez de confiar en tutoriales o en memoria.
3. Corré `gradlew runGameTest`.

## Notas

| Nota | De qué trata |
|---|---|
| [Estadísticas: puntos, compra y aplicación](estadisticas.md) | Los puntos del attachment mandan; `aplicar` recalcula los modificadores (idempotente, id fijo por estadística) en compra, `JOIN` y `COPY_FROM` · al morir hay que reponer la vida con el máximo nuevo · `aplicar` recorta la vida en el momento porque vanilla espera al próximo tick · se compra lo que entra, con `giveExperienceLevels` de `ServerPlayer` para que baje la barra · codec por id de texto: un id desconocido se ignora, y un id **no se renombra nunca** · toda `Estadistica` tiene que estar en `Configuracion.POR_DEFECTO` o NPE · el alias `/stats` necesita su propio `executes` · lo que calcula el cliente (velocidad, ataque, minado, alcance) tiene que ser `isClientSyncable` · alcance = dos atributos · la suerte sólo está verificada al pescar |
| [Menú de estadísticas](menu.md) | `AbstractContainerMenu` con `GENERIC_9x3` (no `ChestMenu`, para poner `SlotDeMuestra`) · `clicked` no llama a `super` en los slots 0–26 y siempre hace `sendAllDataToRemote`, si no el ítem queda "fantasma" en el cursor del cliente · el doble click y el arrastre arrancan en el inventario del jugador: los frenan `mayPickup`/`mayPlace` y `canTakeItemForPickAll`/`canDragTo` · se redibuja desde `broadcastChanges` si cambian niveles o puntos · `ITEM_NAME` y lore con `withItalic(false)` · ocultar `ATTRIBUTE_MODIFIERS` o la espada muestra su daño |
