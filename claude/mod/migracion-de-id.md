# Migración del id (estadisticasrpg → herosforge)

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: después de actualizar a la 0.12.0, un jugador entra sin sus puntos; un jugador entra con los puntos pero la vida máxima sumada dos veces; la config vuelve a los valores por defecto.

## Qué cambió

**Desde la 0.12.0 el id del mod es `herosforge`** (antes `estadisticasrpg`): paquete `com.herosforge`, jar, config, attachment, `SavedData`, canales de red, prefijo `herosforge$` de los mixins y las claves de la tecla.

| Dato | Antes | Ahora | Cómo se pasa (`MigracionDeId`) |
|---|---|---|---|
| Config del servidor | `config/estadisticasrpg.json` | `config/herosforge.json` | `migrarConfig(".json")`, lo primero de `onInitialize` |
| Config del cliente | `config/estadisticasrpg-cliente.json` | `config/herosforge-cliente.json` | `migrarConfig("-cliente.json")` en `ConfiguracionDelCliente.cargar` |
| Puntos del grupo | `world/data/estadisticasrpg/puntos_del_grupo.dat` | `world/data/herosforge/…` | `migrarMundo` en `SERVER_STARTING` |
| Puntos de cada jugador | attachment `estadisticasrpg:puntos` | `herosforge:puntos` | `migrarJugador` en `JOIN`, antes de `Mejoras.alEntrar` |
| Modificadores del jugador | `estadisticasrpg:<estadística>` | `herosforge:<estadística>` | `migrarJugador` saca los viejos; `aplicar` pone los nuevos |
| Canales de red | `estadisticasrpg:*` | `herosforge:*` | No se migran: el cliente con la pantalla tiene que actualizar el jar |

## Los puntos del jugador

**El attachment viejo (`PUNTOS_VIEJOS`) sigue registrado para siempre**, con el mismo codec. Fabric descarta al cargar un attachment cuyo id no conoce: si no estuviera registrado, un jugador que no entra desde antes del cambio perdería sus puntos la primera vez que vuelva.

`migrarJugador` corre en el mismo listener de `JOIN` que `alEntrar`, **antes**: así `alEntrar` (y `sumarIndividuales`, con los puntos del grupo) ve los puntos ya en el id nuevo.

- **"Vacío" cuenta como "no tiene"**: cualquier lectura con `getAttachedOrCreate` crea el attachment nuevo vacío, y el viejo con puntos no tiene que perder contra eso.
- **Los modificadores viejos se sacan** de cada atributo de cada estadística. Vanilla guarda los modificadores permanentes en el NBT con su id: si quedaran, `aplicar` pondría los nuevos al lado y cada estadística sumaría dos veces (una vida de 20 + 6 + 6).

## Los archivos

`moverSiFalta`: si existe el viejo y no el nuevo, lo **mueve**; si ya hay uno nuevo, no toca nada; si falla, tira la excepción y no arranca (mejor que arrancar sin los puntos y pisarlos al guardar). La config se mueve antes de leerla, así el dueño del servidor no pierde sus números.

## Lo que no se migra

- **La pantalla propia del cliente:** los canales se llaman `herosforge:*`. Un cliente con el jar viejo no los registra y usa el cofre, como un cliente vanilla.
- **La tecla:** la clave pasó a `key.herosforge.abrir`. Quien la había cambiado de K la tiene que volver a asignar.

## Probado

- `unJugadorDelIdViejoConservaSusPuntos`: un jugador guardado con el attachment y el modificador viejos entra con sus puntos, sin lo viejo y con la vida sumada una sola vez; el archivo del grupo se mueve y no pisa uno nuevo.
- Con una copia del mundo de un servidor real (Server3): los puntos del grupo y la config quedaron iguales con el id nuevo.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Puntos del grupo](grupo.md)
- [Pantalla propia del cliente](pantalla.md)
- [Configuración](configuracion.md)
