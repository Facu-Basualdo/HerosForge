package com.herosforge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Metabolismo, nutrición, glotón y tenacidad: no hay atributos de vanilla para el hambre, la comida ni los efectos,
 * así que los aplican los mixins de {@code com.herosforge.mixin} llamando a lo de acá. Sólo para jugadores del
 * servidor: el hambre y los efectos los decide el servidor, y el cliente vanilla los recibe ya calculados.
 */
public final class Supervivencia {
	private Supervivencia() {
	}

	/** El cansancio que suma una acción (correr, saltar, pegar): con más cansancio baja la barra de hambre. */
	public static float cansancio(ServerPlayer jugador, float cansancio) {
		return (float) (cansancio * (1 - Mejoras.fraccion(jugador, Estadistica.METABOLISMO)));
	}

	/**
	 * Lo que se suma al comer, después de lo de vanilla. El alimento se redondea: un pan (5) con +50 % da 2 de más
	 * y no 2,5. La saturación ya viene multiplicada por el alimento en {@link FoodProperties}.
	 */
	public static void nutrir(ServerPlayer jugador, FoodProperties comida) {
		double nutricion = Mejoras.efecto(jugador, Estadistica.NUTRICION);
		if (nutricion <= 0) {
			return;
		}
		int alimento = (int) Math.round(comida.nutrition() * nutricion);
		float saturacion = (float) (comida.saturation() * nutricion);
		if (alimento > 0 || saturacion > 0) {
			jugador.getFoodData().eat(new FoodProperties(alimento, saturacion, false));
		}
	}

	/**
	 * Los ticks que dura comer o beber algo recién empezado. Nunca menos de 1: con 0, vanilla no termina nunca de
	 * usarlo (la cuenta baja a −1 y no vuelve a pasar por 0).
	 */
	public static int tiempoDeComer(ServerPlayer jugador, ItemStack usado, int ticks) {
		if (!usado.has(DataComponents.CONSUMABLE)) {
			return ticks;
		}
		double gloton = Mejoras.fraccion(jugador, Estadistica.GLOTON);
		if (gloton <= 0) {
			return ticks;
		}
		return Math.max(1, (int) Math.ceil(ticks * (1 - gloton)));
	}

	/** Un efecto negativo dura menos. Los infinitos (los de un faro o un comando) se dejan como están. */
	public static MobEffectInstance efecto(ServerPlayer jugador, MobEffectInstance efecto) {
		if (efecto.getEffect().value().getCategory() != MobEffectCategory.HARMFUL || efecto.isInfiniteDuration()) {
			return efecto;
		}
		double tenacidad = Mejoras.fraccion(jugador, Estadistica.TENACIDAD);
		if (tenacidad <= 0) {
			return efecto;
		}
		int duracion = Math.max(1, (int) Math.round(efecto.getDuration() * (1 - tenacidad)));
		return new MobEffectInstance(efecto.getEffect(), duracion, efecto.getAmplifier(), efecto.isAmbient(), efecto.isVisible(), efecto.showIcon());
	}
}
