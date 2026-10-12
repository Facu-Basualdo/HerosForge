package com.herosforge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Las estadísticas de pelea sin atributo en vanilla: crítico, ejecución, resistencias, represalia, escudo, esquiva
 * y destreza. Todo del lado del servidor; {@code DanioMixin} y {@code TensarMixin} llaman a lo de acá.
 */
public final class Combate {
	private static final int TICKS_POR_SEGUNDO = 20;
	/** El tick del servidor en que cada jugador recibió daño por última vez: el escudo se recarga sólo fuera de pelea. */
	private static final Map<UUID, Integer> ULTIMO_GOLPE = new ConcurrentHashMap<>();

	private Combate() {
	}

	public static void registrar() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(Combate::esquivar);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Combate::alRecibir);
		ServerTickEvents.END_SERVER_TICK.register(servidor -> {
			if (servidor.getTickCount() % TICKS_POR_SEGUNDO == 0) {
				recargarEscudos(servidor);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((conexion, servidor) -> ULTIMO_GOLPE.remove(conexion.getPlayer().getUUID()));
	}

	/**
	 * El daño que recibe {@code herido} antes de la armadura: crítico y ejecución si pega un jugador, resistencias
	 * si lo recibe un jugador. Lo llama {@code DanioMixin} al principio de {@code LivingEntity.hurtServer}.
	 */
	public static float danio(LivingEntity herido, DamageSource fuente, float danio) {
		if (danio <= 0) {
			return danio;
		}
		// Las espinas de la represalia también son "del jugador": sin esto, un crítico podría salir de una espina
		if (fuente.getEntity() instanceof ServerPlayer atacante && atacante != herido && !fuente.is(DamageTypes.THORNS)) {
			if (atacante.getRandom().nextDouble() < Mejoras.fraccion(atacante, Estadistica.CRITICO)) {
				danio *= (float) Configuracion.MULTIPLICADOR_CRITICO;
				// Las mismas partículas del crítico de vanilla, para que se note que salió
				atacante.crit(herido);
			}
			if (herido.getHealth() <= herido.getMaxHealth() * Configuracion.UMBRAL_EJECUCION) {
				danio *= (float) (1 + Mejoras.efecto(atacante, Estadistica.EJECUCION));
			}
		}
		if (herido instanceof ServerPlayer jugador && esElemental(fuente)) {
			danio *= (float) (1 - Mejoras.fraccion(jugador, Estadistica.RESISTENCIAS));
		}
		return danio;
	}

	/** Fuego (también lava y el piso de magma), explosiones y magia (pociones, wither, el aliento del dragón). */
	static boolean esElemental(DamageSource fuente) {
		return fuente.is(DamageTypeTags.IS_FIRE) || fuente.is(DamageTypeTags.IS_EXPLOSION)
				|| fuente.is(DamageTypes.MAGIC) || fuente.is(DamageTypes.INDIRECT_MAGIC) || fuente.is(DamageTypes.WITHER)
				|| fuente.is(DamageTypes.WITHER_SKULL) || fuente.is(DamageTypes.DRAGON_BREATH);
	}

	/**
	 * Sólo se esquivan los ataques de algo (un mob, una flecha, otro jugador): esquivar la lava, el vacío o el
	 * hambre no tendría sentido, y {@code BYPASSES_INVULNERABILITY} es {@code /kill}.
	 */
	private static boolean esquivar(LivingEntity herido, DamageSource fuente, float danio) {
		if (!(herido instanceof ServerPlayer jugador) || fuente.getEntity() == null || fuente.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		if (jugador.getRandom().nextDouble() >= Mejoras.fraccion(jugador, Estadistica.ESQUIVA)) {
			return true;
		}
		ServerLevel level = jugador.level();
		level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0f, 1.4f);
		level.sendParticles(ParticleTypes.CLOUD, jugador.getX(), jugador.getY() + 1, jugador.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
		return false;
	}

	private static void alRecibir(LivingEntity herido, DamageSource fuente, float danioBase, float danio, boolean bloqueado) {
		if (!(herido instanceof ServerPlayer jugador)) {
			return;
		}
		ULTIMO_GOLPE.put(jugador.getUUID(), jugador.level().getServer().getTickCount());
		represalia(jugador, fuente, danio, bloqueado);
	}

	/**
	 * Sólo cuerpo a cuerpo: el que pega es el mismo que lo causa (una flecha tiene a la flecha de directa y al
	 * arquero de causante). Las espinas no se devuelven: entre dos jugadores con represalia, se rebotarían para siempre.
	 */
	static void represalia(ServerPlayer jugador, DamageSource fuente, float danio, boolean bloqueado) {
		if (bloqueado || danio <= 0 || fuente.is(DamageTypes.THORNS)) {
			return;
		}
		Entity directo = fuente.getDirectEntity();
		if (!(directo instanceof LivingEntity atacante) || directo != fuente.getEntity() || atacante == jugador || !atacante.isAlive()) {
			return;
		}
		double represalia = Mejoras.efecto(jugador, Estadistica.REPRESALIA);
		if (represalia > 0) {
			atacante.hurtServer(jugador.level(), jugador.damageSources().thorns(jugador), (float) (danio * represalia));
		}
	}

	/**
	 * Una vez por segundo. El tope es el escudo comprado y no la absorción máxima: con una manzana dorada encima,
	 * vanilla sube el máximo y el escudo rellenaría también lo de la manzana.
	 */
	public static void recargarEscudos(MinecraftServer servidor) {
		int ahora = servidor.getTickCount();
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			if (!jugador.isAlive() || jugador.isSpectator()) {
				continue;
			}
			float escudo = (float) Mejoras.efecto(jugador, Estadistica.ESCUDO);
			float actual = jugador.getAbsorptionAmount();
			if (escudo <= 0 || actual >= escudo) {
				continue;
			}
			Integer golpe = ULTIMO_GOLPE.get(jugador.getUUID());
			if (golpe != null && ahora - golpe < Configuracion.SEGUNDOS_PARA_RECARGAR_ESCUDO * TICKS_POR_SEGUNDO) {
				continue;
			}
			jugador.setAbsorptionAmount(Math.min(escudo, actual + Configuracion.ESCUDO_POR_SEGUNDO));
		}
	}

	/** Para los tests: que el escudo se pueda recargar sin esperar los segundos fuera de pelea. */
	public static void olvidarGolpes() {
		ULTIMO_GOLPE.clear();
	}

	/**
	 * {@code tiempoRestante} es lo que vanilla le pasa a {@code releaseUsing}: la carga es la duración del uso menos
	 * eso. Se devuelve uno más chico, como si hubiera tensado más tiempo. El cliente sigue mostrando la animación de
	 * vanilla, pero la flecha (o la carga de la ballesta) la decide el servidor.
	 */
	public static int tensar(LivingEntity quien, Item arma, ItemStack stack, int tiempoRestante) {
		if (!(quien instanceof ServerPlayer jugador)) {
			return tiempoRestante;
		}
		double destreza = Mejoras.efecto(jugador, Estadistica.DESTREZA);
		if (destreza <= 0) {
			return tiempoRestante;
		}
		int duracion = arma.getUseDuration(stack, quien);
		int tensado = Math.max(0, duracion - tiempoRestante);
		// Pasarse de la duración no rompe nada (el arco y la ballesta cortan la carga en 1), pero sí desbordar el int
		long acelerado = Math.min(Math.round(tensado * (1 + destreza)), 2L * duracion);
		return (int) (duracion - acelerado);
	}
}
