package com.herosforge;

import java.util.List;
import java.util.Optional;

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
	DANIO("danio", Rama.GUERRERO, Items.IRON_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_DAMAGE), Efecto.mas(1)),
	VELOCIDAD_ATAQUE("velocidad_ataque", Rama.GUERRERO, Items.FEATHER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.ATTACK_SPEED), Efecto.mas(100)),
	// Sin atributo: vanilla no tiene uno para los proyectiles, así que lo aplica ProyectilMixin al pegar
	DANIO_DISTANCIA("danio_distancia", Rama.GUERRERO, Items.BOW, Efecto.mas(100)),
	// Sin atributo: TensarMixin acelera la carga al soltar el arco o la ballesta
	DESTREZA("destreza", Rama.GUERRERO, Items.CROSSBOW, Efecto.mas(100)),
	// Vanilla sólo barre con espadas
	BARRIDO("barrido", Rama.GUERRERO, Items.GOLDEN_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SWEEPING_DAMAGE_RATIO), Efecto.mas(100)),
	GOLPE("golpe", Rama.GUERRERO, Items.PISTON, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_KNOCKBACK), Efecto.mas(1)),
	// Sin atributo: los aplica DanioMixin (ver Combate)
	CRITICO("critico", Rama.GUERRERO, Items.BLAZE_POWDER, Efecto.sin(100).con((Configuracion.MULTIPLICADOR_CRITICO - 1) * 100)),
	EJECUCION("ejecucion", Rama.GUERRERO, Items.IRON_AXE, Efecto.mas(100).con(Configuracion.UMBRAL_EJECUCION * 100)),

	// Guardián
	VIDA("vida", Rama.GUARDIAN, Items.GOLDEN_APPLE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_HEALTH), Efecto.mas(0.5).plural()),
	ARMADURA("armadura", Rama.GUARDIAN, Items.IRON_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR), Efecto.mas(1)),
	EMPUJE("empuje", Rama.GUARDIAN, Items.ANVIL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.KNOCKBACK_RESISTANCE), Efecto.mas(100)),
	DUREZA("dureza", Rama.GUARDIAN, Items.DIAMOND_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR_TOUGHNESS), Efecto.mas(1)),
	ANTIEXPLOSION("antiexplosion", Rama.GUARDIAN, Items.TNT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE), Efecto.mas(100)),
	// Sin atributo: las tres que siguen las aplica Combate
	RESISTENCIAS("resistencias", Rama.GUARDIAN, Items.AMETHYST_SHARD, Efecto.menos(100)),
	REPRESALIA("represalia", Rama.GUARDIAN, Items.CACTUS, Efecto.sin(100)),
	// El atributo sólo deja tener absorción: la recarga la hace Combate fuera de pelea
	ESCUDO("escudo", Rama.GUARDIAN, Items.SHIELD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_ABSORPTION), Efecto.sin(0.5).plural()),
	ESQUIVA("esquiva", Rama.GUARDIAN, Items.ENDER_PEARL, Efecto.sin(100)),

	// Superviviente
	// Sin atributo: la aplica Curacion cada segundo. El valor es vida por segundo, en medios corazones como MAX_HEALTH;
	// se muestra por minuto porque por segundo un punto daría "+0 corazones"
	REGENERACION("regeneracion", Rama.SUPERVIVIENTE, Items.GHAST_TEAR, Efecto.mas(30)),
	// Sin atributo: lo aplica Curacion después de cada golpe que da el jugador
	ROBO_DE_VIDA("robo_de_vida", Rama.SUPERVIVIENTE, Items.REDSTONE, Efecto.mas(100)),
	// Sin atributo: las cuatro que siguen las aplican los mixins de comida y efectos (ver Supervivencia)
	METABOLISMO("metabolismo", Rama.SUPERVIVIENTE, Items.BREAD, Efecto.menos(100)),
	NUTRICION("nutricion", Rama.SUPERVIVIENTE, Items.COOKED_BEEF, Efecto.mas(100)),
	GLOTON("gloton", Rama.SUPERVIVIENTE, Items.CAKE, Efecto.menos(100)),
	TENACIDAD("tenacidad", Rama.SUPERVIVIENTE, Items.MILK_BUCKET, Efecto.menos(100)),
	// Resta: BURNING_TIME es cuánto dura el fuego, y 1 es lo de vanilla
	IGNIFUGO("ignifugo", Rama.SUPERVIVIENTE, Items.MAGMA_CREAM, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BURNING_TIME), -1, Efecto.menos(100)),
	RESPIRACION("respiracion", Rama.SUPERVIVIENTE, Items.PUFFERFISH, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.OXYGEN_BONUS), Efecto.mas(1)),

	// Explorador
	VELOCIDAD("velocidad", Rama.EXPLORADOR, Items.SUGAR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.MOVEMENT_SPEED), Efecto.mas(100)),
	SALTO("salto", Rama.EXPLORADOR, Items.SLIME_BALL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.JUMP_STRENGTH), Efecto.mas(100)),
	CAIDA("caida", Rama.EXPLORADOR, Items.HAY_BLOCK, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SAFE_FALL_DISTANCE), Efecto.mas(1).plural()),
	PASO("paso", Rama.EXPLORADOR, Items.LADDER, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.STEP_HEIGHT), Efecto.mas(1).plural()),
	// 1 es lo que da Agilidad acuática III: el agua deja de frenar
	NADO("nado", Rama.EXPLORADOR, Items.PRISMARINE_SHARD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.WATER_MOVEMENT_EFFICIENCY), Efecto.menos(100)),
	// Agachado se camina al 30 % de la velocidad normal: el valor se suma a ese 30 %
	SIGILO("sigilo", Rama.EXPLORADOR, Items.LEATHER_BOOTS, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SNEAKING_SPEED), Efecto.mas(100)),
	LIGEREZA("ligereza", Rama.EXPLORADOR, Items.PHANTOM_MEMBRANE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.GRAVITY), -1, Efecto.menos(100)),

	// Artesano
	MINADO("minado", Rama.ARTESANO, Items.IRON_PICKAXE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BLOCK_BREAK_SPEED), Efecto.mas(100)),
	// Bajo el agua se mina al 20 % de la velocidad normal: Afinidad acuática le suma 80 puntos
	MINADO_SUBMARINO("minado_submarino", Rama.ARTESANO, Items.NAUTILUS_SHELL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SUBMERGED_MINING_SPEED), Efecto.mas(100)),
	// Los dos rangos juntos: con sólo el de bloques, llegarías a un bloque pero no al mob parado al lado
	ALCANCE("alcance", Rama.ARTESANO, Items.FISHING_ROD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE), Efecto.mas(1).plural()),
	SUERTE("suerte", Rama.ARTESANO, Items.RABBIT_FOOT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.LUCK), Efecto.mas(1)),
	// Sin atributo: las cuatro que siguen las aplican Oficios y sus mixins
	IMAN("iman", Rama.ARTESANO, Items.LODESTONE, Efecto.sin(1).plural()),
	CUIDADO_DEL_EQUIPO("cuidado_del_equipo", Rama.ARTESANO, Items.GRINDSTONE, Efecto.sin(100)),
	REGATEO("regateo", Rama.ARTESANO, Items.EMERALD, Efecto.menos(100)),
	SABIDURIA("sabiduria", Rama.ARTESANO, Items.BOOK, Efecto.mas(100));

	private final String id;
	private final Rama rama;
	private final Item icono;
	private final AttributeModifier.@Nullable Operation operacion;
	private final List<Holder<Attribute>> atributos;
	private final double signo;
	private final Efecto efecto;

	Estadistica(String id, Rama rama, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, double signo, Efecto efecto) {
		this.id = id;
		this.rama = rama;
		this.icono = icono;
		this.operacion = operacion;
		this.atributos = atributos;
		this.signo = signo;
		this.efecto = efecto;
	}

	Estadistica(String id, Rama rama, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, Efecto efecto) {
		this(id, rama, icono, operacion, atributos, 1, efecto);
	}

	Estadistica(String id, Rama rama, Item icono, Efecto efecto) {
		this(id, rama, icono, null, List.of(), 1, efecto);
	}

	/**
	 * Cómo se lee el efecto: el número que se muestra es {@code valor × factor}, con su signo, y va en la plantilla
	 * {@code herosforge.estadistica.<id>.efecto} (con {@code .uno} o {@code .varios} si concuerda con el número, como
	 * "1 bloque" y "2 bloques"). {@code extras} son otros números fijos de la plantilla, como el daño del crítico.
	 */
	record Efecto(double factor, String signo, boolean concuerda, double... extras) {
		static Efecto mas(double factor) {
			return new Efecto(factor, "+", false);
		}

		/** Para lo que mejora al bajar (hambre, precios): "−30 %". */
		static Efecto menos(double factor) {
			return new Efecto(factor, "−", false);
		}

		static Efecto sin(double factor) {
			return new Efecto(factor, "", false);
		}

		Efecto plural() {
			return new Efecto(factor, signo, true, extras);
		}

		Efecto con(double... otros) {
			return new Efecto(factor, signo, concuerda, otros);
		}
	}

	public String id() {
		return id;
	}

	public String nombre(String idioma) {
		return Idioma.plantilla(idioma, "herosforge.estadistica." + id);
	}

	/** Para la pantalla con dos columnas, donde el nombre entero no entra: el {@code .corto} del archivo, si tiene. */
	public String nombreCorto(String idioma) {
		String clave = "herosforge.estadistica." + id + ".corto";
		return Idioma.claves(idioma).contains(clave) ? Idioma.plantilla(idioma, clave) : nombre(idioma);
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

	/** El efecto como lo lee el jugador ("+1,5 corazones", "+6 % de velocidad", "+1.5 hearts"). */
	public String describir(String idioma, double valor) {
		double mostrado = valor * efecto.factor();
		String clave = "herosforge.estadistica." + id + ".efecto" + (efecto.concuerda() ? (mostrado == 1 ? ".uno" : ".varios") : "");
		Object[] args = new Object[1 + efecto.extras().length];
		args[0] = efecto.signo() + Idioma.numero(idioma, mostrado);
		for (int i = 0; i < efecto.extras().length; i++) {
			args[i + 1] = Idioma.numero(idioma, efecto.extras()[i]);
		}
		return Idioma.tr(idioma, clave, args).getString();
	}

	public static Optional<Estadistica> porId(String id) {
		for (Estadistica estadistica : values()) {
			if (estadistica.id.equals(id)) {
				return Optional.of(estadistica);
			}
		}
		return Optional.empty();
	}
}
