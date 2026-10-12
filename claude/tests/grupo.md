# Tests del grupo

[← Índice Tests](INDICE.md)

Síntoma que esta nota evita: después de un test de puntos compartidos, otros tests ven puntos que nunca dieron, o les cambia la vida máxima, porque el modo y los puntos del grupo quedaron prendidos en el servidor de prueba.

| Test | Qué verifica |
|---|---|
| `grupoLoQueCompraUnoLoTienenTodos` | A compra y paga · B ve el punto y tiene el corazón extra · el attachment de A no se toca · `reiniciar` desde B le saca los puntos a A |
| `grupoSumaLosPuntosIndividualesUnaSolaVez` | Al entrar se toma el máximo por estadística, no la suma · después de `reiniciar`, reconectar no devuelve los puntos viejos · los attachments quedan intactos |
| `grupoAlReaparecerConservaLosPuntos` | Morir y reaparecer con los puntos del grupo: vida máxima 40 y vida llena |

## Soul Link no está cargado

**Los tests fuerzan el modo con `Mejoras.usarPuntosDelGrupo(true)`.** Soul Link no está en el classpath de los gametests, así que lo que hace ese mod no se prueba: que reparta la experiencia gastada y la curación del corazón nuevo. En los tests paga y se cura sólo el que compra. Eso se probó a mano en la 0.9.1 (ver [Puntos del grupo](../mod/grupo.md#probado-a-mano-con-los-dos-mods)).

## conPuntosDelGrupo

**Todo test del grupo corre adentro de `conPuntosDelGrupo(helper, () -> …)`:**

1. `PuntosDelGrupo.de(servidor).olvidarTodo()`: el `SavedData` vive en el mundo de prueba y lo comparten todos los tests.
2. `usarPuntosDelGrupo(true)`.
3. En el `finally`: modo apagado, `olvidarTodo()` otra vez y `aplicarATodos`, para que nadie quede con modificadores de los puntos del grupo.

Vale lo mismo que para la config (ver [Tests de configuración](configuracion.md)): **sin `startSequence` ni `thenIdle` adentro**, porque entre ticks corren los otros tests con el modo prendido.

**Los jugadores se crean antes de prender el modo** (`entrar()` dispara `JOIN`, que en modo grupo suma sus puntos individuales) y se sacan después de apagarlo. En `grupoSumaLosPuntosIndividualesUnaSolaVez`, los puntos individuales se dan con el modo apagado, y `alEntrar` se llama a mano para simular que reconectan.

## Integración real

`./gradlew runGameTest "-PsoulLinkJar=../SoulLink/build/libs/soullink-3.0.0.jar"` cambia el entrypoint de tests a `SoulLinkGameTest` y agrega ese jar únicamente al runtime de gametest. La suite normal sigue sin Soul Link.

La prueba crea cuatro jugadores conectados y ejecuta, en un mismo tick, bajas de vida, daño y curación pendientes, reinicio, recarga de un tope menor, dos compras con saldo para una, devolución y compra con XP pendiente de otro jugador. Usa la sincronización del mod real, no una simulación. Restaurá configuración, jugadores y puntos en finally.

`puntosPorGrupoDeSoulLink` arma dos grupos con `/soullink grupo` (desde la consola: hace falta ser operador). Lo que compra uno de rojo lo tiene su compañero y no azul; al pasar al compañero a azul se queda sin los puntos ni el modificador de rojo, que siguen en rojo. **Usa grupos propios con un sufijo al azar**, no el general: así no se cruza con `transaccionesConSoulLinkReal`, que corre en el mismo batch con el general, y los grupos de una corrida anterior (quedan en el mundo de prueba) no traen puntos.

**`RamasGameTest` falla con Soul Link cargado, y no es un bug de ninguno de los dos mods.** Sus tests corren en paralelo con jugadores que quedan todos en el grupo general: comparten inventario, hambre y vida entre sí y se pisan los números (6 a 8 fallas que cambian de corrida en corrida; ya pasaba con la 0.13.2 y Soul Link 2.1.0). En esta suite mirá sólo los `soul_link_game_test_*`.

## Ver también

- [Puntos del grupo](../mod/grupo.md)
- [Tests de configuración](configuracion.md)
- [Tests de estadísticas](estadisticas.md)
