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
	VIDA("vida", "Vida", Items.GOLDEN_APPLE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.MAX_HEALTH), Estadistica::corazones),
	DANIO("danio", "Daño", Items.IRON_SWORD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ATTACK_DAMAGE), valor -> conSigno(valor) + " de daño"),
	// Sin atributo: vanilla no tiene uno para los proyectiles, así que lo aplica ProyectilMixin al pegar
	DANIO_DISTANCIA("danio_distancia", "Daño a distancia", Items.BOW, valor -> conSigno(valor * 100) + " % de daño a distancia"),
	ARMADURA("armadura", "Armadura", Items.IRON_CHESTPLATE, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.ARMOR), valor -> conSigno(valor) + " de armadura"),
	VELOCIDAD_ATAQUE("velocidad_ataque", "Velocidad de ataque", Items.FEATHER, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.ATTACK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de ataque"),
	VELOCIDAD("velocidad", "Velocidad", Items.SUGAR, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.MOVEMENT_SPEED), valor -> conSigno(valor * 100) + " % de velocidad"),
	MINADO("minado", "Minado", Items.IRON_PICKAXE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE, List.of(Attributes.BLOCK_BREAK_SPEED), valor -> conSigno(valor * 100) + " % de velocidad de minado"),
	// Los dos rangos juntos: con sólo el de bloques, llegarías a un bloque pero no al mob parado al lado
	ALCANCE("alcance", "Alcance", Items.FISHING_ROD, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE), valor -> conSigno(valor) + (valor == 1 ? " bloque" : " bloques") + " de alcance"),
	EMPUJE("empuje", "Resistencia al empuje", Items.ANVIL, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.KNOCKBACK_RESISTANCE), valor -> conSigno(valor * 100) + " % de resistencia al empuje"),
	SUERTE("suerte", "Suerte", Items.RABBIT_FOOT, AttributeModifier.Operation.ADD_VALUE, List.of(Attributes.LUCK), valor -> conSigno(valor) + " de suerte");

	private final String id;
	private final String nombre;
	private final Item icono;
	private final AttributeModifier.@Nullable Operation operacion;
	private final List<Holder<Attribute>> atributos;
	private final DoubleFunction<String> descripcion;

	Estadistica(String id, String nombre, Item icono, AttributeModifier.@Nullable Operation operacion, List<Holder<Attribute>> atributos, DoubleFunction<String> descripcion) {
		this.id = id;
		this.nombre = nombre;
		this.icono = icono;
		this.operacion = operacion;
		this.atributos = atributos;
		this.descripcion = descripcion;
	}

	Estadistica(String id, String nombre, Item icono, DoubleFunction<String> descripcion) {
		this(id, nombre, icono, null, List.of(), descripcion);
	}

	public String id() {
		return id;
	}

	public String nombre() {
		return nombre;
	}

	/** El ítem que la representa en el menú. */
	public Item icono() {
		return icono;
	}

	/** {@code null} si no tiene atributos. */
	public AttributeModifier.@Nullable Operation operacion() {
		return operacion;
	}

	/** Vacía si el efecto no es un atributo (daño a distancia): {@link Mejoras#aplicar} no le pone nada. */
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
