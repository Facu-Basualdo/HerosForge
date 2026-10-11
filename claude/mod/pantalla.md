# Pantalla propia del cliente

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el juego crashea en la pantalla de título con "Components not bound yet" apenas se instala el mod en el cliente; un cliente modificado se regala experiencia pidiendo −5 puntos; un jugador sin el mod en el cliente deja de poder abrir el menú; la K no hace nada con el inventario abierto; el botón del inventario queda en cualquier lado al abrir el libro de recetas.

## Qué es

Un complemento **opcional** del lado del cliente: una pantalla propia (`PantallaDeEstadisticas`) para ver y comprar puntos, que se abre con la tecla **K**, con un botón en el inventario de supervivencia o con `/estadisticas`. **No decide nada:** muestra lo que manda el servidor y le pide las compras, que el servidor valida y cobra con el mismo `Mejoras.comprar` que el cofre.

**Es el mismo jar**, con `splitEnvironmentSourceSets()` en `build.gradle`: el código del cliente va en `src/client` (paquete `com.estadisticasrpg.cliente`), que el servidor no ve al compilar. Un servidor dedicado nunca carga esas clases: el entrypoint `client` y `estadisticasrpg.client.mixins.json` (con `"environment": "client"` en `fabric.mod.json`) sólo corren en el cliente.

| Quién tiene el mod | Qué pasa |
|---|---|
| Servidor sí, cliente no | Como siempre: `/estadisticas` abre el cofre. El servidor no le manda nada |
| Servidor sí, cliente sí | La pantalla propia. `/estadisticas` también la abre, salvo que el cliente tenga `usarPantalla: false` |
| Servidor no (o una versión sin la pantalla), cliente sí | La K avisa en la barra de acción que el servidor no tiene el mod; el botón del inventario no aparece |

## Los paquetes (`Paquetes`)

Registrados en `Paquetes.registrar()` desde `InterfazDelCliente.registrar()`, que corre en el entrypoint `main` (o sea, también en el cliente: ahí los necesita `ClientPlayNetworking`).

| Paquete | Sentido | Para qué |
|---|---|---|
| `Estado(experiencia, nivel, costo, grupo, filas)` | servidor → cliente | Todo lo que dibuja la pantalla. Una `Fila(id, nombre, puntos, tope, ahora, proximo)` por `Estadistica`, en el orden del enum |
| `Resultado(texto, compro)` | servidor → cliente | El `Mejoras.mensaje` de una compra pedida desde la pantalla |
| `AbrirPantalla()` | servidor → cliente | `/estadisticas` de alguien que prefiere la pantalla |
| `Comprar(id, cantidad)` | cliente → servidor | Botones **+1** y **+5** |
| `Preferencia(pantalla)` | cliente → servidor | Al entrar (`ClientPlayConnectionEvents.JOIN`): si `/estadisticas` le abre la pantalla o el cofre |
| `AbrirCofre()` | cliente → servidor | **Ver como cofre**, y la tecla con `usarPantalla: false` |

- **Los textos van armados en el servidor** (`ahora`, `proximo`, `nombre`, el mensaje), igual que en el cofre: la pantalla dice lo mismo aunque el cliente tenga otra versión del mod u otra config. El cliente sólo usa su `Estadistica` para el ícono, y si el id no existe en su enum (servidor más nuevo) pone una barrera.
- **`AbrirCofre` es un paquete y no `sendCommand("estadisticas")`:** con la preferencia de pantalla, el comando lo mandaría de vuelta a la pantalla.
- **Un cliente vanilla no registra estos canales**, y todo lo que se manda pasa antes por `ServerPlayNetworking.canSend`. Registrar los tipos en el servidor no le cambia nada a un cliente sin el mod.

## Lo que hace el servidor (`InterfazDelCliente`)

- **El estado se manda sólo cuando cambia:** en `END_SERVER_TICK`, a cada jugador con `canSend`, se compara una `Foto(experiencia, nivel, puntos, config, grupo)` con la última enviada (`ENVIADO`, por UUID). Es lo mismo que hace `MenuDeEstadisticas.broadcastChanges` para redibujar el cofre: así la pantalla se actualiza si junta orbes, si un op usa `dar`, si con los puntos del grupo compra otro, o con `recargar`. La config se compara por valor (es un record): un `recargar` sin cambios no reenvía.
- `abrir` (lo que llama `/estadisticas`) manda el estado forzado y `AbrirPantalla` **sólo si prefirió la pantalla y además `canSend`**; si no, `MenuDeEstadisticas.abrir`. Lo verifica el gametest `sinElComplementoEstadisticasAbreElCofre`.
- **`comprar` no le cree nada al cliente:** id desconocido o `cantidad < 1` → no se intenta comprar (devuelve vacío). Con una cantidad negativa, `Mejoras.comprar` cobraría experiencia negativa, o sea que **la regalaría** y restaría puntos. La cantidad se corta en el tope, sólo para que el mensaje no diga "no te alcanzó para los 2147483647"; el `max(…, 1)` evita pedir 0 con tope 0, que `comprar` daría por `OK`.
- `ENVIADO` y `PREFIEREN_PANTALLA` se limpian en `ServerPlayConnectionEvents.DISCONNECT`. Al reaparecer el `ServerPlayer` es otro pero el UUID es el mismo, así que no se reenvía nada de más.
- Los receptores de Fabric corren en el hilo del servidor (lo dice el javadoc de `PlayPayloadHandler`): se compra directo, sin `server.execute`.

## Lo que hace el cliente (`src/client`)

| Clase | Qué hace |
|---|---|
| `EstadisticasRPGCliente` | Entrypoint `client`: los receptores (guarda el último `Estado`), la tecla, la preferencia al entrar, `abrir` y `servidorConElMod` (`canSend(Comprar)`) |
| `PantallaDeEstadisticas` | La pantalla. Una fila por estadística con ícono, nombre, lo que da ahora, `puntos/tope`, una barra y **+1**/**+5**; tooltip con el detalle |
| `BotonDelInventario` | Un `Button` con una botella de experiencia, a la derecha del del libro de recetas. También hace que la K funcione con el inventario abierto |
| `ConfiguracionDelCliente` | `config/estadisticasrpg-cliente.json`: `usarPantalla` y `botonEnInventario`, los dos `true` por defecto. Un valor que no es booleano usa el de por defecto y avisa en el log |
| `mixin/AccesoAPantallaDeContenedor` | Accessor de `leftPos` (protegido) |

- **Nada de `ItemStack` en campos estáticos de clases que se cargan antes de entrar a un mundo.** `BotonDelInventario` se carga en el `AFTER_INIT` de la pantalla de título, antes de que vanilla enlace los componentes de los ítems, y un `static final ItemStack` ahí revienta el juego al arrancar ("Components not bound yet"). Por eso el ícono es un campo de instancia (el botón sólo se crea dentro de un mundo) y la pantalla arma los suyos al dibujar. Lo encontró el client gametest.
- **La K no llega a la `KeyMapping` con una pantalla abierta:** `consumeClick` sólo cuenta en el mundo. En el inventario se escucha con `ScreenKeyboardEvents.afterKeyPress`, salvo con `isInputCaptured()` (escribiendo en el buscador del libro de recetas la K es una letra). Adentro de la pantalla, la K y la E la cierran (`keyPressed`).
- **El botón se reubica en cada frame (`ScreenEvents.beforeExtract`):** abrir o cerrar el libro de recetas cambia `leftPos` sin volver a llamar a `init`. La posición copia `InventoryScreen.getRecipeBookButtonPosition` (`leftPos + 104`, `height / 2 − 22`, 20 × 18) y se corre un botón a la derecha: ahí no hay slots.
- **Antes de abrir la pantalla desde el inventario se llama a `inventario.onClose()`**, que manda el paquete de cerrar como la E. Sin eso el servidor deja los ítems en la grilla de crafteo y el del cursor sin devolver. Por eso Esc y **Listo** no vuelven a la misma instancia: arman un `InventoryScreen` nuevo.
- **Es un `Button` (no un `AbstractButton`)** para que `clickScreenButton` de los client gametests y otros mods que buscan botones lo encuentren.
- **`isPauseScreen()` es `false`:** en singleplayer, pausar frena el servidor integrado y las compras no se contestarían hasta cerrar.
- **La pantalla no rearma los widgets con cada `Estado`:** si llegan las mismas estadísticas en el mismo orden, en `tick()` sólo cambia las filas y el `active` de los botones (rearmar le saca el foco a quien navega con el teclado, y la experiencia cambia con cada orbe). Con otras estadísticas, `rebuildWidgets()`.
- **El tamaño está pensado para 426 × 240**, lo más chico que elige la escala automática de la interfaz (720p y 1440p): ancho fijo de 400 y el alto de las filas entre 14 y 18 según cuántas sean. Con 10 estadísticas entran en una columna; si algún día no entran ni a 14, hay que pasar a dos columnas o a una lista con scroll.
- La tecla y su categoría usan un archivo de idioma (`src/client/resources/assets/estadisticasrpg/lang/en_us.json`, en castellano): es lo único con clave de traducción, porque *Controles* sólo acepta claves. El resto de los textos de la pantalla son `Component.literal`.

## Agregar una estadística

No hace falta tocar nada de esto: `InterfazDelCliente.estado` recorre `Estadistica.values()` y la pantalla dibuja las filas que lleguen. Revisá que siga entrando en 240 de alto (la captura `estadisticas-pantalla-chica` del client gametest).

## Ver también

- [Tests de la pantalla](../tests/pantalla.md)
- [Menú de estadísticas](menu.md): el cofre, que sigue siendo la interfaz sin el mod en el cliente
- [Estadísticas: puntos, compra y aplicación](estadisticas.md): `Mejoras.comprar` y `Mejoras.mensaje`, que usa la pantalla
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md): lo que cambió en las clases de pantallas y teclas
