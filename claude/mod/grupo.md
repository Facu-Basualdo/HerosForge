# Puntos del grupo (con Inventario Compartido)

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: con Inventario Compartido, el que compró 10 corazones queda con 20 de vida porque otro tiene menos máximo; uno gasta niveles de todos y sólo él mejora; al reconectar, a un jugador le vuelven los puntos de antes de un `reiniciar`; un agente busca los puntos en el attachment y están en otro lado.

## Cuándo se activa

**Si `FabricLoader.isModLoaded("inventariocompartido")` (`EstadisticasRPG.INVENTARIO_COMPARTIDO`), en `onInitialize` se llama a `Mejoras.usarPuntosDelGrupo(true)`.** No hay opción en la config, y no se depende de las clases de Inventario Compartido, sólo de su id de mod: así no hace falta compilar contra él ni tenerlo en el classpath.

El porqué: Inventario Compartido comparte la experiencia (la reparte por diferencias en `SharedExperience.sync`) y la vida (en `SharedVitals.syncHealth`, recortada contra la vida máxima de **cada uno**). Con puntos individuales:

- comprar gastaría niveles de todos para mejorar a uno solo;
- con vidas máximas distintas, el de menos máximo le cortaría la vida al grupo.

Inventario Compartido es **un único grupo para todo el servidor**: no hay equipos ni grupos por jugador, y `PuntosDelGrupo` replica eso.

## Dónde se guardan

| Modo | Dónde | Archivo |
|---|---|---|
| Individual | Attachment `EstadisticasRPG.PUNTOS` de cada jugador | `playerdata/<uuid>.dat` |
| Grupo | `PuntosDelGrupo`, un `SavedData` del servidor (`SavedDataType` con id `estadisticasrpg:puntos_del_grupo`) | `data/estadisticasrpg/puntos_del_grupo.dat` en el mundo |

`PuntosDelGrupo` guarda `puntos` (con el mismo `PuntosDelJugador.CODEC`, así que también ignora ids desconocidos) y `jugadores_sumados` (`UUIDUtil.CODEC_SET`).

**En modo grupo el attachment de cada jugador no se lee ni se escribe** (salvo en `alEntrar`, ver abajo). Si se saca Inventario Compartido, cada uno vuelve a tener los suyos tal como estaban.

## Todo pasa por puntos() y guardar()

**`Mejoras.puntos(jugador)` y `Mejoras.guardar(jugador, puntos)` son los únicos que saben de los dos modos.** El resto (`comprar`, `dar`, `reiniciar`, el menú, el comando `ver`) llama a esos dos. **No leas `getAttachedOrCreate(PUNTOS)` directo**: en modo grupo leerías puntos viejos.

| | Individual | Grupo |
|---|---|---|
| `guardar` | `setAttached` + `aplicar(jugador)` | `PuntosDelGrupo.cambiar` + **`aplicarATodos`** |
| `reiniciar(jugador)` | Los de ese jugador | **Los de todos** |
| `alReaparecer` | Copia el attachment del viejo | No copia nada: los puntos ya están en el grupo |
| Curar al comprar vida | Al que compró | Al que compró. Inventario Compartido reparte esa curación al resto, como cualquier otra |

`aplicarATodos` sólo llega a los conectados: los que entran después toman los puntos en `alEntrar`.

## Los puntos de antes se suman una sola vez

`Mejoras.alEntrar` (en `JOIN`) llama a `PuntosDelGrupo.sumarIndividuales(uuid, attachment)`:

- **Por estadística, el máximo entre lo del grupo y lo del jugador, no la suma.** Sumar dejaría pasar el tope (3 + 8 de vida sobre un tope de 10) y regalaría puntos.
- **Una sola vez por jugador** (`jugadores_sumados`). Si se suma cada vez, un `reiniciar` del grupo se desharía solo cuando alguien con puntos viejos reconecta.
- Si eso cambió los puntos del grupo, `aplicarATodos`; si no, `aplicar` al que entró.

## Textos

En modo grupo, el comando dice "del grupo" en vez de "de <jugador>" (`ComandoEstadisticas.deQuien`), y la botella de experiencia del menú avisa que los puntos son de todos. `dar` y `reiniciar` siguen pidiendo un jugador: sirve cualquiera.

## No verificado con los dos mods a la vez

Los gametests fuerzan el modo con `usarPuntosDelGrupo(true)` sin Inventario Compartido cargado (ver [Tests del grupo](../tests/grupo.md)). **Falta probar a mano, con los dos jars en un servidor**, que la experiencia gastada le baje a todos y que la curación del corazón nuevo se reparta.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Tests del grupo](../tests/grupo.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
