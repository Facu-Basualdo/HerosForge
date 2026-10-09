# Menú de estadísticas

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el ítem de muestra queda "fantasma" en el cursor después de comprar; un jugador se lleva la manzana dorada del menú; la espada del menú muestra "+6 de daño de ataque", que no tiene nada que ver con los puntos; todo el lore se ve en cursiva.

## Qué es

`MenuDeEstadisticas` es un `AbstractContainerMenu` de tipo **`MenuType.GENERIC_9x3`**: 27 slots de muestra arriba y el inventario del jugador abajo. El cliente vanilla lo dibuja como un cofre común, así que **no hace falta el mod del lado del cliente**. Se abre con `MenuDeEstadisticas.abrir(jugador)` (`/estadisticas` o `/stats` sin argumentos).

**Extiende `AbstractContainerMenu` y no `ChestMenu`** porque `ChestMenu` agrega sus propios `Slot` en el constructor, y los del menú tienen que ser `SlotDeMuestra`.

| Slot | Qué hay |
|---|---|
| 4 (arriba al medio) | Botella de experiencia: niveles del jugador, costo, cómo se compra |
| `slotDe(estadistica)` (fila del medio) | `Estadistica.icono()`, o vidrio gris (`Items.STAINED_GLASS_PANE.gray()`) si llegó al tope |
| El resto | Vidrio negro con el tooltip oculto |

`slotDe` reparte según cuántas estadísticas hay: **con 5 o menos, separadas por un hueco; con más, seguidas y centradas**. En la fila del medio entran 9, que es lo que trae la fase 3. Si algún día hay más de 9, hay que cambiar el reparto (por ejemplo, a dos filas).

## Un click compra, nada se mueve

**`clicked` no llama a `super` para los slots del menú (0–26):** con `PICKUP` (click) compra 1, con `QUICK_MOVE` (shift + click) compra `CON_SHIFT` (5), y cualquier otra entrada (`SWAP`, `THROW`, `CLONE`, `QUICK_CRAFT`) no hace nada. Los slots del jugador y los clicks fuera de la ventana (`slotId` -999) van a `super` y funcionan normal.

**Después de cada click en el menú se llama a `sendAllDataToRemote()`, compre o no.** El cliente vanilla **predice** el resultado del click y mueve el ítem en pantalla antes de que el servidor conteste. Si el servidor no reenvía el estado completo, el ítem queda en el cursor del cliente aunque en el servidor nunca se movió.

El mensaje de la compra (`Mejoras.mensaje`) va a la **barra de acción** (`sendSystemMessage(…, true)`), para no llenar el chat con un mensaje por click.

## Por qué interceptar clicked no alcanza

Hay acciones que **arrancan en un slot del jugador**, pasan por `super.clicked` y tocan los del menú:

| Acción | Qué haría vanilla | Qué lo frena |
|---|---|---|
| Doble click (`PICKUP_ALL`) en el inventario | Junta en el cursor los ítems iguales de **todos** los slots | `SlotDeMuestra.mayPickup` → `false` **y** `canTakeItemForPickAll` → `false` para `SlotDeMuestra` (vanilla mira las dos) |
| Arrastrar (`QUICK_CRAFT`) | Reparte el cursor en los slots por los que pasa | Los eventos de cada slot del menú caen en el `clicked` interceptado; además `SlotDeMuestra.mayPlace` → `false` y `canDragTo` → `false` |
| Shift + click en el inventario | `quickMoveStack` mueve el ítem al otro lado | `quickMoveStack` devuelve `ItemStack.EMPTY` |

**El doble click sólo junta ítems con el mismo tipo y los mismos componentes** (`canItemQuickReplace`). Una manzana dorada común no junta la del menú, porque a esa le faltan el nombre y el lore. El riesgo real es una **copia exacta del ícono** (un jugador en creativo puede armarla), y para eso están `mayPickup` y `canTakeItemForPickAll`. Las firmas están verificadas en 26.3 con `javap`.

## Se redibuja solo

`redibujar()` vuelve a armar los 27 slots desde la config, los puntos y los niveles. Se llama al abrir, después de cada compra y **desde `broadcastChanges()` cuando cambiaron los niveles o los puntos** respecto de lo último que se dibujó. Así el menú se actualiza si el jugador junta experiencia con el menú abierto, o si un op le da puntos.

`broadcastChanges` lo llama el tick del jugador, y sólo manda al cliente los slots que cambiaron. Armar ítems nuevos en cada `redibujar` es barato.

## Cómo se ven los ítems

- **El nombre va en `DataComponents.ITEM_NAME`, no en `CUSTOM_NAME`:** el nombre personalizado se ve en cursiva y como "renombrado en un yunque".
- **El lore es cursiva por defecto en vanilla:** cada línea lleva `withItalic(false)` (`texto()`), o el menú entero se ve torcido.
- **`TOOLTIP_DISPLAY` oculta `ATTRIBUTE_MODIFIERS`** en los íconos: la espada y el peto mostrarían su daño y su armadura de vanilla. El relleno usa `TooltipDisplay(true, …)` y no muestra tooltip.
- El color del nombre y del costo es **verde si alcanza para un punto, rojo si no**.
- Todo el texto es `Component.literal` en castellano, armado en el servidor.

## Agregar una estadística al menú

No hace falta tocar el menú: alcanza con el `icono` en `Estadistica` (ver [Estadísticas](estadisticas.md#agregar-una-estadística)). `slotDe` reubica todo.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Tests del menú](../tests/menu.md)
- [Probar en el juego](../general/probar-en-el-juego.md): cómo se ve con un cliente vanilla
