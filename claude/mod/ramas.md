# Ramas y las estadísticas sin atributo

[← Índice Mod](INDICE.md)

Síntomas que esta nota evita: el cliente sigue viendo los precios de los aldeanos sin el descuento; con dos jugadores con represalia el servidor se cuelga rebotando espinas; la sabiduría suma también a la experiencia que devuelve bajar un punto; el escudo rellena la absorción de la manzana dorada; comer con el glotón al máximo no termina nunca; el imán devuelve a la mano lo que el jugador acaba de tirar.

## Las ramas

Desde la 0.13.0 cada `Estadistica` tiene una `Rama` (guerrero, guardián, superviviente, explorador y artesano), que reemplaza al viejo `combate()`. **Sólo ordenan**: no hay requisitos ni bloqueos, cualquier estadística se compra desde el principio. El enum `Estadistica` va ordenado por rama, y dentro de cada una de lo básico a lo fuerte. `Rama.estadisticas()` las devuelve en ese orden, y lo usan el menú, la pantalla y `/estadisticas ver`.

- **Hasta 9 por rama:** es una fila del cofre (lo atrapa el gametest `cadaEstadisticaTieneConfigYSlot`). Hoy son 8, 9, 8, 7 y 8.
- El id de la rama viaja en el paquete del estado (ver [Pantalla](pantalla.md)) y nunca se guarda: se puede renombrar, pero un cliente viejo dejaría de reconocerla.

## Lo que no es un atributo

Vanilla no tiene atributos para el hambre, los críticos, la esquiva, los precios ni la experiencia, y Fabric API no tiene un evento para cambiar el daño (sólo para cancelarlo). Estas estadísticas se construyen sin atributos, como el daño a distancia (ver [Estadísticas](estadisticas.md#daño-a-distancia-un-mixin-no-un-atributo)), y las aplican tres clases con sus mixins. La lógica está en la clase y el mixin sólo la llama: así se prueba sin depender del punto de inyección.

| Estadística | Clase | Dónde engancha |
|---|---|---|
| Crítico, ejecución, resistencias | `Combate.danio` | `DanioMixin`: `@ModifyVariable` del `float` en el `HEAD` de `LivingEntity.hurtServer`, antes de la armadura |
| Esquiva | `Combate` | `ServerLivingEntityEvents.ALLOW_DAMAGE` |
| Represalia | `Combate` | `AFTER_DAMAGE` (también anota el tick del último golpe para el escudo) |
| Escudo | `Combate.recargarEscudos` | `END_SERVER_TICK` cada 20 ticks, más el atributo `MAX_ABSORPTION` |
| Destreza | `Combate.tensar` | `TensarMixin`: el `int tiempoRestante` en el `HEAD` de `releaseUsing` de `BowItem` **y** `CrossbowItem` |
| Metabolismo | `Supervivencia.cansancio` | `HambreMixin`: el `float` en el `HEAD` de `Player.causeFoodExhaustion` |
| Nutrición | `Supervivencia.nutrir` | `ComidaMixin`: `TAIL` de `FoodProperties.onConsume` |
| Glotón | `Supervivencia.tiempoDeComer` | `UsoMixin`: `TAIL` de `LivingEntity.startUsingItem` (con `@Shadow useItemRemaining`) |
| Tenacidad | `Supervivencia.efecto` | `UsoMixin`: el `MobEffectInstance` en el `HEAD` de `addEffect(MobEffectInstance, Entity)` |
| Imán | `Oficios.atraer` | `END_SERVER_TICK`, cada tick |
| Cuidado del equipo | `Oficios.durabilidad` | `DurabilidadMixin`: `RETURN` de `ItemStack.processDurabilityChange` |
| Regateo | `Oficios.regatear` | `AldeanoMixin`: en `Villager.updateSpecialPrices`, **antes** de `getTradingPlayer()` |
| Sabiduría | `Oficios.sabiduria` | `OrbeMixin`: el `int` que se guarda en `ExperienceOrb.playerTouch` (lo que queda después de Reparación) |

- **`@Local` es de MixinExtras**, que viene con Fabric Loader (0.5.5): sirve para leer otros argumentos del método desde un `@ModifyVariable`. Las firmas están verificadas con `javap` en 26.3.
- **Todo sólo para `ServerPlayer`.** `Player` y `LivingEntity` también corren en el cliente: los mixins preguntan `(Object) this instanceof ServerPlayer`.
- **Probabilidades y reducciones con `Mejoras.fraccion`**, que corta en 1: con un tope enorme en la config, la esquiva pasaría del 100 % o el hambre sería negativa. Los multiplicadores que suben (ejecución, sabiduría, destreza) usan `Mejoras.efecto` sin corte.
- Los números que no son por punto (el ×1,5 del crítico, el 30 % de la ejecución, los 5 segundos y el medio corazón por segundo del escudo) son constantes en `Configuracion`, no en el archivo.

## Lo que rompería vanilla si no estuviera

- **Regateo antes de mandar las ofertas.** `updateSpecialPrices` aplica los descuentos y **al final se las manda al jugador** (`sendMerchantOffers`). En el `TAIL`, el servidor cobraría con descuento pero el cliente mostraría el precio sin él. Vanilla vuelve todo a 0 con `resetSpecialPrices` al abrir y cerrar el comercio, igual que con Héroe de la aldea.
- **La represalia no devuelve espinas** (`DamageTypes.THORNS`): con dos jugadores con represalia, cada uno le devolvería al otro para siempre. Tampoco devuelve lo que no es cuerpo a cuerpo: el que pega (`getDirectEntity`) tiene que ser el que lo causa (`getEntity`); con una flecha son distintos.
- **El crítico tampoco sale de una espina**: las espinas de la represalia tienen al jugador de causante y entrarían a `Combate.danio`.
- **La esquiva sólo esquiva algo con causante** (`getEntity() != null`) y nunca `BYPASSES_INVULNERABILITY` (`/kill`): la lava, el vacío y el hambre no se esquivan.
- **El escudo se recarga hasta lo comprado, no hasta `getMaxAbsorption()`**: una manzana dorada sube el máximo con su propio modificador, y el escudo rellenaría también eso. Al bajar puntos, `Mejoras.aplicar` corta la absorción al máximo nuevo (como la vida).
- **El glotón nunca deja 0 ticks:** vanilla termina de comer cuando la cuenta pasa **por** 0 en `updateUsingItem`; empezando en 0 baja a −1 y no termina nunca. Sólo toca lo que tiene `DataComponents.CONSUMABLE` (comida, pociones, leche): el arco usa la misma cuenta y no se acelera.
- **El cliente vanilla no se entera del glotón ni de la destreza**, y no hace falta: el servidor termina de comer y manda el evento 9 (`ServerPlayer.completeUsingItem`), y la fuerza de la flecha la decide el servidor al soltar. El cliente sigue mostrando la animación de siempre.
- **La sabiduría no toca `giveExperiencePoints`:** `Mejoras.bajar` también lo usa para devolver experiencia, y ahí sumaría. Además, la experiencia es la moneda de los puntos: por eso el tope por defecto es +25 %.
- **El imán no atrae con demora para levantar** (`hasPickUpDelay`): un ítem recién tirado volvería solo a la mano. Tampoco atrae agachado, para poder dejar cosas. Pone `needsSync`: si no, el cliente ve el ítem quieto hasta la próxima actualización de movimiento.
- **La nutrición suma después de vanilla** con otro `FoodData.eat`: la saturación igual se corta contra el alimento. El alimento extra se redondea.
- **La tenacidad no acorta lo infinito** (faros, comandos con `infinite`), ni lo que no es `HARMFUL`.

## Los atributos nuevos

| Estadística | Atributo | Nota |
|---|---|---|
| Dureza | `ARMOR_TOUGHNESS` | |
| Barrido | `SWEEPING_DAMAGE_RATIO` | Vanilla sólo barre con espadas |
| Golpe contundente | `ATTACK_KNOCKBACK` | |
| Antiexplosión | `EXPLOSION_KNOCKBACK_RESISTANCE` | Sólo el empuje: el daño de las explosiones es de Resistencias |
| Escudo | `MAX_ABSORPTION` | Sin él, `setAbsorptionAmount` corta en 0 |
| Ignífugo | `BURNING_TIME` | **Resta** (`ADD_MULTIPLIED_BASE` negativo) |
| Paso | `STEP_HEIGHT` | **Cliente** |
| Nado | `WATER_MOVEMENT_EFFICIENCY` | **Cliente** |
| Sigilo | `SNEAKING_SPEED` | **Cliente** |
| Ligereza | `GRAVITY` | **Resta**, **cliente** |
| Minado submarino | `SUBMERGED_MINING_SPEED` | **Cliente** |

**Lo que resta lleva `signo` −1 en `Estadistica`**, y `aplicar` usa `valorDelModificador(efecto)`. El `porPunto` de la config sigue siendo positivo: la config no acepta negativos (ver [Configuración](configuracion.md)), y así "más puntos = mejor" vale para todas. Los de cliente están en `losAtributosQueUsaElClienteSeSincronizan`.

## Ver también

- [Tests de las ramas](../tests/ramas.md)
- [Estadísticas: puntos, compra y aplicación](estadisticas.md)
- [Menú de estadísticas](menu.md): la portada con las ramas y una página por rama
- [Pantalla propia del cliente](pantalla.md): una pestaña por rama
- [Minecraft 26.x sin ofuscar](../general/minecraft-26-sin-ofuscar.md): verificar las firmas antes de escribir un mixin
