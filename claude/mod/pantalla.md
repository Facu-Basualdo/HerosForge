# Pantalla propia del cliente

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el juego crashea en la pantalla de título con "Components not bound yet" apenas se instala el mod en el cliente; un cliente modificado se regala experiencia pidiendo −5 puntos; un jugador con el complemento de una versión anterior queda desconectado al entrar; un jugador sin el mod en el cliente deja de poder abrir el menú; la K no hace nada con el inventario abierto; el botón del inventario queda en cualquier lado al abrir el libro de recetas.

## Qué es

Un complemento **opcional** del lado del cliente: una pantalla propia (`PantallaDeEstadisticas`) para ver, comprar y bajar puntos, que se abre con la tecla **K**, con un botón en el inventario de supervivencia o con `/estadisticas`. **No decide nada:** muestra lo que manda el servidor y le pide las compras y las bajas, que el servidor valida con el mismo `Mejoras.comprar` y `Mejoras.bajar` que el cofre.

**Es el mismo jar**, con `splitEnvironmentSourceSets()` en `build.gradle`: el código del cliente va en `src/client` (paquete `com.herosforge.cliente`), que el servidor no ve al compilar. Un servidor dedicado nunca carga esas clases: el entrypoint `client` y `herosforge.client.mixins.json` (con `"environment": "client"` en `fabric.mod.json`) sólo corren en el cliente.

| Quién tiene el mod | Qué pasa |
|---|---|
| Servidor sí, cliente no | Como siempre: `/estadisticas` abre el cofre. El servidor no le manda nada |
| Servidor sí, cliente sí | La pantalla propia. `/estadisticas` también la abre, salvo que el cliente tenga `usarPantalla: false` |
| Servidor no (o una versión anterior a la del cliente), cliente sí | La K avisa en la barra de acción que el servidor no tiene el mod; el botón del inventario no aparece |
| Servidor sí, cliente con una versión anterior (sin el canal `estado_v3`) | Como sin el mod: el cofre, y la K avisa que el servidor no tiene el mod. Ver Versiones de los canales |

## Los paquetes (`Paquetes`)

Registrados en `Paquetes.registrar()` desde `InterfazDelCliente.registrar()`, que corre en el entrypoint `main` (o sea, también en el cliente: ahí los necesita `ClientPlayNetworking`).

| Paquete | Sentido | Para qué |
|---|---|---|
| `Estado(experiencia, nivel, costo, devolucion, grupo, filas)` (canal `estado_v3`) | servidor → cliente | Todo lo que dibuja la pantalla. Una `Fila(id, nombre, rama, nombreDeLaRama, puntos, tope, ahora, proximo)` por `Estadistica`, en el orden del enum |
| `Resultado(texto, compro)` | servidor → cliente | El `Mejoras.mensaje` de una compra o una baja pedida desde la pantalla (`compro` = si salió: verde o rojo) |
| `AbrirPantalla()` | servidor → cliente | `/estadisticas` de alguien que prefiere la pantalla |
| `Comprar(id, cantidad)` | cliente → servidor | Botones **+1** y **+5** |
| `Bajar(id, cantidad)` (canal `bajar_v2`) | cliente → servidor | Botón **-1**. Es también la pregunta del cliente por la versión del servidor |
| `Preferencia(pantalla)` | cliente → servidor | Al entrar (`ClientPlayConnectionEvents.JOIN`): si `/estadisticas` le abre la pantalla o el cofre |
| `AbrirCofre()` | cliente → servidor | **Ver como cofre**, y la tecla con `usarPantalla: false` |

- **Los textos van armados en el servidor** (`ahora`, `proximo`, `nombre`, el mensaje), igual que en el cofre: la pantalla dice lo mismo aunque el cliente tenga otra versión del mod u otra config. El cliente sólo usa su `Estadistica` para el ícono, y si el id no existe en su enum (servidor más nuevo) pone una barrera.
- **`AbrirCofre` es un paquete y no `sendCommand("estadisticas")`:** con la preferencia de pantalla, el comando lo mandaría de vuelta a la pantalla.
- **Un cliente vanilla no registra estos canales**, y todo lo que se manda pasa antes por `ServerPlayNetworking.canSend`. Registrar los tipos en el servidor no le cambia nada a un cliente sin el mod.

## Versiones de los canales

**Un `StreamCodec` no tolera un campo de más ni de menos:** si el servidor manda un `Estado` con un campo que el cliente no conoce, el cliente lee mal el paquete y se desconecta. Por eso, cuando cambia lo que lleva un paquete, **cambia el nombre del canal** (`estado` → `estado_v2` en la 0.10.0, al agregar `devolucion`; `estado_v3` en la 0.13.0, al agregar la rama a cada fila):

- **Servidor nuevo, cliente viejo:** el cliente registró `estado`, no `estado_v2`. `canSend(Estado.TIPO)` es `false`, no se le manda nada y `abrir` le abre el cofre. **`abrir` pregunta por el canal del estado, no por `AbrirPantalla`**: el cliente viejo también registra `abrir_pantalla`, y la pantalla se quedaría esperando un estado que nunca llega.
- **Servidor viejo, cliente nuevo:** `servidorConElMod()` pregunta por `Bajar`, el canal más nuevo. El servidor viejo no lo tiene, así que la K avisa en vez de abrir una pantalla que nunca recibiría el estado.
- **Por eso `Bajar` cambia de nombre junto con el estado** (`bajar` → `bajar_v2` en la 0.13.0) aunque lo que lleva sea igual: el cliente sólo puede preguntar por canales cliente → servidor. Si `bajar` hubiera quedado igual, un cliente 0.12 vería que el servidor 0.13 "tiene el mod", abriría la pantalla y se quedaría esperando un `estado_v2` que nunca llega.

Al agregar otro campo a un paquete, subí el sufijo del estado (`_v4`) **y el de `Bajar`**, y cambiá esas dos preguntas al canal nuevo.

## Lo que hace el servidor (`InterfazDelCliente`)

- **El estado se manda sólo cuando cambia:** en `END_SERVER_TICK`, a cada jugador con `canSend`, se compara una `Foto(experiencia, nivel, puntos, config, grupo)` con la última enviada (`ENVIADO`, por UUID). Es lo mismo que hace `MenuDeEstadisticas.broadcastChanges` para redibujar el cofre: así la pantalla se actualiza si junta orbes, si un op usa `dar`, si con los puntos del grupo compra otro, o con `recargar`. La config se compara por valor (es un record): un `recargar` sin cambios no reenvía.
- `abrir` (lo que llama `/estadisticas`) manda el estado forzado y `AbrirPantalla` **sólo si prefirió la pantalla y además `canSend(Estado.TIPO)`**; si no, `MenuDeEstadisticas.abrir`. Lo verifica el gametest `sinElComplementoEstadisticasAbreElCofre`.
- **`comprar` no le cree nada al cliente:** id desconocido o `cantidad < 1` → no se intenta comprar (devuelve vacío). Con una cantidad negativa, `Mejoras.comprar` cobraría experiencia negativa, o sea que **la regalaría** y restaría puntos. La cantidad se corta en el tope, sólo para que el mensaje no diga "no te alcanzó para los 2147483647"; el `max(…, 1)` evita pedir 0 con tope 0, que `comprar` daría por `OK`. **`bajar` valida igual:** con −5, `Mejoras.bajar` no baja nada (corta en 0), pero un pedido así nunca es legítimo y no se intenta.
- `ENVIADO` y `PREFIEREN_PANTALLA` se limpian en `ServerPlayConnectionEvents.DISCONNECT`. Al reaparecer el `ServerPlayer` es otro pero el UUID es el mismo, así que no se reenvía nada de más.
- Los receptores de Fabric corren en el hilo del servidor (lo dice el javadoc de `PlayPayloadHandler`): se compra directo, sin `server.execute`.

## Lo que hace el cliente (`src/client`)

| Clase | Qué hace |
|---|---|
| `HerosForgeCliente` | Entrypoint `client`: los receptores (guarda el último `Estado`), la tecla, la preferencia al entrar, `abrir` y `servidorConElMod` (`canSend(Bajar)`) |
| `PantallaDeEstadisticas` | La pantalla. Una pestaña por rama y, de la elegida, una fila por estadística con ícono, nombre, lo que da ahora, `puntos/tope`, una barra y **-1**/**+1**/**+5**; tooltip con el detalle y lo que devuelve bajar |
| `BotonDelInventario` | Un `Button` con una botella de experiencia, a la derecha del del libro de recetas. También hace que la K funcione con el inventario abierto |
| `ConfiguracionDelCliente` | `config/herosforge-cliente.json`: `usarPantalla` y `botonEnInventario`, los dos `true` por defecto. Un valor que no es booleano usa el de por defecto y avisa en el log |
| `mixin/AccesoAPantallaDeContenedor` | Accessor de `leftPos` (protegido) |

- **Nada de `ItemStack` en campos estáticos de clases que se cargan antes de entrar a un mundo.** `BotonDelInventario` se carga en el `AFTER_INIT` de la pantalla de título, antes de que vanilla enlace los componentes de los ítems, y un `static final ItemStack` ahí revienta el juego al arrancar ("Components not bound yet"). Por eso el ícono es un campo de instancia (el botón sólo se crea dentro de un mundo) y la pantalla arma los suyos al dibujar. Lo encontró el client gametest.
- **La K no llega a la `KeyMapping` con una pantalla abierta:** `consumeClick` sólo cuenta en el mundo. En el inventario se escucha con `ScreenKeyboardEvents.afterKeyPress`, salvo con `isInputCaptured()` (escribiendo en el buscador del libro de recetas la K es una letra). Adentro de la pantalla, la K y la E la cierran (`keyPressed`).
- **El botón se reubica en cada frame (`ScreenEvents.beforeExtract`):** abrir o cerrar el libro de recetas cambia `leftPos` sin volver a llamar a `init`. La posición copia `InventoryScreen.getRecipeBookButtonPosition` (`leftPos + 104`, `height / 2 − 22`, 20 × 18) y se corre un botón a la derecha: ahí no hay slots.
- **Antes de abrir la pantalla desde el inventario se llama a `inventario.onClose()`**, que manda el paquete de cerrar como la E. Sin eso el servidor deja los ítems en la grilla de crafteo y el del cursor sin devolver. Por eso Esc y **Listo** no vuelven a la misma instancia: arman un `InventoryScreen` nuevo.
- **Es un `Button` (no un `AbstractButton`)** para que `clickScreenButton` de los client gametests y otros mods que buscan botones lo encuentren.
- **`isPauseScreen()` es `false`:** en singleplayer, pausar frena el servidor integrado y las compras no se contestarían hasta cerrar.
- **La pantalla no rearma los widgets con cada `Estado`:** si llegan las mismas estadísticas en el mismo orden, en `tick()` sólo cambia las filas y el `active` de los botones (rearmar le saca el foco a quien navega con el teclado, y la experiencia cambia con cada orbe). Con otras estadísticas, `rebuildWidgets()`.
- **Una pestaña por rama**, en el orden en que llegan las filas: las ramas salen del estado (`Fila.rama` y `nombreDeLaRama`), no del enum del cliente, así una rama que este cliente no conoce se ve igual. La elegida está en un campo **estático** (`ramaElegida`): al volver a abrir la pantalla sigue en la misma, y si ya no existe se va a la primera. Cambiar de pestaña hace `rebuildWidgets()`. `BotonForja` recorta el texto con "…": en 426 de ancho, "Superviviente" no entra en su pestaña.
- **La rama elegida va en dos columnas** (la primera mitad a la izquierda: con 9, quedan 5 y 4), hasta 620 de ancho y filas de 16–30 de alto según el espacio disponible. **El alto del panel sale de la rama más larga, no de la elegida:** si no, el panel cambia de tamaño con cada pestaña (lo muestran las capturas `pantalla-chica` y `pantalla-chica-explorador`, que tienen que quedar iguales). A 426 × 240 se muestran nombres cortos y puntos; con columnas de al menos 260 y filas de al menos 26 se agrega el efecto debajo y puntos/tope. El detalle completo sigue en el tooltip. La cabecera separa experiencia/nivel del costo y recorta cada texto a su espacio para evitar desbordes con millones de XP. El costo va centrado en el panel, salvo que la experiencia llegue hasta ahí: entonces se corre a la derecha.
- **BotonForja extiende Button** y sobrescribe `extractContents` (la firma verificada con `javap`): conserva narración, foco de teclado y compatibilidad con `clickScreenButton`, pero dibuja colores propios y un borde claro al enfocarse. No sobrescribir `extractWidgetRenderState`: es final en esta versión.

- Los botones mandan con el **id** de la estadística, no con la `Fila`: el renglón se reemplaza con cada estado nuevo y la lambda del botón no.
- La tecla y su categoría usan un archivo de idioma (`src/client/resources/assets/herosforge/lang/en_us.json`, en castellano): es lo único con clave de traducción, porque *Controles* sólo acepta claves. El resto de los textos de la pantalla son `Component.literal`.

## Agregar una estadística

No hace falta tocar nada de esto: `InterfazDelCliente.estado` recorre `Estadistica.values()` y la pantalla dibuja las filas que lleguen, en la pestaña de su rama. Revisá que siga entrando en 240 de alto y que el nombre no se corte (las capturas del client gametest, que ya corren a 426 × 240; la del guardián tiene la rama más larga): si se corta, un `nombreCorto`.

## Ver también

- [Tests de la pantalla](../tests/pantalla.md)
- [Menú de estadísticas](menu.md): el cofre, que sigue siendo la interfaz sin el mod en el cliente
- [Estadísticas: puntos, compra y aplicación](estadisticas.md): `Mejoras.comprar`, `Mejoras.bajar` y `Mejoras.mensaje`, que usa la pantalla
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md): lo que cambió en las clases de pantallas y teclas
- [Idioma](idioma.md): de dónde salen los textos y en qué idioma.
