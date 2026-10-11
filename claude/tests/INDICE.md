# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

Los tests de cada fase están listados en [docs/plan.md](../../docs/plan.md#verificación).

| Nota | De qué trata |
|---|---|
| [Tests de estadísticas](estadisticas.md) | En 26.3 `assertValueEqual(actual, esperado, …)`: al revés, el mensaje miente · los jugadores de prueba no tickean (conexión falsa), así que lo que vanilla hace "en el próximo tick" no pasa · supervivencia con `entrar()`, no `makeMockServerPlayerInLevel` · morir = `respawn(j, false, KILLED)`, End = `true` · guardar/cargar sacando `"UUID"` · el jugador de prueba no es op · comentá el registro del evento para ver que el test falla |
| [Tests del menú](menu.md) | Los clicks se simulan con `menu.clicked(slot, botón, ContainerInput, jugador)`; el arrastre con `getQuickcraftMask` · para el doble click, el cursor tiene que tener una **copia exacta** del ícono: una manzana común no junta la del menú, con o sin protección, y el test pasaría siempre · `broadcastChanges` hay que llamarlo a mano |
| [Tests de configuración](configuracion.md) | Los tests de lectura usan un directorio temporal, nunca `build/run/gameTest/config`, que es el que lee `recargar` · `runGameTest` borra esa carpeta antes de arrancar: si no, después de cambiar `POR_DEFECTO` los tests corren con los valores viejos · cambiar la config global sólo dentro de un test sin secuencia y restaurando en `finally`: entre ticks corren los otros tests · la consola (`servidor.createCommandSourceStack()`) es op |
| [Tests del grupo](grupo.md) | Inventario Compartido no está cargado: el modo se fuerza con `usarPuntosDelGrupo(true)` y no se prueba su reparto de experiencia y vida · todo adentro de `conPuntosDelGrupo`, que borra el `SavedData` y apaga el modo en un `finally` · los jugadores se crean antes de prender el modo, porque `JOIN` suma sus puntos individuales |
