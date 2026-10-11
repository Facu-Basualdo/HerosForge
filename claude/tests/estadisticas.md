# Tests de estadísticas

[← Índice Tests](INDICE.md)

Síntomas que esta nota evita: un test falla con "Expected X to be 40.0: was 20.0" y los números parecen al revés; un test espera algo que vanilla hace "en el próximo tick" y nunca pasa; un test de compra pasa en creativo y no prueba nada.

Todo está en `EstadisticasRPGGameTest`. Cada test crea su propio jugador y lo saca al final: no hay estado global, así que corren en paralelo sin pisarse.

## Qué cubre cada test

| Test | Qué verifica |
|---|---|
| `comprarVidaCobraUnNivelYLlenaLaVida` | Con 5 niveles: queda en 4, `MAX_HEALTH` 20,6, vida 20,6; `aplicar` dos veces no apila |
| `sinNivelesNoCambiaNada` | `SIN_NIVELES`, 0 comprados, `MAX_HEALTH` sigue en 20 |
| `enElTopeNoCobra` | `TOPE` sin cobrar; `dar` tampoco pasa el tope |
| `compraLoQueEntra` | Pide 5 con 3 de margen → compra 3 (`TOPE`); pide 5 con 2 niveles → compra 2 (`SIN_NIVELES`) |
| `elCostoSubeConLosPuntosComprados` | Con `puntosPorAumento − 1` puntos repartidos en dos estadísticas: comprar 2 cobra 1 + 2 · el próximo cuesta 2 · con 1 nivel no compra · con 3 niveles y pidiendo 5 compra 1 |
| `velocidadEsPorcentajeDeLaBase` | 100 puntos → `MOVEMENT_SPEED` = base × 1,2 |
| `alReaparecerConservaPuntosYVida` | Morir: puntos, `MAX_HEALTH` 50 y vida 50 · volver del End: vida actual (30) |
| `guardarYCargarConservaPuntosYVida` | Guardar y cargar: puntos, `MAX_HEALTH` 50 y vida 33 |
| `unaEstadisticaDesconocidaNoBorraLasDemas` | El codec ignora un id que no existe y lee los demás |
| `reiniciarDejaTodoEnCero` | Puntos vacíos, sin modificadores, vida recortada a 20 en el momento |
| `comandos` | `subir` compra y cobra, `/stats` funciona, `dar` sin op no hace nada, una estadística desconocida no cobra |
| `estadisticasDeLaFase3AlMaximo` | Velocidad de ataque 5,2 con la mano vacía · minado 2 · alcance 6,5 a bloques y 5 a entidades · empuje 0,5 · `getLuck()` 3 |
| `losAtributosQueUsaElClienteSeSincronizan` | Los atributos que calcula el cliente son `isClientSyncable()` (lista a mano: hay que sumar ahí cualquier estadística nueva que use el cliente) |
| `cadaEstadisticaTieneConfigYSlot` | Toda `Estadistica` está en `Configuracion.POR_DEFECTO` y tiene un slot propio en la fila del medio del menú |

Los valores con decimales (porcentajes, alcance, y la vida, que con 0,6 por punto casi nunca es entera) se comparan con `assertCerca`, no con `assertValueEqual`: `0,1 × 1,2` no da exactamente `0,12` en `double`. **La tolerancia es 1e-4 y no 1e-9 porque la vida actual es `float`:** `20,6f` pasado a `double` es 20,600000381…

**Los números de los tests salen de `POR_DEFECTO`:** si se cambia un `porPunto` o un tope, hay que rehacer las cuentas acá (los topes de 100 obligan a `dar(…, 100)` para llegar al máximo). El test del costo lee `puntosPorAumento` de `POR_DEFECTO` para no depender del número.

## assertValueEqual va al revés de lo que parece

**En 26.3 la firma es `assertValueEqual(actual, esperado, mensaje)`**: el primero es el valor real. El mensaje de error es "Expected `<mensaje>` to be `<esperado>`: was `<actual>`". Con los argumentos al revés, el test pasa o falla igual, pero el mensaje miente y hace perder tiempo. Verificado con `javap -c` sobre `GameTestHelper`.

## Los jugadores de prueba no tickean

`entrar()` conecta al jugador con una `Connection` sobre un `EmbeddedChannel`, que el servidor nunca tickea. **Así que `LivingEntity.tick()` no corre**, y todo lo que vanilla hace "en el próximo tick" no pasa aunque el test espere con `thenIdle`. Por ejemplo:

- `refreshDirtyAttributes`, que recorta la vida cuando baja `MAX_HEALTH`.

Si algo depende de un tick, o el mod lo hace en el momento (como `Mejoras.aplicar` con la vida), o el test tiene que llamar al método de vanilla a mano. **No esperes ticks para eso.**

## Cómo armar cada escenario

| Escenario | Cómo |
|---|---|
| Jugador en supervivencia | `entrar(helper, nombre)`: `new ServerPlayer` + `PlayerList.placeNewPlayer` (dispara `JOIN`) + `setGameMode(SURVIVAL)`. **No `makeMockServerPlayerInLevel`**, que fuerza creativo |
| Niveles | `setExperienceLevels(n)` |
| Morir y reaparecer | `PlayerList.respawn(jugador, false, Entity.RemovalReason.KILLED)`: el `false` es `restoreFrom(viejo, false)`, el camino de la muerte. Devuelve el jugador **nuevo** |
| Volver del End | `respawn(jugador, true, CHANGED_DIMENSION)` |
| Guardar y cargar | `TagValueOutput.createWithContext` + `saveWithoutId`, **sacar `"UUID"`** (si no, el jugador nuevo hereda el UUID del viejo y el `PlayerList` se confunde) y `entrar(helper, nombre, guardado)`, que hace `load` antes de `placeNewPlayer` como el login de vanilla |
| Comandos | `getCommands().performPrefixedCommand(jugador.createCommandSourceStack(), "...")`. El jugador de prueba **no es op**: sirve para probar que `dar` y `reiniciar` lo rechazan |
| Leer el codec | `PuntosDelJugador.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow()` |

`performPrefixedCommand` no tira excepciones: un comando que falla (sin permiso, argumento inválido) sólo le manda el error a la fuente. **Lo que se verifica es el estado después** (puntos, niveles), no que haya fallado.

## Que un test pruebe lo que dice

Antes de dar por bueno un test de un evento, comentá el registro del evento y corré `gradlew runGameTest`: el test tiene que fallar. `alReaparecerConservaPuntosYVida` se comprobó así contra `COPY_FROM`.

## Ver también

- [Estadísticas: puntos, compra y aplicación](../mod/estadisticas.md)
- [Tests del menú](menu.md)
- [Tests de configuración](configuracion.md)
- [Tests del grupo](grupo.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
