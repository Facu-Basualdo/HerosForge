# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

Los tests de cada fase están listados en [docs/plan.md](../../docs/plan.md#verificación).

| Nota | De qué trata |
|---|---|
| [Tests de estadísticas](estadisticas.md) | En 26.3 `assertValueEqual(actual, esperado, …)`: al revés, el mensaje miente · los jugadores de prueba no tickean (conexión falsa), así que lo que vanilla hace "en el próximo tick" no pasa · supervivencia con `entrar()`, no `makeMockServerPlayerInLevel` · morir = `respawn(j, false, KILLED)`, End = `true` · guardar/cargar sacando `"UUID"` · el jugador de prueba no es op · comentá el registro del evento para ver que el test falla · los proyectiles se prueban con `tick()` a mano sin agregarlos al mundo, contra un gólem de hierro (sin armadura, que no reduce lineal) |
| [Tests del menú](menu.md) | Los clicks se simulan con `menu.clicked(slot, botón, ContainerInput, jugador)`; el arrastre con `getQuickcraftMask` · para el doble click, el cursor tiene que tener una **copia exacta** del ícono: una manzana común no junta la del menú, con o sin protección, y el test pasaría siempre · `broadcastChanges` hay que llamarlo a mano |
| [Tests de configuración](configuracion.md) | Los tests de lectura usan un directorio temporal, nunca `build/run/gameTest/config`, que es el que lee `recargar` · `runGameTest` borra esa carpeta antes de arrancar: si no, después de cambiar `POR_DEFECTO` los tests corren con los valores viejos · cambiar la config global sólo dentro de un test sin secuencia y restaurando en `finally`: entre ticks corren los otros tests · la consola (`servidor.createCommandSourceStack()`) es op |
| [Tests de la pantalla](pantalla.md) | `runClientGameTest` abre una ventana y no está en `build`: corrélo a mano al tocar `src/client` · los jugadores de prueba del servidor nunca tienen los canales (`canSend` es `false`) · `clickScreenButton` sólo encuentra `Button`/`CycleButton` · abierta desde el inventario, Esc y Listo vuelven ahí · teclas con las constantes de `InputConstants` (en 26.3 K es 14, no GLFW) · `setCursorPos(0, 0)` antes de las capturas · mirá las capturas: el test no ve texto superpuesto |
| [Tests del grupo](grupo.md) | La suite normal no carga Soul Link; `-PsoulLinkJar` ejecuta `SoulLinkGameTest` con el mod real y cuatro jugadores · en la suite normal el modo se fuerza con `usarPuntosDelGrupo(true)` y no se prueba su reparto de experiencia y vida · todo adentro de `conPuntosDelGrupo`, que borra el `SavedData` y apaga el modo en un `finally` · los jugadores se crean antes de prender el modo, porque `JOIN` suma sus puntos individuales |
