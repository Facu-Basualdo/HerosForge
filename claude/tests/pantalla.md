# Tests de la pantalla

[← Índice Tests](INDICE.md)

Síntomas que esta nota evita: los gametests del servidor pasan y el juego crashea al abrirlo con el mod en el cliente; `clickScreenButton` no encuentra un botón que está a la vista; el test de la pantalla falla por un tooltip en la captura o espera que la pantalla se cierre cuando vuelve al inventario.

## Dos tipos de test

| Tarea | Dónde | Qué prueba |
|---|---|---|
| `gradlew runGameTest` | `HerosForgeGameTest` (servidor sin ventana) | Lo que hace `InterfazDelCliente`, llamándolo directo |
| `gradlew runClientGameTest` | `PantallaClientGameTest` (un cliente de verdad con el servidor integrado) | La tecla, la pantalla, el botón del inventario y que una compra desde la pantalla llegue al servidor |

**`runClientGameTest` abre una ventana de Minecraft** y tarda medio minuto: no está colgado de `build` ni de `check` (sólo `runGameTest` lo está). Corrélo a mano cuando toques `src/client`, `Paquetes` o `InterfazDelCliente`. Se activa con `enableClientGameTests = true` en `build.gradle`, y el entrypoint es `fabric-client-gametest` en `src/gametest/resources/fabric.mod.json`. El source set `gametest` ve las clases de `src/client`.

**Los gametests del servidor no alcanzan para el cliente:** el crash de "Components not bound yet" (ver [Pantalla propia](../mod/pantalla.md)) pasaba al arrancar el cliente, y todos los del servidor daban OK.

## Los del servidor

| Test | Qué verifica |
|---|---|
| `elEstadoParaLaPantallaTieneTodo` | Experiencia, costo, devolución, una fila por estadística en el orden del enum con su rama (id y nombre), puntos y tope, y los textos `ahora`/`proximo` armados en el servidor (`ahora` vacío con 0 puntos) |
| `elEstadoViajaPorLaRedSinPerderNada` | `Estado`, `Comprar` y `Bajar` codificados y decodificados con `Unpooled.buffer()` dan lo mismo y no sobran bytes |
| `losPedidosDelClienteNoSeCreen` | Cantidad −5 o 0 y una estadística que no existe no compran ni cobran; `Integer.MAX_VALUE` compra lo que alcanza y cobra justo · lo mismo con `Bajar`: los pedidos truchos no bajan nada, y `Integer.MAX_VALUE` baja los que hay |
| `sinElComplementoEstadisticasAbreElCofre` | Con `preferir(true)` pero sin los canales registrados, `/estadisticas` abre `MenuDeEstadisticas` |

- **Los jugadores de prueba (`entrar()`) nunca tienen los canales:** su conexión es un `EmbeddedChannel` y no hubo fase de configuración, así que `canSend` da `false`. Por eso estos tests llaman a `estado`, `comprar` y `preferir` directo y no pueden ver los paquetes que se mandan. Lo que pasa con `canSend` en `true` lo cubre el client gametest.

## El del cliente (`PantallaClientGameTest`)

Crea un mundo de singleplayer, pone supervivencia, da experiencia y puntos con comandos, y:

1. Aprieta K → espera `PantallaDeEstadisticas` → captura `estadisticas-pantalla` (abre en la primera rama, la del guerrero).
2. `clickScreenButton("Guardián")` → captura `estadisticas-guardian` → `clickScreenButton("+1")` (en esa pestaña, el primero es el de vida) → comprueba en el servidor (`computeOnServer`) que subió un punto y cobró 30 → captura → `clickScreenButton("-1")` (también el de vida) → bajó el punto y devolvió 15.
3. K la cierra; E abre el inventario → captura `estadisticas-inventario` → `clickScreenButton("Estadísticas")` abre la pantalla → Esc vuelve al inventario.
4. Con el inventario abierto, K la abre (sigue en la pestaña del guardián, la rama más larga) → captura `estadisticas-pantalla-chica` (la ventana es 852 × 480, o sea interfaz de 426 × 240; la ventana del test ya arranca de ese tamaño, así que todas las capturas muestran las dos columnas) → **Listo** vuelve al inventario.

Gotchas:

- **`clickScreenButton` hace click en el primero con ese texto**: con varias ramas, "+1" es el de la primera fila **de la pestaña abierta**. Elegí la pestaña antes.
- **`clickScreenButton(texto)` sólo encuentra `Button` y `CycleButton`**, buscando por `getMessage().getString()` (lo traduce como clave, y un literal queda igual). Un `AbstractButton` propio no aparece aunque se vea: por eso `BotonDelInventario` extiende `Button`.
- **Abierta desde el inventario, Esc y Listo vuelven al inventario**, no a `null`: el test espera `InventoryScreen`.
- **Las teclas se aprietan con `InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K)`.** En 26.3 los códigos ya no son los de GLFW (K es 14): usá siempre las constantes.
- **`setCursorPos(0, 0)` antes de las capturas**: si no, el cursor queda donde cayó y el tooltip de algún botón tapa la pantalla.
- Después de pedir una compra: `waitForServerboundPackets()` y `waitForClientboundPackets()` antes de mirar el servidor o sacar la captura.
- Las capturas quedan en `build/run/clientGameTest/screenshots`, numeradas. **Miralas** cuando cambies el dibujo: el test sólo falla si algo no se puede abrir o comprar, no si se superpone texto. `runClientGameTest` borra `build/run/clientGameTest/config` antes de arrancar, igual que `runGameTest` con la suya.

## Ver también

- [Pantalla propia del cliente](../mod/pantalla.md)
- [Tests del menú](menu.md): el cofre, que sí se prueba entero desde el servidor
- [Probar en el juego](../general/probar-en-el-juego.md): lo que falta probar a mano (un servidor dedicado con un cliente con y otro sin el mod)

La prueba también captura nivel 989 (millones de XP) en la ventana chica y una ventana de 1280 × 800. Revisá `estadisticas-experiencia-alta` y `estadisticas-pantalla-amplia`: los textos de la cabecera deben quedar dentro de su mitad y los botones dentro del panel.
