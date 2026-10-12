package com.herosforge;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** En el orden en que se muestran: por rama, y dentro de cada rama de lo básico a lo más fuerte. */
public enum Estadistica {
	// Guerrero
	DANIO("danio", "Daño", Rama.GUERRERO, Items.IRON_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_DAMAGE), valor -> conSigno(valor) + " de daño"),
	VELOCIDAD_ATAQUE("velocidad_ataque", "Velocidad de ataque", Rama.GUERRERO, Items.FEATHER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.ATTACK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de ataque"),
	// Sin atributo: vanilla no tiene uno para los proyectiles, así que lo aplica ProyectilMixin al pegar
	DANIO_DISTANCIA("danio_distancia", "Daño a distancia", Rama.GUERRERO, Items.BOW, valor -> conSigno(valor * 100) + " % de daño a distancia"),
	// Sin atributo: TensarMixin acelera la carga al soltar el arco o la ballesta
	DESTREZA("destreza", "Destreza", Rama.GUERRERO, Items.CROSSBOW, valor -> conSigno(valor * 100) + " % de velocidad al tensar arcos y ballestas"),
	// Vanilla sólo barre con espadas
	BARRIDO("barrido", "Barrido", Rama.GUERRERO, Items.GOLDEN_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SWEEPING_DAMAGE_RATIO), valor -> conSigno(valor * 100) + " % del daño en el barrido de la espada"),
	GOLPE("golpe", "Golpe contundente", Rama.GUERRERO, Items.PISTON, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_KNOCKBACK), valor -> conSigno(valor) + " de empuje al pegar"),
	// Sin atributo: los aplica DanioMixin (ver Combate)
	CRITICO("critico", "Crítico", Rama.GUERRERO, Items.BLAZE_POWDER, valor -> numero(valor * 100) + " % de probabilidad de crítico (+" + numero((Configuracion.MULTIPLICADOR_CRITICO - 1) * 100) + " % de daño)"),
	EJECUCION("ejecucion", "Ejecución", Rama.GUERRERO, Items.IRON_AXE, valor -> conSigno(valor * 100) + " % de daño a enemigos con menos del " + numero(Configuracion.UMBRAL_EJECUCION * 100) + " % de vida"),

	// Guardián
	VIDA("vida", "Vida", Rama.GUARDIAN, Items.GOLDEN_APPLE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_HEALTH), Estadistica::corazones),
	ARMADURA("armadura", "Armadura", Rama.GUARDIAN, Items.IRON_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR), valor -> conSigno(valor) + " de armadura"),
	EMPUJE("empuje", "Resistencia al empuje", Rama.GUARDIAN, Items.ANVIL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.KNOCKBACK_RESISTANCE), valor -> conSigno(valor * 100) + " % de resistencia al empuje"),
	DUREZA("dureza", "Dureza", Rama.GUARDIAN, Items.DIAMOND_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR_TOUGHNESS), valor -> conSigno(valor) + " de dureza de armadura"),
	ANTIEXPLOSION("antiexplosion", "Antiexplosión", Rama.GUARDIAN, Items.TNT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), valor -> conSigno(valor * 100) + " % de resistencia al empuje de las explosiones"),
	// Sin atributo: las tres que siguen las aplica Combate
	RESISTENCIAS("resistencias", "Resistencias", Rama.GUARDIAN, Items.AMETHYST_SHARD, valor -> conMenos(valor * 100) + " % de daño de fuego, explosiones y magia"),
	REPRESALIA("represalia", "Represalia", Rama.GUARDIAN, Items.CACTUS, valor -> "Devolvés el " + numero(valor * 100) + " % del daño cuerpo a cuerpo"),
	// El atributo sólo deja tener absorción: la recarga la hace Combate fuera de pelea
	ESCUDO("escudo", "Escudo", Rama.GUARDIAN, Items.SHIELD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_ABSORPTION), valor -> "Escudo de " + numero(valor / 2) + (valor == 2 ? " corazón" : " corazones") + " que se recarga fuera de pelea"),
	ESQUIVA("esquiva", "Esquiva", Rama.GUARDIAN, Items.ENDER_PEARL, valor -> numero(valor * 100) + " % de probabilidad de esquivar un ataque"),

	// Superviviente
	// Sin atributo: la aplica Curacion cada segundo. El valor es vida por segundo, en medios corazones como MAX_HEALTH;
	// se muestra por minuto porque por segundo un punto daría "+0 corazones"
	REGENERACION("regeneracion", "Regeneración", Rama.SUPERVIVIENTE, Items.GHAST_TEAR, valor -> conSigno(valor * 30) + " corazones por minuto"),
	// Sin atributo: lo aplica Curacion después de cada golpe que da el jugador
	ROBO_DE_VIDA("robo_de_vida", "Robo de vida", Rama.SUPERVIVIENTE, Items.REDSTONE, valor -> conSigno(valor * 100) + " % del daño que hacés"),
	// Sin atributo: las cuatro que siguen las aplican los mixins de comida y efectos (ver Supervivencia)
	METABOLISMO("metabolismo", "Metabolismo", Rama.SUPERVIVIENTE, Items.BREAD, valor -> conMenos(valor * 100) + " % de hambre"),
	NUTRICION("nutricion", "Nutrición", Rama.SUPERVIVIENTE, Items.COOKED_BEEF, valor -> conSigno(valor * 100) + " % de lo que llena la comida"),
	GLOTON("gloton", "Glotón", Rama.SUPERVIVIENTE, Items.CAKE, valor -> conMenos(valor * 100) + " % del tiempo para comer y beber"),
	TENACIDAD("tenacidad", "Tenacidad", Rama.SUPERVIVIENTE, Items.MILK_BUCKET, valor -> conMenos(valor * 100) + " % de duración de los efectos negativos"),
	// Resta: BURNING_TIME es cuánto dura el fuego, y 1 es lo de vanilla
	IGNIFUGO("ignifugo", "Ignífugo", Rama.SUPERVIVIENTE, Items.MAGMA_CREAM, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BURNING_TIME), -1, valor -> conMenos(valor * 100) + " % de tiempo prendido fuego"),
	RESPIRACION("respiracion", "Respiración", Rama.SUPERVIVIENTE, Items.PUFFERFISH, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.OXYGEN_BONUS), valor -> conSigno(valor) + " de aire bajo el agua"),

	// Explorador
	VELOCIDAD("velocidad", "Velocidad", Rama.EXPLORADOR, Items.SUGAR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.MOVEMENT_SPEED), valor -> conSigno(valor * 100) + " % de velocidad"),
	SALTO("salto", "Salto", Rama.EXPLORADOR, Items.SLIME_BALL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.JUMP_STRENGTH), valor -> conSigno(valor * 100) + " % de fuerza de salto"),
	CAIDA("caida", "Caída segura", Rama.EXPLORADOR, Items.HAY_BLOCK, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SAFE_FALL_DISTANCE), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de caída sin daño"),
	PASO("paso", "Paso", Rama.EXPLORADOR, Items.LADDER, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.STEP_HEIGHT), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de escalón sin saltar"),
	// 1 es lo que da Agilidad acuática III: el agua deja de frenar
	NADO("nado", "Nado", Rama.EXPLORADOR, Items.PRISMARINE_SHARD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.WATER_MOVEMENT_EFFICIENCY), valor -> conMenos(valor * 100) + " % de lo que frena el agua"),
	// Agachado se camina al 30 % de la velocidad normal: el valor se suma a ese 30 %
	SIGILO("sigilo", "Sigilo", Rama.EXPLORADOR, Items.LEATHER_BOOTS, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SNEAKING_SPEED), valor -> conSigno(valor * 100) + " % de la velocidad normal al ir agachado"),
	LIGEREZA("ligereza", "Ligereza", Rama.EXPLORADOR, Items.PHANTOM_MEMBRANE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.GRAVITY), -1, valor -> conMenos(valor * 100) + " % de gravedad"),

	// Artesano
	MINADO("minado", "Minado", Rama.ARTESANO, Items.IRON_PICKAXE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BLOCK_BREAK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de minado"),
	// Bajo el agua se mina al 20 % de la velocidad normal: Afinidad acuática le suma 80 puntos
	MINADO_SUBMARINO("minado_submarino", "Minado submarino", Rama.ARTESANO, Items.NAUTILUS_SHELL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SUBMERGED_MINING_SPEED), valor -> conSigno(valor * 100) + " % de la velocidad normal al minar bajo el agua"),
	// Los dos rangos juntos: con sólo el de bloques, llegarías a un bloque pero no al mob parado al lado
	ALCANCE("alcance", "Alcance", Rama.ARTESANO, Items.FISHING_ROD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de alcance"),
	SUERTE("suerte", "Suerte", Rama.ARTESANO, Items.RABBIT_FOOT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.LUCK), valor -> conSigno(valor) + " de suerte"),
	// Sin atributo: las cuatro que siguen las aplican Oficios y sus mixins
	IMAN("iman", "Imán", Rama.ARTESANO, Items.LODESTONE, valor -> "Atraés ítems y experiencia a " + numero(valor) + (valor == 1 ? " bloque" : " bloques") + " (agachado no)"),
	CUIDADO_DEL_EQUIPO("cuidado_del_equipo", "Cuidado del equipo", Rama.ARTESANO, Items.GRINDSTONE, valor -> numero(valor * 100) + " % de probabilidad de no gastar durabilidad"),
	REGATEO("regateo", "Regateo", Rama.ARTESANO, Items.EMERALD, valor -> conMenos(valor * 100) + " % en los precios de los aldeanos"),
	SABIDURIA("sabiduria", "Sabiduría", Rama.ARTESANO, Items.BOOK, valor -> conSigno(valor * 100) + " % de experiencia de los orbes");

	private final String id;
	private final String nombre;
	private final Rama rama;
	private final Item icono;
	private final AttributeModifier.@Nullable Operation operacion;
	private final List<Holder<Attribute>> atributos;
	private final double signo;
	private final DoubleFunction<String> descripcion;

	Estadistica(String id, String nombre, Rama rama, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, double signo, DoubleFunction<String> descripcion) {
		this.id = id;
		this.nombre = nombre;
		this.rama = rama;
		this.icono = icono;
		this.operacion = operacion;
		this.atributos = atributos;
		this.signo = signo;
		this.descripcion = descripcion;
	}

	Estadistica(String id, String nombre, Rama rama, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, DoubleFunction<String> descripcion) {
		this(id, nombre, rama, icono, operacion, atributos, 1, descripcion);
	}

	Estadistica(String id, String nombre, Rama rama, Item icono, DoubleFunction<String> descripcion) {
		this(id, nombre, rama, icono, null, List.of(), 1, descripcion);
	}

	public String id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}

	/** Para la pantalla con dos columnas, donde el nombre entero no entra. */
	public String nombreCorto() {
		return switch (this) {
			case DANIO_DISTANCIA -> "Daño a dist.";
			case VELOCIDAD_ATAQUE -> "Vel. de ataque";
			case EMPUJE -> "Res. al empuje";
			case GOLPE -> "Golpe cont.";
			case MINADO_SUBMARINO -> "Minado subm.";
			case CUIDADO_DEL_EQUIPO -> "Cuidado equipo";
			default -> nombre;
		};
	}

	public Rama rama() {
		return rama;
	}

	/** El ítem que la representa en el menú. */
	public Item icono() {
		return icono;
	}

	/** {@code null} si no tiene atributos. */
	public AttributeModifier.@Nullable Operation operacion() {
		return operacion;
	}

	/** Vacía si el efecto no es un atributo (daño a distancia, regeneración, crítico…): {@link Mejoras#aplicar} no le pone nada. */
	public List<Holder<Attribute>> atributos() {
		return atributos;
	}

	/**
	 * El valor del modificador para un efecto. El efecto es siempre positivo (la config no acepta un {@code porPunto}
	 * negativo), pero la gravedad y el tiempo prendido fuego mejoran al bajar: ésas restan.
	 */
	public double valorDelModificador(double efecto) {
		return efecto * signo;
	}

	/** Un modificador por estadística con id fijo: aplicar dos veces lo reemplaza en vez de apilarlo. */
	public Identifier idModificador() {
		return HerosForge.id(id);
	}

	/** El efecto en castellano, como lo lee el jugador ("+1,5 corazones", "+6 % de velocidad"). */
	public String describir(double valor) {
		return descripcion.apply(valor);
	}

	public static Optional<Estadistica> porId(String id) {
		for (Estadistica estadistica : values()) {
			if (estadistica.id.equals(id)) {
				return Optional.of(estadistica);
			}
		}
		return Optional.empty();
	}

	private static String corazones(double vida) {
		double corazones = vida / 2;
		return conSigno(corazones) + (corazones == 1 ? " corazón" : " corazones");
	}

	/** Con coma decimal y sin ceros de más: 2.5 → "+2,5", 3.0 → "+3". */
	static String conSigno(double valor) {
		return "+" + numero(valor);
	}

	/** Para lo que mejora al bajar (hambre, precios): 30 → "−30". */
	static String conMenos(double valor) {
		return "−" + numero(valor);
	}

	static String numero(double valor) {
		double redondeado = Math.round(valor * 100) / 100.0;
		if (redondeado == Math.rint(redondeado)) {
			return Long.toString((long) redondeado);
		}
		return Double.toString(redondeado).replace('.', ',');
	}
}
