package com.estadisticasrpg;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
		/** Los niveles no alcanzaron para todo (o para nada). */
		SIN_NIVELES
	}

	/**
	 * {@code resultado} dice qué cortó la compra cuando {@code comprados} es menos de lo pedido.
	 * {@code costoDelProximo} es el del punto siguiente, ya con lo comprado.
	 */
	public record Compra(Estadistica estadistica, int pedidos, int comprados, Resultado resultado, int costoDelProximo) {
	}

	// Lo decide EstadisticasRPG al iniciar (si está Inventario Compartido); los tests lo fuerzan
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
		return jugador.getAttachedOrCreate(EstadisticasRPG.PUNTOS);
	}

	/** Lo que cuesta el próximo punto, según cuántos hay ya comprados en total. */
	public static int costoDelProximo(ServerPlayer jugador) {
		return EstadisticasRPG.config().costo(puntos(jugador).total());
	}

	/**
	 * Compra lo que entre según el tope y los niveles: lo que se cobra es exactamente lo que se sube. Cada punto se
	 * cobra con su propio costo, así que comprar 5 de una cuesta lo mismo que de a uno.
	 */
	public static Compra comprar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		Configuracion config = EstadisticasRPG.config();
		Configuracion.Mejora mejora = config.de(estadistica);
		PuntosDelJugador puntos = puntos(jugador);

		int margen = Math.max(0, mejora.tope() - puntos.de(estadistica));
		int comprados = 0;
		int costo = 0;
		while (comprados < Math.min(cantidad, margen)) {
			int siguiente = config.costo(puntos.total() + comprados);
			if (costo + siguiente > jugador.experienceLevel) {
				break;
			}
			costo += siguiente;
			comprados++;
		}
		Resultado resultado = comprados == cantidad ? Resultado.OK : comprados == margen ? Resultado.TOPE : Resultado.SIN_NIVELES;
		if (comprados == 0) {
			efectos(jugador, false);
			return new Compra(estadistica, cantidad, 0, resultado, config.costo(puntos.total()));
		}

		// El override de ServerPlayer pone lastSentExp = -1: sin eso la barra del cliente no baja, porque
		// vanilla sólo la reenvía cuando cambia totalExperience y bajar niveles no lo toca
		jugador.giveExperienceLevels(-costo);
		sumar(jugador, estadistica, comprados);
		efectos(jugador, true);
		return new Compra(estadistica, cantidad, comprados, resultado, config.costo(puntos.total() + comprados));
	}

	/** Da puntos sin cobrar, hasta el tope. Devuelve cuántos dio. */
	public static int dar(ServerPlayer jugador, Estadistica estadistica, int cantidad) {
		Configuracion.Mejora mejora = EstadisticasRPG.config().de(estadistica);
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
	}

	/**
	 * Al entrar. Con los puntos compartidos, la primera vez de cada jugador se suman al grupo los que había
	 * comprado por su cuenta: si eso sube algo, les cambia a todos.
	 */
	public static void alEntrar(ServerPlayer jugador) {
		if (delGrupo) {
			MinecraftServer servidor = jugador.level().getServer();
			if (PuntosDelGrupo.de(servidor).sumarIndividuales(jugador.getUUID(), jugador.getAttachedOrCreate(EstadisticasRPG.PUNTOS))) {
				aplicarATodos(servidor);
				return;
			}
		}
		aplicar(jugador);
	}

	/** Pone o quita los modificadores según los puntos. Es idempotente: no apila. */
	public static void aplicar(ServerPlayer jugador) {
		Configuracion config = EstadisticasRPG.config();
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
			jugador.setHealth(jugador.getMaxHealth());
		}
	}

	/**
	 * Al morir, {@code restoreFrom} copia sólo los valores base: los modificadores se pierden y la vida queda
	 * en el máximo sin los puntos. Se leen los puntos del jugador viejo para no depender del orden en que
	 * Fabric copia el attachment respecto de este evento.
	 */
	public static void alReaparecer(ServerPlayer viejo, ServerPlayer nuevo, boolean vivo) {
		if (!delGrupo) {
			nuevo.setAttached(EstadisticasRPG.PUNTOS, puntos(viejo));
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
					: "Te faltan niveles: el próximo punto cuesta " + niveles(compra.costoDelProximo()));
		}
		Configuracion.Mejora mejora = EstadisticasRPG.config().de(estadistica);
		String texto = estadistica.nombre() + " " + estadistica.describir(mejora.porPunto() * compra.comprados())
				+ " (" + compra.comprados() + (compra.comprados() == 1 ? " punto" : " puntos") + ")";
		if (compra.resultado() == Resultado.TOPE) {
			texto += ". Llegaste al máximo";
		} else if (compra.resultado() == Resultado.SIN_NIVELES) {
			texto += ". No te alcanzó para los " + compra.pedidos();
		}
		return Component.literal(texto);
	}

	static String niveles(int cantidad) {
		return cantidad + (cantidad == 1 ? " nivel" : " niveles");
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
			jugador.heal((float) (EstadisticasRPG.config().de(estadistica).porPunto() * cantidad));
		}
	}

	/**
	 * Con los puntos compartidos, a todos los conectados: los que entren después los toman en {@link #alEntrar}.
	 * Sólo se cura al que compró: Inventario Compartido reparte esa curación al resto, como cualquier otra.
	 */
	private static void guardar(ServerPlayer jugador, PuntosDelJugador puntos) {
		if (delGrupo) {
			MinecraftServer servidor = jugador.level().getServer();
			PuntosDelGrupo.de(servidor).cambiar(puntos);
			aplicarATodos(servidor);
		} else {
			jugador.setAttached(EstadisticasRPG.PUNTOS, puntos);
			aplicar(jugador);
		}
	}
}
