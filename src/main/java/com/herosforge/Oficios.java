package com.herosforge;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Imán, cuidado del equipo, regateo y sabiduría: lo de la rama del artesano que no es un atributo. */
public final class Oficios {
	/** Lo que avanza por tick algo atraído: más rápido que caminar, para que no quede atrás si el jugador se mueve. */
	private static final double VELOCIDAD_DEL_IMAN = 0.35;

	private Oficios() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(Oficios::atraer);
	}

	/**
	 * Mueve los ítems y los orbes hacia el jugador; la recogida es la de vanilla. Agachado no atrae, para poder
	 * tirar cosas y dejarlas. Un ítem recién tirado tiene demora para levantarlo, y mientras tanto tampoco se atrae:
	 * si no, volvería solo a la mano.
	 */
	public static void atraer(MinecraftServer servidor) {
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			if (!jugador.isAlive() || jugador.isSpectator() || jugador.isShiftKeyDown()) {
				continue;
			}
			double radio = Mejoras.efecto(jugador, Estadistica.IMAN);
			if (radio <= 0) {
				continue;
			}
			AABB zona = jugador.getBoundingBox().inflate(radio);
			Vec3 destino = jugador.position().add(0, jugador.getBbHeight() / 2, 0);
			for (ItemEntity item : jugador.level().getEntitiesOfClass(ItemEntity.class, zona, item -> item.isAlive() && !item.hasPickUpDelay())) {
				acercar(item, destino);
			}
			for (ExperienceOrb orbe : jugador.level().getEntitiesOfClass(ExperienceOrb.class, zona, Entity::isAlive)) {
				acercar(orbe, destino);
			}
		}
	}

	private static void acercar(Entity entidad, Vec3 destino) {
		Vec3 hacia = destino.subtract(entidad.position());
		double distancia = hacia.length();
		if (distancia < 0.5) {
			return;
		}
		entidad.setDeltaMovement(hacia.scale(Math.min(VELOCIDAD_DEL_IMAN, distancia) / distancia));
		// Sin esto, el cliente sigue viendo el ítem quieto hasta la próxima actualización de movimiento
		entidad.needsSync = true;
	}

	/** Cada punto de durabilidad que se iba a gastar tiene su probabilidad de salvarse, como Irrompibilidad. */
	public static int durabilidad(ServerPlayer jugador, int gasto) {
		double cuidado = Mejoras.fraccion(jugador, Estadistica.CUIDADO_DEL_EQUIPO);
		if (cuidado <= 0 || gasto <= 0) {
			return gasto;
		}
		RandomSource azar = jugador.getRandom();
		int gastados = 0;
		for (int i = 0; i < gasto; i++) {
			if (azar.nextDouble() >= cuidado) {
				gastados++;
			}
		}
		return gastados;
	}

	/**
	 * Como Héroe de la aldea: una parte del precio, redondeada para abajo pero de al menos 1 esmeralda. Vanilla
	 * vuelve los precios a la normalidad ({@code resetSpecialPrices}) cada vez que se abre y se cierra el comercio.
	 */
	public static void regatear(ServerPlayer jugador, MerchantOffers ofertas) {
		double regateo = Mejoras.fraccion(jugador, Estadistica.REGATEO);
		if (regateo <= 0) {
			return;
		}
		for (MerchantOffer oferta : ofertas) {
			int descuento = (int) Math.floor(regateo * oferta.getBaseCostA().getCount());
			oferta.addToSpecialPriceDiff(-Math.max(descuento, 1));
		}
	}

	/**
	 * La experiencia de un orbe (lo que queda después de reparar con Reparación). Con orbes chicos, el extra que no
	 * llega a un punto entero sale al azar: un orbe de 1 con +25 % da 2 una vez de cada cuatro.
	 */
	public static int sabiduria(ServerPlayer jugador, int experiencia) {
		double sabiduria = Mejoras.efecto(jugador, Estadistica.SABIDURIA);
		if (sabiduria <= 0 || experiencia <= 0) {
			return experiencia;
		}
		double extra = experiencia * sabiduria;
		int entero = (int) Math.floor(extra);
		if (jugador.getRandom().nextDouble() < extra - entero) {
			entero++;
		}
		return (int) Math.min((long) experiencia + entero, Integer.MAX_VALUE);
	}
}
