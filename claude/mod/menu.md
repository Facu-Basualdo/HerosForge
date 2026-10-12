# Menú de estadísticas

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: un click en la portada compra en vez de abrir la rama; el ítem de muestra queda "fantasma" en el cursor después de comprar o bajar; un jugador se lleva la manzana dorada del menú; la espada del menú muestra "+6 de daño de ataque", que no tiene nada que ver con los puntos; todo el lore se ve en cursiva.

## Qué es

`MenuDeEstadisticas` es un `AbstractContainerMenu` de tipo **`MenuType.GENERIC_9x3`**: 27 slots de muestra arriba y el inventario del jugador abajo. El cliente vanilla lo dibuja como un cofre común, así que **no hace falta el mod del lado del cliente**. Se abre con `MenuDeEstadisticas.abrir(jugador)` (`/estadisticas` o `/stats` sin argumentos).

**Extiende `AbstractContainerMenu` y no `ChestMenu`** porque `ChestMenu` agrega sus propios `Slot` en el constructor, y los del menú tienen que ser `SlotDeMuestra`.

Las 40 estadísticas no entran en 27 slots: **el menú tiene páginas**. `abierta` es la `Rama` que se ve, o `null` para la portada, y `mostrar(rama)` cambia de página y redibuja. Es el mismo contenedor: el título no cambia (para cambiarlo habría que reabrir el menú, y el cursor saltaría al centro).

| Slot | Portada (`abierta == null`) | Página de una rama |
|---|---|---|
| 0 | Vidrio negro | Flecha: vuelve a la portada |
| 4 (arriba al medio) | Botella de experiencia: experiencia, nivel y costo | La misma, con cómo se compra y se baja |
| Fila del medio (9–17) | Las 5 ramas (`slotDe(rama)`): resumen y los puntos de cada estadística | Las estadísticas de la rama (`slotDe(estadistica)`), o vidrio gris (`Items.STAINED_GLASS_PANE.gray()`) si llegaron al tope |
| Fila de abajo (18–26) | Vidrio negro | Las 5 ramas como pestañas, en la misma columna que en la portada; la abierta brilla (`ENCHANTMENT_GLINT_OVERRIDE`) |
| El resto | Vidrio negro con el tooltip oculto | |

Dentro de una fila, con 5 o menos van separadas por un hueco (las ramas: slots 9, 11, 13, 15, 17); con más, seguidas y centradas. **Con más de 9 estadísticas en una rama no entran** (lo atrapa el gametest `cadaEstadisticaTieneConfigYSlot`): habría que partirla en dos ramas. `slotDe(estadistica)` es el slot en la página de su rama: dos estadísticas de ramas distintas comparten número de slot, por eso `estadisticaEn` sólo busca en la rama abierta.

## Un click compra o baja, nada se mueve

**`clicked` no llama a `super` para los slots del menú (0–26).** Un click (`PICKUP` o `QUICK_MOVE`, con cualquier botón) en una rama de la portada, en una pestaña o en la flecha **sólo navega**: nunca compra. En una estadística, con `PICKUP` (click) compra 1, con `QUICK_MOVE` (shift + click) compra `CON_SHIFT` (5). **Con el botón derecho (`boton == 1`) baja en vez de comprar** (`Mejoras.bajar`, que devuelve la mitad de la experiencia): vanilla manda el botón también con shift, así que shift + click derecho baja 5. El vidrio gris de una estadística al máximo también se puede bajar. Cualquier otra entrada (`SWAP`, `THROW`, `CLONE`, `QUICK_CRAFT`) no hace nada. Los slots del jugador y los clicks fuera de la ventana (`slotId` -999) van a `super` y funcionan normal.

**Después de cada click en el menú se llama a `sendAllDataToRemote()`, compre o no.** El cliente vanilla **predice** el resultado del click y mueve el ítem en pantalla antes de que el servidor conteste. Si el servidor no reenvía el estado completo, el ítem queda en el cursor del cliente aunque en el servidor nunca se movió.

El mensaje de la compra o la baja (`Mejoras.mensaje`) va a la **barra de acción** (`sendSystemMessage(…, true)`), para no llenar el chat con un mensaje por click.

## Por qué interceptar clicked no alcanza

Hay acciones que **arrancan en un slot del jugador**, pasan por `super.clicked` y tocan los del menú:

| Acción | Qué haría vanilla | Qué lo frena |
|---|---|---|
| Doble click (`PICKUP_ALL`) en el inventario | Junta en el cursor los ítems iguales de **todos** los slots | `SlotDeMuestra.mayPickup` → `false` **y** `canTakeItemForPickAll` → `false` para `SlotDeMuestra` (vanilla mira las dos) |
| Arrastrar (`QUICK_CRAFT`) | Reparte el cursor en los slots por los que pasa | Los eventos de cada slot del menú caen en el `clicked` interceptado; además `SlotDeMuestra.mayPlace` → `false` y `canDragTo` → `false` |
| Shift + click en el inventario | `quickMoveStack` mueve el ítem al otro lado | `quickMoveStack` devuelve `ItemStack.EMPTY` |

**El doble click sólo junta ítems con el mismo tipo y los mismos componentes** (`canItemQuickReplace`). Una manzana dorada común no junta la del menú, porque a esa le faltan el nombre y el lore. El riesgo real es una **copia exacta del ícono** (un jugador en creativo puede armarla), y para eso están `mayPickup` y `canTakeItemForPickAll`. Las firmas están verificadas en 26.3 con `javap`.

## Se redibuja solo

`redibujar()` vuelve a armar los 27 slots de la página abierta desde la config, los puntos y la experiencia. Se llama al abrir, después de cada compra y **desde `broadcastChanges()` cuando cambiaron la experiencia (`Mejoras.experiencia`, no el nivel: el costo es en puntos y un orbe ya puede alcanzar), los puntos o la config** respecto de lo último que se dibujó. Así el menú se actualiza si el jugador junta experiencia con el menú abierto, si un op le da puntos o si se usa `recargar`. La config se compara por identidad (ver [Configuración](configuracion.md)).

`broadcastChanges` lo llama el tick del jugador, y sólo manda al cliente los slots que cambiaron. Armar ítems nuevos en cada `redibujar` es barato.

## Cómo se ven los ítems

- **El nombre va en `DataComponents.ITEM_NAME`, no en `CUSTOM_NAME`:** el nombre personalizado se ve en cursiva y como "renombrado en un yunque".
- **El lore es cursiva por defecto en vanilla:** cada línea lleva `withItalic(false)` (`texto()`), o el menú entero se ve torcido.
- **`TOOLTIP_DISPLAY` oculta `ATTRIBUTE_MODIFIERS`** en los íconos: la espada y el peto mostrarían su daño y su armadura de vanilla. El relleno usa `TooltipDisplay(true, …)` y no muestra tooltip.
- El color del nombre y del costo es **verde si alcanza para un punto, rojo si no**.
- Todo el texto es `Component.literal` en castellano, armado en el servidor.

## Agregar una estadística al menú

No hace falta tocar el menú: alcanza con el `icono` y la `rama` en `Estadistica` (ver [Estadísticas](estadisticas.md#agregar-una-estadística)). `slotDe` reubica todo. Una rama nueva va en `Rama`, con su ícono y su resumen: entran hasta 9 en la fila de la portada, pero con más de 5 dejan de ir separadas.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Tests del menú](../tests/menu.md)
- [Configuración](configuracion.md)
- [Probar en el juego](../general/probar-en-el-juego.md): cómo se ve con un cliente vanilla
- [Idioma](idioma.md): de dónde salen los textos y en qué idioma.
