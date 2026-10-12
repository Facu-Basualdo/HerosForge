# Probar en el juego

[← Hub](../../CLAUDE.md)

Los gametests cubren la lógica, pero no muestran cómo se ve el menú en un cliente, si la barra de experiencia baja en pantalla, ni si +20 % de velocidad marea. Para eso hace falta un servidor y un cliente.

## Pasos

1. `gradlew runServer` una vez. Crea `run/` y se cierra pidiendo la EULA.
2. En `run/eula.txt` poné `eula=true` (es aceptar la EULA de Mojang: lo hace la persona, no el agente).
3. En `run/server.properties` poné **`online-mode=false`**: los clientes de desarrollo no tienen cuenta autenticada y sin esto el servidor los rechaza.
4. `gradlew runServer` de nuevo, y `op Player…` en la consola del servidor para poder usar los comandos de op.
5. `gradlew runClient` (y `gradlew runClient2` si querés dos jugadores) → *Multijugador → Conexión directa → `localhost`*.

**Después de cambiar código hay que reiniciar el cliente y el servidor:** no hay hot reload.

`run/` y `run-client2/` están en `.gitignore`.

## Comandos útiles

| Comando | Para qué |
|---|---|
| `/xp add @s 300 points` | Tener experiencia para comprar (10 puntos con el costo por defecto) |
| `/estadisticas` | Abrir el menú |
| `/estadisticas dar <jugador> <estadistica> <cantidad>` | Saltear el costo (op) |
| `/estadisticas recargar` | Probar cambios en `run/config/herosforge.json` sin reiniciar el servidor (op) |
| `/attribute @s minecraft:max_health get` | Ver el valor real del atributo con los puntos |
| `/kill @s` | Confirmar que los puntos y la vida máxima siguen después de reaparecer |

## Qué mirar

- Que la barra de experiencia baje **en el momento** de comprar, sin reconectar.
- Que la vida nueva aparezca llena al comprar vida, y que al reaparecer tengas la vida máxima con los puntos.
- En el menú: que el ítem no se pueda sacar ni quede "fantasma" en el cursor, y que el lore se actualice después de cada compra.
- **Con un cliente vanilla sin el mod:** todo lo anterior tiene que verse igual, y los textos tienen que estar en castellano (no claves de traducción crudas). El mod no tiene que hacer falta del lado del cliente.
- **Con el mod en el cliente (`runClient`):** la K y el botón del inventario abren la [pantalla propia](../mod/pantalla.md), `/estadisticas` también, y **Ver como cofre** abre el cofre. Probá a la vez un cliente con y otro sin el mod: el vanilla tiene que seguir con el cofre. Con `"usarPantalla": false` en `run/config/herosforge-cliente.json`, la K abre el cofre. La pantalla en singleplayer ya la prueba `runClientGameTest`; lo que no prueba es un servidor dedicado.

## Ver también

- [Entorno Java y Gradle](entorno-java-gradle.md)
- [Minecraft 26.x sin ofuscar](minecraft-26-sin-ofuscar.md)
- [Estadísticas (mod)](../mod/estadisticas.md) y [sus tests](../tests/estadisticas.md): lo que los gametests no pueden ver
