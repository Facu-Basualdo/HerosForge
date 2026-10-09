# Índice — Tests (`src/gametest`)

Fabric GameTest API · `gradlew runGameTest` levanta un servidor sin ventana, corre los tests y sale con error si alguno falla.

[← Hub](../../CLAUDE.md)

Los tests de cada fase están listados en [docs/plan.md](../../docs/plan.md#verificación).

| Nota | De qué trata |
|---|---|
| [Tests de estadísticas](estadisticas.md) | En 26.3 `assertValueEqual(actual, esperado, …)`: al revés, el mensaje miente · los jugadores de prueba no tickean (conexión falsa), así que lo que vanilla hace "en el próximo tick" no pasa · supervivencia con `entrar()`, no `makeMockServerPlayerInLevel` · morir = `respawn(j, false, KILLED)`, End = `true` · guardar/cargar sacando `"UUID"` · el jugador de prueba no es op · comentá el registro del evento para ver que el test falla |
