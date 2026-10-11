package com.estadisticasrpg;

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

public enum Estadistica {
	VIDA("vida", "Vida", true, Items.GOLDEN_APPLE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_HEALTH), Estadistica::corazones),
	DANIO("danio", "Daño", true, Items.IRON_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_DAMAGE), valor -> conSigno(valor) + " de daño"),
	// Sin atributo: vanilla no tiene uno para los proyectiles, así que lo aplica ProyectilMixin al pegar
	DANIO_DISTANCIA("danio_distancia", "Daño a distancia", true, Items.BOW, valor -> conSigno(valor * 100) + " % de daño a distancia"),
	ARMADURA("armadura", "Armadura", true, Items.IRON_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR), valor -> conSigno(valor) + " de armadura"),
	VELOCIDAD_ATAQUE("velocidad_ataque", "Velocidad de ataque", true, Items.FEATHER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.ATTACK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de ataque"),
	// Sin atributo: la aplica Curacion cada segundo. El valor es vida por segundo, en medios corazones como MAX_HEALTH;
	// se muestra por minuto porque por segundo un punto daría "+0 corazones"
	REGENERACION("regeneracion", "Regeneración", true, Items.GHAST_TEAR, valor -> conSigno(valor * 30) + " corazones por minuto"),
	// Sin atributo: lo aplica Curacion después de cada golpe que da el jugador
	ROBO_DE_VIDA("robo_de_vida", "Robo de vida", true, Items.REDSTONE, valor -> conSigno(valor * 100) + " % del daño que hacés"),
	VELOCIDAD("velocidad", "Velocidad", false, Items.SUGAR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.MOVEMENT_SPEED), valor -> conSigno(valor * 100) + " % de velocidad"),
	MINADO("minado", "Minado", false, Items.IRON_PICKAXE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BLOCK_BREAK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de minado"),
	// Los dos rangos juntos: con sólo el de bloques, llegarías a un bloque pero no al mob parado al lado
	ALCANCE("alcance", "Alcance", false, Items.FISHING_ROD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de alcance"),
	EMPUJE("empuje", "Resistencia al empuje", false, Items.ANVIL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.KNOCKBACK_RESISTANCE), valor -> conSigno(valor * 100) + " % de resistencia al empuje"),
	SUERTE("suerte", "Suerte", false, Items.RABBIT_FOOT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.LUCK), valor -> conSigno(valor) + " de suerte"),
	SALTO("salto", "Salto", false, Items.SLIME_BALL, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.JUMP_STRENGTH), valor -> conSigno(valor * 100) + " % de fuerza de salto"),
	CAIDA("caida", "Caída segura", false, Items.HAY_BLOCK, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.SAFE_FALL_DISTANCE), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de caída sin daño"),
	RESPIRACION("respiracion", "Respiración", false, Items.PUFFERFISH, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.OXYGEN_BONUS), valor -> conSigno(valor) + " de aire bajo el agua");

	private final String id;
	private final String nombre;
	private final boolean combate;
	private final Item icono;
	private final AttributeModifier.@Nullable Operation operacion;
	private final List<Holder<Attribute>> atributos;
	private final DoubleFunction<String> descripcion;

	Estadistica(String id, String nombre, boolean combate, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, DoubleFunction<String> descripcion) {
		this.id = id;
		this.nombre = nombre;
		this.combate = combate;
		this.icono = icono;
		this.operacion = operacion;
		this.atributos = atributos;
		this.descripcion = descripcion;
	}

	Estadistica(String id, String nombre, boolean combate, Item icono, DoubleFunction<String> descripcion) {
		this(id, nombre, combate, icono, null, List.of(), descripcion);
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
			default -> nombre;
		};
	}

	/** Las de combate van en la fila de arriba del menú, las demás en la de abajo. */
	public boolean combate() {
		return combate;
	}

	/** El ítem que la representa en el menú. */
	public Item icono() {
		return icono;
	}

	/** {@code null} si no tiene atributos. */
	public AttributeModifier.@Nullable Operation operacion() {
		return operacion;
	}

	/** Vacía si el efecto no es un atributo (daño a distancia, regeneración, robo de vida): {@link Mejoras#aplicar} no le pone nada. */
	public List<Holder<Attribute>> atributos() {
		return atributos;
	}

	/** Un modificador por estadística con id fijo: aplicar dos veces lo reemplaza en vez de apilarlo. */
	public Identifier idModificador() {
		return EstadisticasRPG.id(id);
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

	static String numero(double valor) {
		double redondeado = Math.round(valor * 100) / 100.0;
		if (redondeado == Math.rint(redondeado)) {
			return Long.toString((long) redondeado);
		}
		return Double.toString(redondeado).replace('.', ',');
	}
}
