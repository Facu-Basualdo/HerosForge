package com.herosforge;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Las dos estadísticas que curan: regeneración y robo de vida. No tienen atributo en vanilla (la regeneración
 * natural depende del hambre, y no hay nada que cure al pegar), así que se aplican acá, del lado del servidor.
 */
public final class Curacion {
	private static final int TICKS_POR_SEGUNDO = 20;

	private Curacion() {
	}

	public static void registrar() {
		ServerTickEvents.END_SERVER_TICK.register(servidor -> {
			if (servidor.getTickCount() % TICKS_POR_SEGUNDO == 0) {
				regenerar(servidor);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Curacion::alPegar);
	}

	/**
	 * Una vez por segundo. Con los puntos del grupo cura uno solo: Soul Link suma la curación de cada
	 * jugador a la vida de todos, así que si curaran todos, el grupo se regeneraría una vez por jugador conectado.
	 */
	public static void regenerar(MinecraftServer servidor) {
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			if (!jugador.isAlive() || jugador.isSpectator() || jugador.isCreative()) {
				continue;
			}
			float cura = (float) Mejoras.efecto(jugador, Estadistica.REGENERACION);
			if (cura > 0 && jugador.getHealth() < jugador.getMaxHealth()) {
				jugador.heal(cura);
			}
			if (Mejoras.puntosDelGrupo()) {
				return;
			}
		}
	}

	/** Cuenta todo el daño que hace el jugador: cuerpo a cuerpo, flechas y tridentes ({@code getEntity} es el dueño del proyectil). */
	public static void alPegar(LivingEntity herido, DamageSource fuente, float danioBase, float danio, boolean bloqueado) {
		if (bloqueado || danio <= 0 || !(fuente.getEntity() instanceof ServerPlayer jugador) || jugador == herido || !jugador.isAlive()) {
			return;
		}
		double robo = Mejoras.efecto(jugador, Estadistica.ROBO_DE_VIDA);
		if (robo > 0) {
			jugador.heal((float) (danio * robo));
		}
	}
}
