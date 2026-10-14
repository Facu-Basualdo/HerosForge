# Tests de la pantalla de configuración

[← Índice Tests](INDICE.md)

`ConfiguracionClientGameTest` (entrypoint `fabric-client-gametest`, `gradlew runClientGameTest`): un cliente de verdad con un mundo singleplayer. **Abre la pantalla con `context.setScreen`**, porque Mod Menu no está en el entorno de prueba: la integración con Mod Menu (el entrypoint) no se prueba acá. Deja capturas en `build/run/clientGameTest/screenshots`.

## Qué cubre

- «treinta» en *Experiencia por punto* deja **Guardar** apagado.
- 45 se guarda y `HerosForge.config().experienciaPorPunto()` del servidor integrado da 45 (con la pantalla pausando el juego).
- *Devolución al bajar* en 5: `cargar` avisa y usaría 0,5, así que no se guarda. La pantalla queda abierta y el archivo sigue con `0.5` y con el 45 de antes.
- Desde **Pantalla propia…**, apagar *Botón en el inventario* y guardar deja `HerosForgeCliente.config().botonEnInventario()` en false.

## Gotchas

- **El cliente de prueba arranca en inglés** (`lang:en_us`): el test pone `getLanguageManager().setSelected("es_ar")` antes de crear el mundo para buscar los campos por su nombre en español. Eso no recarga los recursos de vanilla: en las capturas **Cancel** y **ON** siguen en inglés, y los nombres de los mobs también.
- **Los campos no están en `screen.children()` directamente**: están adentro del contenedor de `ScrollableLayout`. El test recorre los `ContainerEventHandler` y filtra los `EditBox` por `getMessage()` (el nombre de la fila). `clickScreenButton("Guardar")` sí lo encuentra, porque está en el pie.
- **`CycleButton.onPress` necesita un `InputWithModifiers` de verdad** (`new KeyEvent(InputConstants.KEY_RETURN, 0, 0)`): con null revienta en `hasShiftDown()`. Los `CycleButton` se buscan por su valor (`getValue()`), no por posición: los sí/no también son `CycleButton`.
- **`runClientGameTest` borra `build/run/clientGameTest/config` antes de arrancar**: el test deja valores cambiados, y la corrida siguiente tiene que empezar con los de por defecto.

`PantallaClientGameTest` también pone el cliente en `es_ar` al arrancar: desde la 0.15.0 la pantalla de estadísticas sigue el idioma del cliente y buscaba los botones en español con el cliente en inglés.

## Ver también

- [Mod: pantalla de configuración](../mod/pantalla-de-configuracion.md)
