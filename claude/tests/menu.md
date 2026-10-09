# Tests del menú

[← Índice Tests](INDICE.md)

Síntoma que esta nota evita: un test que "prueba" que el doble click no saca el ítem del menú pasa siempre, incluso sin ninguna protección, porque el ítem del cursor no era igual al de muestra.

Los tests son `menuCompraConClick` y `menuNoDejaSacarNiPonerItems`, en `EstadisticasRPGGameTest`.

| Test | Qué verifica |
|---|---|
| `menuCompraConClick` | `/estadisticas` abre `MenuDeEstadisticas` · click compra 1 y cobra 1 · shift + click compra 5 · el cursor queda vacío y el ícono en su lugar · `SWAP`, `THROW` y `CLONE` no compran ni sacan nada · con el tope, el slot pasa a vidrio gris después de `broadcastChanges` |
| `menuNoDejaSacarNiPonerItems` | Doble click con una copia del ícono no lo junta · arrastrar tierra por un slot del menú sólo la reparte en el inventario · shift + click desde el inventario no mueve nada |

## Simular clicks

**Se llama a `menu.clicked(slotId, boton, ContainerInput, jugador)` directamente**, que es lo que hace `ServerGamePacketListenerImpl` al recibir el paquete. Los slots 0–26 son los del menú y del 27 en adelante los del inventario del jugador.

| Acción del jugador | Llamada |
|---|---|
| Click | `clicked(slot, 0, PICKUP, j)` |
| Shift + click | `clicked(slot, 0, QUICK_MOVE, j)` |
| Tecla numérica, Q, click del medio | `SWAP`, `THROW`, `CLONE` |
| Doble click | `setCarried(…)` y `clicked(slotDelJugador, 0, PICKUP_ALL, j)` |
| Arrastrar | `clicked(-999, getQuickcraftMask(QUICKCRAFT_HEADER_START, tipo), QUICK_CRAFT, j)`, un `HEADER_CONTINUE` por cada slot y `clicked(-999, …HEADER_END…)` |

Las constantes y `getQuickcraftMask(header, tipo)` son públicas en `AbstractContainerMenu`.

## El ítem del cursor tiene que ser una copia exacta

**Para probar el doble click, el cursor tiene que tener `menu.getSlot(slot).getItem().copy()`, no `new ItemStack(Items.GOLDEN_APPLE)`.** `PICKUP_ALL` sólo junta ítems con el mismo tipo **y los mismos componentes**. El ícono tiene `ITEM_NAME`, `LORE` y `TOOLTIP_DISPLAY`, así que una manzana común nunca lo junta, haya protección o no.

Se comprobó sacando `mayPickup` y `canTakeItemForPickAll`. Con una manzana común el test pasaba igual; con la copia, falla como tiene que fallar.

Lo mismo vale para cualquier test de "no se puede sacar": **antes de darlo por bueno, sacale la protección y fijate que falle** (ver [Tests de estadísticas](estadisticas.md#que-un-test-pruebe-lo-que-dice)).

## Detalles

- El menú se abre de verdad (`openMenu`), así que `sendAllDataToRemote` manda paquetes por la conexión falsa del jugador de prueba: no rompe nada.
- `broadcastChanges` no corre solo, porque los jugadores de prueba no tickean (ver [Tests de estadísticas](estadisticas.md)). Para ver un redibujo que no viene de un click, llamalo a mano.
- Cerrá el menú (`closeContainer`) antes de `salir`.

## Ver también

- [Menú de estadísticas](../mod/menu.md)
- [Tests de estadísticas](estadisticas.md)
