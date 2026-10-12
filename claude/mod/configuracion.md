# Configuración

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: después de actualizar el mod, un servidor sigue con los números viejos; un dueño de servidor escribe mal un tope y pierde todos sus números; después de actualizar el mod, una estadística nueva no aparece en el archivo y nadie sabe que existe; `recargar` cambia los números pero los jugadores conectados siguen con los viejos; el menú abierto muestra el costo anterior.

## Dónde y cómo

`config/herosforge.json` (`HerosForge.archivoDeConfiguracion()`, con `FabricLoader.getConfigDir()`). Se lee en `onInitialize` y con `/estadisticas recargar` (op).

```json
{
  "experienciaPorPunto": 30,
  "devolucionAlBajar": 0.5,
  "estadisticas": {
    "vida": { "porPunto": 0.6, "tope": 100 },
    ...
  }
}
```

**`porPunto` va en las unidades del atributo de vanilla**, no en las que ve el jugador: la vida en medios corazones (`0.6` = 0,3 corazones) y las de `ADD_MULTIPLIED_BASE` como fracción (`0.002` = +0,2 %). `experienciaPorPunto` va en puntos de experiencia, no en niveles (ver [Estadísticas](estadisticas.md#comprar-mejorascomprar)). `devolucionAlBajar` es la fracción del costo que se devuelve al bajar un punto, **de 0 a 1**: con más, bajar y volver a subir fabricaría experiencia. Las claves de `estadisticas` son los `Estadistica.id()`: **por eso un id no se renombra nunca**, igual que en el attachment (ver [Estadísticas](estadisticas.md#formato-en-disco)).

`Configuracion.POR_DEFECTO` sigue siendo la fuente de los valores por defecto. El archivo sólo los pisa.

**Cambiar `POR_DEFECTO` no cambia los servidores que ya tienen el archivo:** el archivo se escribió con los valores de la versión anterior y se siguen leyendo ésos. Sólo se completan los campos que faltan. Para tomar los nuevos hay que borrarlo (lo dice el README). Lo mismo pasa en los gametests: ver [Tests de configuración](../tests/configuracion.md#archivos-de-prueba).

## Leer: un error no tira abajo todo

`ArchivoDeConfiguracion.cargar(Path)` devuelve una `Lectura(config, avisos)`. La config es `null` sólo si el archivo no se puede usar.

| Caso | Resultado | ¿Reescribe el archivo? |
|---|---|---|
| No existe | `POR_DEFECTO` | Sí, lo crea |
| JSON roto, o no es un objeto | `config == null` + 1 aviso; **quien llama se queda con la config que tenía** | **No**: es el archivo del dueño, que lo arregle él |
| Campo inválido (no es número, `porPunto <= 0`, entero negativo o con decimales, `devolucionAlBajar` fuera de 0–1) | Ese campo con el valor por defecto + aviso | No |
| Id que no es una estadística | Se ignora + aviso | No |
| Falta una estadística, `experienciaPorPunto`, `devolucionAlBajar` o `estadisticas`, **sin avisos** | Lo que falta con el valor por defecto | **Sí, con lo leído** (no con los valores por defecto), así aparecen las estadísticas nuevas de una actualización |

- **Con avisos no se reescribe nunca,** aunque falte algo: si no, el valor mal escrito se reemplazaría por el de por defecto y el dueño perdería lo que quiso poner. Primero que corrija; en la próxima lectura sin avisos se completa.
- **`porPunto` tiene que ser mayor que 0:** con uno negativo, el jugador pagaría experiencia para empeorar.
- **Las claves de antes de la 0.7.0 (`costoPorPunto` y `puntosPorAumento`, en niveles) no se leen ni dan aviso:** el archivo no tiene `experienciaPorPunto`, así que se reescribe y se van solas. Con un aviso no se reescribiría nunca y quedarían para siempre.
- `experienciaPorPunto` 0 es válido (gratis), y `tope` 0 también (la estadística no se puede comprar y en el menú aparece como vidrio gris).
- Al arrancar, los avisos van al log con `[config]`; con `recargar`, al que ejecutó el comando, en amarillo.

## Aplicar: los conectados ya, los demás al entrar

`HerosForge.usar(config, servidor)` cambia la config y llama a `Mejoras.aplicar` para **cada jugador conectado**. Los que no están conectados la toman en `JOIN`, que también llama a `aplicar` (ver [Estadísticas](estadisticas.md)).

Como los puntos son la fuente de verdad, cambiar la config **nunca toca los puntos**, sólo lo que valen:

- **Bajar un tope:** los puntos de más se guardan pero no suman (`Mejora.efecto` usa `min(puntos, tope)`). Si se vuelve a subir, vuelven a contar sin pagar de nuevo.
- **Bajar la vida:** `aplicar` recorta la vida actual en el momento.

**El menú abierto se redibuja solo**, porque `MenuDeEstadisticas.broadcastChanges` compara `HerosForge.config()` con la última config que dibujó, **por identidad** (`!=`). Cada `usar` pone un objeto nuevo, así que siempre redibuja, aunque los valores sean iguales. Ver [Menú de estadísticas](menu.md).

## La config es global

`HerosForge.config` es un `static volatile`: hay una sola por servidor. **Un test que la cambie tiene que restaurarla en un `finally`** (ver [Tests de configuración](../tests/configuracion.md)).

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Menú de estadísticas](menu.md)
- [Tests de configuración](../tests/configuracion.md)
- [Idioma](idioma.md): de dónde salen los textos y en qué idioma.
