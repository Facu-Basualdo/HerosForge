# Tests de las ramas

[← Índice Tests](INDICE.md)

Síntomas que esta nota evita: un test de daño al jugador pasa siempre aunque la estadística no ande (el jugador no recibe nada); la flecha o el ítem del test "no existen"; un test de probabilidad falla una vez de cada tantas.

`RamasGameTest` prueba las 25 estadísticas de la 0.13.0 (ver [Ramas](../mod/ramas.md)). Está registrada en `src/gametest/resources/fabric.mod.json` junto con `HerosForgeGameTest`, y usa sus helpers (`entrar`, `salir`, `assertCerca`, `ponerExperiencia`), que son de paquete.

## Lo que hay que saber antes de escribir uno

- **Un jugador recién conectado es invulnerable** hasta que su cliente avisa que cargó el mundo: `ServerPlayer.isInvulnerableTo` pregunta `connection.hasClientLoaded()`. Los de prueba no tienen cliente, así que **`hurtServer` no les hace nada** y los asserts de "pega entero" fallan en silencio. `vulnerable(helper, nombre)` llama a `connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket())`, que es lo que manda el cliente de verdad.
- **Un golpe por jugador:** después de cada golpe vanilla da medio segundo de invulnerabilidad (`invulnerableTime`, privado). Para medir dos golpes, dos jugadores.
- **El jugador aparece en el spawn del mundo, que puede no estar cargado**, y ahí `addFreshEntity` no agrega nada: la flecha que dispara o el ítem que atrae no existen y el test falla a veces sí y a veces no. `enElTest(helper, nombre, x)` lo mueve adentro de la estructura del test con `snapTo(helper.absoluteVec(…))`.
- **Lo que depende del azar se prueba al 100 %:** `conConfig` pone `porPunto` 0,01 (100 puntos = 1) y restaura en `finally`, como el resto de los tests que cambian la config (ver [Tests de configuración](configuracion.md)). Las demás se miden contra un jugador con 0 puntos, que nunca sortea.
- **El golpe de un mob escala con la dificultad** (`mobAttack`, en `Player.hurtServer`): la represalia se mide contra lo que entró de verdad (`20 − vida`), no contra el número que se pasó.
- **Con la mano vacía, `getInventory().add` pone el ítem en la mano.** Para disparar, primero el arco en la mano y después las flechas: al revés, el arco pisa las flechas y `releaseUsing` no dispara.

## Qué cubre

| Test | Qué verifica |
|---|---|
| `atributosNuevosAlMaximo` | Los 11 atributos nuevos con 100 puntos (también los que restan) y que `reiniciar` los saca |
| `descripciones` | Textos con signo menos, corazones del escudo y el ×1,5 del crítico |
| `criticoYEjecucion` | 8 de daño pega 12 con el crítico seguro, y con la ejecución sólo debajo del 30 % de vida |
| `resistenciasBajanElDanioElemental` | Fuego y magia −30 %; el daño genérico entero |
| `esquivaSoloAtaques` | El golpe de un gólem no entra; el daño sin causante sí |
| `represaliaDevuelveElGolpe` | 30 % de lo que entró vuelve al gólem; una flecha no |
| `escudoSeRecargaFueraDePelea` | Medio corazón por llamada hasta 8, se come el golpe, no se recarga recién golpeado y se corta al bajar puntos |
| `destrezaTensaMasRapido` | `Combate.tensar` (10 ticks valen 14) y, con un arco de verdad, la flecha sale más rápido |
| `metabolismoBajaMenosElHambre` | 4,4 de cansancio gastan saturación sin puntos; con −50 % no |
| `nutricionYGloton` | Un filete da 12 en vez de 8; comer tarda la mitad y el arco no cambia |
| `tenacidadAcortaLoNegativo` | El veneno dura la mitad, la velocidad entera |
| `imanAtraeLoQueEstaCerca` | El ítem va hacia el jugador; sin puntos o con demora para levantar, no |
| `cuidadoDelEquipoSalvaDurabilidad` | `hurtAndBreak(5)` no gasta nada con el cuidado seguro |
| `sabiduriaSoloEnLosOrbes` | Un orbe de 100 da 125; bajar un punto devuelve lo de siempre |
| `regateoBajaLosPrecios` | −30 % de 10 esmeraldas; una esmeralda no baja de 1 |

**Los mixins se prueban también al cargar:** con `defaultRequire: 1`, un punto de inyección que no existe tira abajo el servidor de prueba antes del primer test. El de los aldeanos (`regateoBajaLosPrecios`) sólo prueba `Oficios.regatear`: armar un aldeano con profesión y ofertas para comerciar no vale lo que cuesta, y el mixin se verifica al cargar.

## Ver también

- [Ramas y las estadísticas sin atributo](../mod/ramas.md)
- [Tests de estadísticas](estadisticas.md)
- [Tests del menú](menu.md)
