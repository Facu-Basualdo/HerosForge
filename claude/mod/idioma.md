# Idioma de los textos

[← Índice Mod](INDICE.md)

**Todo texto que ve un jugador sale de `assets/herosforge/lang/<idioma>.json`, en el idioma de ese jugador.** Si un jugador con el juego en inglés ve "Daño", el texto se armó con el idioma de la config en vez del suyo; si ve `herosforge.estadistica.danio`, falta la clave.

## Quién arma cada texto

| Texto | Dónde | Idioma |
|---|---|---|
| Menú de cofre, mensajes de compra y baja, `/estadisticas` | Servidor (`Idioma.tr`) | El que informa el cliente del jugador (`clientInformation().language()`); el de quien corrió el comando |
| Filas de la pantalla propia (nombre, rama, "ahora", "próximo") y el resultado de una compra | Servidor, en `InterfazDelCliente.estado` y `Paquetes.Resultado` | El del jugador: llegan ya armadas |
| Botones, títulos y tooltips de la pantalla propia, el botón del inventario | Cliente (`IdiomaDelCliente`) | El elegido en el juego (`LanguageManager.getSelected()`), leído cada vez: se puede cambiar con la pantalla abierta |
| La tecla en Controles | Vanilla, con las claves `key.herosforge.*` | El del cliente |

Los dos caminos llegan al mismo idioma, porque el servidor lee el que el cliente le informa. `Idioma.resolver` lleva cualquier `es*` a `es_ar` y cualquier `en*` a `en_us`; uno que no tenemos (y la consola) usa `idioma` de la config (`en_us` por defecto).

**Los archivos de idioma están en `src/main/resources`, no en `src/client`.** El servidor los necesita para armar los textos, y con `splitEnvironmentSourceSets` los recursos de `src/client` no están en el classpath del servidor de desarrollo ni de los gametests. Con los dos juegos de archivos a la vez, `jar` falla por la entrada duplicada.

## Estadísticas y ramas

`Estadistica` ya no tiene el nombre ni la descripción en español: tiene un `Efecto` (factor, signo y si concuerda en número) y los textos van en el archivo.

| Clave | Qué es |
|---|---|
| `herosforge.estadistica.<id>` | El nombre |
| `herosforge.estadistica.<id>.corto` | Opcional: el de la pantalla de dos columnas. Si no está en ese idioma, `nombreCorto` usa el nombre |
| `herosforge.estadistica.<id>.efecto` | La plantilla del efecto, con el número ya con signo ("+6 %") como `%s` |
| `….efecto.uno` / `….efecto.varios` | Para las que concuerdan con el número ("1 bloque", "2 bloques"): `Efecto.plural()` |
| `herosforge.rama.<id>` y `.resumen` | El nombre de la rama y la línea del ícono en el cofre |

`describir(idioma, valor)` muestra `valor × factor` con `Idioma.numero` (coma decimal en español, punto en inglés) y el signo del `Efecto` (`+`, `−` o nada). Los `extras` son otros números fijos de la plantilla (el +50 % del crítico, el 30 % de vida de la ejecución).

**El `concuerda` del record no se puede llamar `plural`:** sería el accessor del componente y choca con el método `plural()` que arma el `Efecto`.

**Los ids no se traducen**: van en la config (`estadisticas.<id>`), en el attachment de puntos y en los argumentos de los comandos.

## Comandos en los dos idiomas

`/estadisticas` (y `/stats`) con `ver`, `subir`, `bajar`, `dar`, `reiniciar`, `recargar`, y lo mismo en inglés: `view`, `buy`, `refund`, `give`, `reset`, `reload`. El árbol se arma una vez por idioma en `ComandoEstadisticas.registrar` (record `Nombres`), sobre el mismo nodo raíz: `/stats` redirige a todo.

Los errores de un comando se crean en cada llamada y no como constantes: el mensaje depende de quién lo corrió.

Los avisos de la config (`ArchivoDeConfiguracion`) siguen en español.

## Tests

`textosEnLosDosIdiomas` verifica que los dos archivos tengan las mismas claves, que cada estadística tenga nombre y efecto (y que `describir` no devuelva la clave), que cada rama tenga nombre y resumen, el singular y el plural, la coma decimal y el nombre corto. Los jugadores de prueba informan `en_us`, así que `elEstadoParaLaPantallaTieneTodo` espera las filas en inglés; los que comparan efectos o mensajes piden el idioma explícito.

## Ver también

- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Menú de estadísticas](menu.md)
- [Pantalla propia del cliente](pantalla.md)
- [Configuración](configuracion.md): la clave `idioma`.
