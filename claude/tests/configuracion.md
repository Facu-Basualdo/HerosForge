# Tests de configuración

[← Índice Tests](INDICE.md)

Síntoma que esta nota evita: un test cambia la config global, falla a la mitad, y otros tests que no tienen nada que ver empiezan a fallar con números raros.

| Test | Qué verifica |
|---|---|
| `configNuevaSeCreaConLosValoresPorDefecto` | Sin archivo: lo crea con `POR_DEFECTO`, y leerlo de nuevo da lo mismo, sin avisos |
| `configRespetaLoCambiadoYAvisaLoInvalido` | Respeta lo válido (incluido `experienciaPorPunto`) · `porPunto` negativo y `tope` que no es número vuelven al valor por defecto · ignora un id inexistente · 3 avisos · con avisos **no reescribe** el archivo |
| `configSinAvisosAgregaLasQueFaltan` | Con un archivo de la 0.6.0 (`costoPorPunto` y `puntosPorAumento` en niveles, y sólo `vida`): conserva la vida, agrega `experienciaPorPunto` y todas las demás, y saca las claves viejas |
| `configRotaNoSeUsa` | JSON cortado: `ok()` falso, 1 aviso, el archivo queda como estaba |
| `cambiarLaConfigSeAplicaALosConectados` | `usar` reaplica a un jugador conectado · bajar el tope no borra puntos · `comprar` cobra el `experienciaPorPunto` nuevo |
| `comandos` (parte de consola) | `dar` y `recargar` desde `servidor.createCommandSourceStack()`, que es op |

## Archivos de prueba

**Los tests de lectura usan `Files.createTempDirectory`, nunca el archivo real.** El servidor de gametest crea el suyo en `build/run/gameTest/config/herosforge.json` al arrancar, y `recargar` (en el test `comandos`) lee ése. Si un test lo editara, `recargar` cargaría esos valores para todos los tests que vienen después.

**`runGameTest` borra `build/run/gameTest/config` antes de arrancar** (un `doFirst` en `build.gradle`). Ese archivo sobrevive entre corridas, y el mod no pisa un archivo que ya existe: después de cambiar `POR_DEFECTO`, los tests corrían con los valores viejos y fallaban con números que no tenían sentido (topes de 10 con el código pidiendo 100).

## Cambiar la config global

`HerosForge.usar` cambia la config de **todo el servidor**, y los gametests corren en el mismo servidor. Es seguro sólo porque:

1. Cada `@GameTest` sin secuencia (`startSequence`) corre de punta a punta en una sola llamada, sin que se intercale otro test.
2. **El test restaura `POR_DEFECTO` en un `finally`.** Si una aserción falla, la excepción sale después de restaurar.

**Un test que cambie la config no puede usar `thenIdle` ni `startSequence`** mientras la config está cambiada: entre ticks corren los otros tests.

## Ver también

- [Configuración](../mod/configuracion.md)
- [Tests de estadísticas](estadisticas.md): cómo correr comandos y armar jugadores
- [Tests del grupo](grupo.md): el mismo cuidado con el modo y los puntos del grupo, que también son globales
