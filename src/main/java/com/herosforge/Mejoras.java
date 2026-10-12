package com.herosforge;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class Mejoras {
	private Mejoras() {
	}

	public enum Resultado {
		/** Se compró todo lo pedido. */
		OK,
		/** El tope no dejó comprar todo (o nada, si {@code comprados == 0}). */
		TOPE,
		/** La experiencia no alcanzó para todo (o para nada). */
		SIN_EXPERIENCIA
	}

	/** {@code resultado} dice qué cortó la compra cuando {@code comprados} es menos de lo pedido. */
	public record Compra(Estadistica estadistica, int pedidos, int comprados, Resultado resultado) {
	}

	/** {@code bajados} puede ser menos que lo pedido si no había tantos puntos; {@code experiencia} es lo que se devolvió. */
	public record Devolucion(Estadistica estadistica, int pedidos, int bajados, int experiencia) {
	}

	// Lo decide HerosForge al iniciar (si está Soul Link); los tests lo fuerzan
	private static volatile boolean delGrupo;

	public static void usarPuntosDelGrupo(boolean usar) {
		delGrupo = usar;
	}

	public static boolean puntosDelGrupo() {
		return delGrupo;
	}

	/** Los del jugador, o los del grupo si están compartidos (ver {@link PuntosDelGrupo}). */
	public static PuntosDelJugador puntos(ServerPlayer jugador) {
		if (delGrupo) {
			return PuntosDelGrupo.de(jugador.level().getServer()).puntos();
		}
		return jugador.getAttachedOrCreate(HerosForge.PUNTOS);
	}

	/**
	 * Los puntos de experiencia que tiene el jugador, contando los niveles enteros y la barra. No es
	 * {@code totalExperience}: ése es el puntaje de la pantalla de muerte, y encantar no lo baja.
	 */
	public static int experiencia(ServerPlayer jugador) {
		long total = puntosHastaNivel(jugador.experienceLevel) + Math.round(jugador.experienceProgress * jugador.getXpNeededForNextLevel());
		// Con /xp se llega a niveles cuya experiencia no entra en un int
		return (int) Math.min(total, Integer.MAX_VALUE);
	}

	/** La experiencia para llegar a un nivel desde 0: la suma de {@code Player.getXpNeededForNextLevel} de los anteriores. */
	static long puntosHastaNivel(long nivel) {
		if (nivel <= 16) {
			return nivel * nivel + 6 * nivel;
		}
		// Las dos fórmulas dan siempre un entero: nivel × (5 × nivel − 81) y nivel × (9 × nivel − 325) son pares
		if (nivel <= 31) {
			return nivel * (5 * nivel - 81) / 2 + 360;
		}
		return nivel * (9 * nivel - 325) / 2 + 2220;
	}

	/**
	 * Por cuánto se multiplica el daño de una flecha o un tridente lanzado (lo usa {@code ProyectilMixin}). Sin
	 * dueño, o si no es un jugador (esqueletos, dispensadores, un jugador desconectado), el de vanilla.
	 */
	public static double multiplicadorADistancia(@Nullable Entity duenio) {
		if (!(duenio instanceof ServerPlayer jugador)) {
			return 1;
		}
		return 1 + HerosForge.config().de(Estadistica.DANIO_DISTANCIA).efecto(puntos(jugador).de(Estadistica.DANIO_DISTANCIA));
	}

	/** Compra lo que entre según el tope y la experiencia: lo que se cobra es exactamente lo que se sube. */
	public static Compra comprar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		CompatibilidadSoulLink.sincronizarExperiencia(jugador.level().getServer());
		Configuracion config = HerosForge.config();
		Configuracion.Mejora mejora = config.de(estadistica);
		PuntosDelJugador puntos = puntos(jugador);

		int margen = Math.max(0, mejora.tope() - puntos.de(estadistica));
		int alcanzan = config.experienciaPorPunto() <= 0 ? cantidad : experiencia(jugador) / config.experienciaPorPunto();
		int comprados = Math.min(cantidad, Math.min(margen, alcanzan));
		Resultado resultado = comprados == cantidad ? Resultado.OK : margen <= alcanzan ? Resultado.TOPE : Resultado.SIN_EXPERIENCIA;
		if (comprados == 0) {
			efectos(jugador, false);
			return new Compra(estadistica, cantidad, 0, resultado);
		}

		// Con puntos negativos vanilla baja de nivel cuando la barra pasa de 0. El override de ServerPlayer pone
		// lastSentExp = -1: sin eso la barra del cliente no se actualizaría
		jugador.giveExperiencePoints(-config.experienciaPorPunto() * comprados);
		CompatibilidadSoulLink.sincronizarExperiencia(jugador.level().getServer());
		sumar(jugador, estadistica, comprados);
		efectos(jugador, true);
		return new Compra(estadistica, cantidad, comprados, resultado);
	}

	/**
	 * Baja hasta {@code cantidad} puntos y devuelve {@link Configuracion#devolucionPorPunto} de experiencia por cada
	 * uno. Con los puntos del grupo, la experiencia la recibe el que bajó: Soul Link la reparte.
	 */
	public static Devolucion bajar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		CompatibilidadSoulLink.sincronizarExperiencia(jugador.level().getServer());
		int actuales = puntos(jugador).de(estadistica);
		int bajados = Math.min(Math.max(cantidad, 0), actuales);
		if (bajados == 0) {
			efectos(jugador, false);
			return new Devolucion(estadistica, cantidad, 0, 0);
		}
		// En long: con un tope enorme en la config y una devolución alta, la cuenta no entra en un int
		int experiencia = (int) Math.min((long) HerosForge.config().devolucionPorPunto() * bajados, Integer.MAX_VALUE);
		guardar(jugador, puntos(jugador).con(estadistica, actuales - bajados));
		if (experiencia > 0) {
			jugador.giveExperiencePoints(experiencia);
			CompatibilidadSoulLink.sincronizarExperiencia(jugador.level().getServer());
		}
		ServerLevel level = jugador.level();
		level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.8f);
		return new Devolucion(estadistica, cantidad, bajados, experiencia);
	}

	/** Da puntos sin cobrar, hasta el tope. Devuelve cuántos dio. */
	public static int dar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		Configuracion.Mejora mejora = HerosForge.config().de(estadistica);
		int dados = Math.min(cantidad, Math.max(0, mejora.tope() - puntos(jugador).de(estadistica)));
		if (dados > 0) {
			sumar(jugador, estadistica, dados);
		}
		return dados;
	}

	/** Todo en 0, sin devolver niveles. Con los puntos compartidos, los de todo el grupo. */
	public static void reiniciar(ServerPlayer jugador) {
		guardar(jugador, PuntosDelJugador.vacio());
	}

	public static void aplicarATodos(MinecraftServer servidor) {
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			aplicar(jugador);
		}
		CompatibilidadSoulLink.sincronizarVida(servidor);
	}

	/**
	 * Al entrar. Con los puntos compartidos, la primera vez de cada jugador se suman al grupo los que había
	 * comprado por su cuenta: si eso sube algo, les cambia a todos.
	 */
	public static void alEntrar(ServerPlayer jugador) {
		if (delGrupo) {
			MinecraftServer servidor = jugador.level().getServer();
			if (PuntosDelGrupo.de(servidor).sumarIndividuales(jugador.getUUID(), jugador.getAttachedOrCreate(HerosForge.PUNTOS))) {
				aplicarATodos(servidor);
				return;
			}
		}
		aplicar(jugador);
	}

	/** Pone o quita los modificadores según los puntos. Es idempotente: no apila. */
	public static void aplicar(ServerPlayer jugador) {
		Configuracion config = HerosForge.config();
		PuntosDelJugador puntos = puntos(jugador);
		for (Estadistica estadistica : Estadistica.values()) {
			double efecto = config.de(estadistica).efecto(puntos.de(estadistica));
			for (Holder<Attribute> atributo : estadistica.atributos()) {
				AttributeInstance instancia = jugador.getAttribute(atributo);
				if (instancia == null) {
					continue;
				}
				if (efecto == 0) {
					instancia.removeModifier(estadistica.idModificador());
				} else {
					instancia.addOrReplacePermanentModifier(new AttributeModifier(estadistica.idModificador(), efecto, estadistica.operacion()));
				}
			}
		}
		// Vanilla recién corta la vida contra el máximo nuevo en el próximo tick (refreshDirtyAttributes): sin esto,
		// después de reiniciar el jugador queda con más vida que el máximo hasta que tickea
		if (jugador.getHealth() > jugador.getMaxHealth()) {
			CompatibilidadSoulLink.recortarVida(jugador);
		}
	}

	/**
	 * Al morir, {@code restoreFrom} copia sólo los valores base: los modificadores se pierden y la vida queda
	 * en el máximo sin los puntos. Se leen los puntos del jugador viejo para no depender del orden en que
	 * Fabric copia el attachment respecto de este evento.
	 */
	public static void alReaparecer(ServerPlayer viejo, ServerPlayer nuevo, boolean vivo) {
		if (!delGrupo) {
			nuevo.setAttached(HerosForge.PUNTOS, puntos(viejo));
		}
		aplicar(nuevo);
		if (!vivo) {
			nuevo.setHealth(nuevo.getMaxHealth());
		}
	}

	public static Component mensaje(Compra compra) {
		Estadistica estadistica = compra.estadistica();
		if (compra.comprados() == 0) {
			return Component.literal(compra.resultado() == Resultado.TOPE
					? estadistica.nombre() + " ya está en el máximo"
					: "Te falta experiencia: cada punto cuesta " + experiencia(HerosForge.config().experienciaPorPunto()));
		}
		Configuracion.Mejora mejora = HerosForge.config().de(estadistica);
		String texto = estadistica.nombre() + " " + estadistica.describir(mejora.porPunto() * compra.comprados())
				+ " (" + compra.comprados() + (compra.comprados() == 1 ? " punto" : " puntos") + ")";
		if (compra.resultado() == Resultado.TOPE) {
			texto += ". Llegaste al máximo";
		} else if (compra.resultado() == Resultado.SIN_EXPERIENCIA) {
			texto += ". No te alcanzó para los " + compra.pedidos();
		}
		return Component.literal(texto);
	}

	public static Component mensaje(Devolucion devolucion) {
		Estadistica estadistica = devolucion.estadistica();
		if (devolucion.bajados() == 0) {
			return Component.literal(estadistica.nombre() + " no tiene puntos para bajar");
		}
		String texto = estadistica.nombre() + " −" + devolucion.bajados() + (devolucion.bajados() == 1 ? " punto" : " puntos")
				+ ": te devolvió " + experiencia(devolucion.experiencia());
		if (devolucion.bajados() < devolucion.pedidos()) {
			texto += ". No tenía los " + devolucion.pedidos();
		}
		return Component.literal(texto);
	}

	static String experiencia(int cantidad) {
		return cantidad + " de experiencia";
	}

	/** Con sonido para todos los que están cerca: comprar es un momento, como subir de nivel. */
	private static void efectos(ServerPlayer jugador, boolean compro) {
		ServerLevel level = jugador.level();
		if (compro) {
			level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, jugador.getX(), jugador.getY() + 1, jugador.getZ(), 12, 0.4, 0.6, 0.4, 0);
		} else {
			// Desde el menú, el mensaje de la barra de acción queda opacado detrás del cofre: el sonido avisa que no se compró
			level.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
		}
	}

	private static void sumar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		guardar(jugador, puntos(jugador).con(estadistica, puntos(jugador).de(estadistica) + cantidad));
		if (estadistica == Estadistica.VIDA) {
			// Subir MAX_HEALTH no sube la vida actual: sin esto el corazón nuevo aparece vacío y parece que no anduvo
			jugador.heal((float) (HerosForge.config().de(estadistica).porPunto() * cantidad));
		}
	}

	/**
	 * Con los puntos compartidos, a todos los conectados: los que entren después los toman en {@link #alEntrar}.
	 * Sólo se cura al que compró: Soul Link reparte esa curación al resto, como cualquier otra.
	 */
	private static void guardar(ServerPlayer jugador, PuntosDelJugador puntos) {
		if (delGrupo) {
			MinecraftServer servidor = jugador.level().getServer();
			PuntosDelGrupo.de(servidor).cambiar(puntos);
			aplicarATodos(servidor);
		} else {
			jugador.setAttached(HerosForge.PUNTOS, puntos);
			aplicar(jugador);
		}
	}
}
