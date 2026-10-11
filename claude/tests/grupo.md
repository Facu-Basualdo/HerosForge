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

## Ver también

- [Puntos del grupo](../mod/grupo.md)
- [Tests de configuración](configuracion.md)
- [Tests de estadísticas](estadisticas.md)
