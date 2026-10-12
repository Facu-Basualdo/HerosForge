# Puntos del grupo (con Soul Link)

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: con Soul Link, el que compró 10 corazones queda con 20 de vida porque otro tiene menos máximo; uno gasta niveles de todos y sólo él mejora; al reconectar, a un jugador le vuelven los puntos de antes de un `reiniciar`; un agente busca los puntos en el attachment y están en otro lado; con Soul Link en grupos de 2 y 2, lo que compra un grupo lo recibe el otro.

## Cuándo se activa

**Si `FabricLoader.isModLoaded("soullink")` (`HerosForge.SOUL_LINK`), en `onInitialize` se llama a `Mejoras.usarPuntosDelGrupo(true)`.** No hay opción en la config. `CompatibilidadSoulLink` resuelve por reflexión los métodos de `com.soullink.api.SoulLinkApi` sólo cuando está cargado: no hay dependencia de compilación ni clases de Soul Link empaquetadas.

El porqué: Soul Link comparte la experiencia (la reparte por diferencias en `SharedExperience.sync`) y la vida (en `SharedVitals.syncHealth`, recortada contra la vida máxima de **cada uno**). Con puntos individuales:

- comprar gastaría niveles de todos para mejorar a uno solo;
- con vidas máximas distintas, el de menos máximo le cortaría la vida al grupo.

## Un grupo de puntos por grupo de Soul Link

**Desde Soul Link 3.0.0 hay grupos vinculados** (`/soullink grupo rojo Ana Beto`): cada grupo comparte su experiencia y su vida, y no las del resto. Por eso **los puntos son por grupo de Soul Link**, con su id (`CompatibilidadSoulLink.grupo(jugador)` → `SoulLinkApi.groupOf`). Los que no tienen grupo están en `general`, y sin Soul Link (o con los puntos individuales) `grupo()` devuelve siempre `general`.

**Al cambiar de grupo se vuelven a aplicar los modificadores.** En `onInitialize`, `CompatibilidadSoulLink.alCambiarDeGrupo(Mejoras::aplicar)` registra un listener con `SoulLinkApi.onGroupChange`, que se llama con el jugador ya en el grupo nuevo: le saca los modificadores del grupo viejo y le pone los del nuevo. Sin eso, quedaría con la vida máxima de su grupo anterior hasta reconectar.

**Hero's Forge 0.14.0 necesita Soul Link 3.0.0 o más nuevo** (`"breaks": {"soullink": "<3.0.0"}` en `fabric.mod.json`): las versiones viejas no tienen `SoulLinkApi`. Soul Link declara lo mismo al revés (`herosforge <0.14.0`).

## Dónde se guardan

| Modo | Dónde | Archivo |
|---|---|---|
| Individual | Attachment `HerosForge.PUNTOS` de cada jugador | `playerdata/<uuid>.dat` |
| Grupo | `PuntosDelGrupo`, un `SavedData` del servidor (`SavedDataType` con id `herosforge:puntos_del_grupo`) | `data/herosforge/puntos_del_grupo.dat` en el mundo |

`PuntosDelGrupo` guarda `puntos` (los del grupo general, con el mismo `PuntosDelJugador.CODEC`, así que también ignora ids desconocidos), `grupos` (los demás, por id de grupo de Soul Link) y `jugadores_sumados` (`UUIDUtil.CODEC_SET`). **El general sigue en `puntos`, como cuando había un solo grupo**: un mundo de la 0.13 se lee sin migrar nada. Un grupo que nunca compró no tiene entrada y `puntos(grupo)` da vacío.

**En modo grupo el attachment de cada jugador no se lee ni se escribe** (salvo en `alEntrar`, ver abajo). Si se saca Soul Link, cada uno vuelve a tener los suyos tal como estaban.

## Todo pasa por puntos() y guardar()

**`Mejoras.puntos(jugador)` y `Mejoras.guardar(jugador, puntos)` son los únicos que saben de los dos modos.** El resto (`comprar`, `dar`, `reiniciar`, el menú, el comando `ver`) llama a esos dos. **No leas `getAttachedOrCreate(PUNTOS)` directo**: en modo grupo leerías puntos viejos.

| | Individual | Grupo |
|---|---|---|
| `guardar` | `setAttached` + `aplicar(jugador)` | `PuntosDelGrupo.cambiar(grupo, …)` + **`aplicarATodos`** (cada uno lee los de su grupo) |
| `reiniciar(jugador)` | Los de ese jugador | **Los de todo su grupo** |
| `alReaparecer` | Copia el attachment del viejo | No copia nada: los puntos ya están en el grupo |
| Curar al comprar vida | Al que compró | Al que compró. Soul Link reparte esa curación al resto, como cualquier otra |

`aplicarATodos` sólo llega a los conectados: los que entran después toman los puntos en `alEntrar`.

## Los puntos de antes se suman una sola vez

`Mejoras.alEntrar` (en `JOIN`) llama a `PuntosDelGrupo.sumarIndividuales(grupo, uuid, attachment)`:

- **Por estadística, el máximo entre lo del grupo y lo del jugador, no la suma.** Sumar podría pasar el tope (60 + 80 de vida sobre un tope de 100) y regalaría puntos.
- **Una sola vez por jugador** (`jugadores_sumados`), en todo el servidor y no por grupo. Si se suma cada vez, un `reiniciar` del grupo se desharía solo cuando alguien con puntos viejos reconecta; y si fuera por grupo, cambiar de grupo llevaría sus puntos viejos a cada grupo nuevo.
- Si eso cambió los puntos del grupo, `aplicarATodos`; si no, `aplicar` al que entró.

## Textos

En modo grupo, el comando dice "del grupo" en vez de "de <jugador>" (`ComandoEstadisticas.deQuien`), y la botella de experiencia del menú avisa que los puntos son de todos. `dar` y `reiniciar` siguen pidiendo un jugador: sirve cualquiera.

## Probado a mano con los dos mods

Los gametests fuerzan el modo con `usarPuntosDelGrupo(true)` sin Soul Link cargado (ver [Tests del grupo](../tests/grupo.md)). **Con los dos jars (y Bloodmoon & Bosses) en un servidor se probó a mano en la 0.9.1 (octubre de 2026) y anda:** la experiencia gastada le baja a todos y la curación del corazón nuevo se reparte. La suite de integración de 0.11.1 carga Soul Link 1.2.1 real y prueba las compras y devoluciones, recortes de vida, reinicio y recarga de topes.

## Lo que se cura

Soul Link reparte la vida **sumando la diferencia** de cada jugador en el tick. Una curación hecha a un solo jugador le llega a todos una vez; la misma curación hecha a cada uno se suma una vez por jugador. Por eso **la regeneración (`Curacion.regenerar`) cura a uno solo por grupo de Soul Link en modo grupo** (el primero de cada grupo que está vivo y en supervivencia o aventura, aunque no tenga regeneración: los puntos son los mismos), y el robo de vida cura sólo al que pegó. La vida nueva al comprar vida también se le da sólo al que compró (`Mejoras.sumar`).

## Transacciones y recortes de máximo

**Comprar sincroniza experiencia antes de validar y después de cobrar** mediante `SoulLinkApi.syncExperience`. Bajar hace lo mismo para la devolución. Sin la segunda sincronización, otra compra del mismo tick usaría la barra vieja de otro jugador. La primera incorpora experiencia pendiente de otros participantes.

**Recortar vida compensa sólo ese recorte con `SoulLinkApi.ignoreHealthChange`**, conservando el delta real de daño o curación. Después de aplicar los atributos a todos se llama `SoulLinkApi.syncHealth`: limita la barra común una vez al máximo nuevo. Esto también cubre reiniciar y recargar. No reemplazar la referencia por la vida actual: borraría daños pendientes.

El puente está verificado contra Soul Link 3.0.0. Usa reflexión cacheada sobre `SoulLinkApi`, que es lo único de Soul Link pensado para otros mods: no vuelvas a tocar sus clases internas. Si cambian esas firmas hay que actualizar el puente y correr la integración. Una incompatibilidad falla explícitamente, no continúa con compras inseguras.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Tests del grupo](../tests/grupo.md)
- [Probar en el juego](../general/probar-en-el-juego.md)
