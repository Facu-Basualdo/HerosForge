package com.estadisticasrpg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * El lado del servidor de la pantalla propia del cliente. Sólo les habla a los clientes que registraron los
 * canales de {@link Paquetes} ({@code canSend}): con un cliente vanilla no se manda nada y {@code /estadisticas}
 * abre el cofre como siempre.
 */
public final class InterfazDelCliente {
	/** Lo último que se le mandó a cada uno: el estado se manda sólo cuando cambia, como el cofre se redibuja. */
	private static final Map<UUID, Foto> ENVIADO = new ConcurrentHashMap<>();
	private static final Set<UUID> PREFIEREN_PANTALLA = ConcurrentHashMap.newKeySet();

	private InterfazDelCliente() {
	}

	/** Lo que cambia lo que muestra la pantalla. La config se compara por valor: un {@code recargar} igual no reenvía. */
	private record Foto(int experiencia, int nivel, PuntosDelJugador puntos, Configuracion config, boolean grupo) {
		static Foto de(ServerPlayer jugador) {
			return new Foto(Mejoras.experiencia(jugador), jugador.experienceLevel, Mejoras.puntos(jugador), EstadisticasRPG.config(), Mejoras.puntosDelGrupo());
		}
	}

	public static void registrar() {
		Paquetes.registrar();
		// Fabric corre estos receptores en el hilo del servidor: se puede comprar directo
		ServerPlayNetworking.registerGlobalReceiver(Paquetes.Comprar.TIPO, (pedido, contexto) -> comprar(contexto.player(), pedido));
		ServerPlayNetworking.registerGlobalReceiver(Paquetes.Bajar.TIPO, (pedido, contexto) -> bajar(contexto.player(), pedido));
		ServerPlayNetworking.registerGlobalReceiver(Paquetes.Preferencia.TIPO, (preferencia, contexto) -> preferir(contexto.player(), preferencia.pantalla()));
		ServerPlayNetworking.registerGlobalReceiver(Paquetes.AbrirCofre.TIPO, (pedido, contexto) -> MenuDeEstadisticas.abrir(contexto.player()));
		ServerTickEvents.END_SERVER_TICK.register(InterfazDelCliente::sincronizarATodos);
		ServerPlayConnectionEvents.DISCONNECT.register((conexion, servidor) -> olvidar(conexion.getPlayer().getUUID()));
	}

	/**
	 * {@code /estadisticas}: la pantalla si el cliente la tiene y la prefiere, si no el cofre. Se pregunta por el
	 * canal del estado y no por el de abrir: un cliente de una versión anterior registra el de abrir, pero no el
	 * estado nuevo, y la pantalla se quedaría esperando al servidor.
	 */
	public static void abrir(ServerPlayer jugador) {
		if (PREFIEREN_PANTALLA.contains(jugador.getUUID()) && ServerPlayNetworking.canSend(jugador, Paquetes.Estado.TIPO)) {
			sincronizar(jugador, true);
			ServerPlayNetworking.send(jugador, new Paquetes.AbrirPantalla());
		} else {
			MenuDeEstadisticas.abrir(jugador);
		}
	}

	public static void preferir(ServerPlayer jugador, boolean pantalla) {
		if (pantalla) {
			PREFIEREN_PANTALLA.add(jugador.getUUID());
		} else {
			PREFIEREN_PANTALLA.remove(jugador.getUUID());
		}
	}

	/**
	 * Lo que pide el cliente no se cree: un cliente modificado puede mandar cualquier id o cantidad. Con una
	 * cantidad negativa, {@link Mejoras#comprar} cobraría experiencia negativa, o sea que la regalaría.
	 *
	 * @return vacío si el pedido no era válido y no se intentó comprar
	 */
	public static Optional<Mejoras.Compra> comprar(ServerPlayer jugador, Paquetes.Comprar pedido) {
		Optional<Estadistica> estadistica = Estadistica.porId(pedido.id());
		if (estadistica.isEmpty() || pedido.cantidad() < 1) {
			return Optional.empty();
		}
		// El tope ya corta la compra; esto es para que el mensaje no diga "no te alcanzó para los 2147483647"
		int cantidad = Math.min(pedido.cantidad(), EstadisticasRPG.config().de(estadistica.get()).tope());
		Mejoras.Compra compra = Mejoras.comprar(jugador, estadistica.get(), Math.max(cantidad, 1));
		if (ServerPlayNetworking.canSend(jugador, Paquetes.Resultado.TIPO)) {
			ServerPlayNetworking.send(jugador, new Paquetes.Resultado(Mejoras.mensaje(compra).getString(), compra.comprados() > 0));
		}
		sincronizar(jugador, false);
		return Optional.of(compra);
	}

	/** Como {@link #comprar}: un id que no existe o una cantidad menor que 1 no se intentan. */
	public static Optional<Mejoras.Devolucion> bajar(ServerPlayer jugador, Paquetes.Bajar pedido) {
		Optional<Estadistica> estadistica = Estadistica.porId(pedido.id());
		if (estadistica.isEmpty() || pedido.cantidad() < 1) {
			return Optional.empty();
		}
		Mejoras.Devolucion devolucion = Mejoras.bajar(jugador, estadistica.get(), pedido.cantidad());
		if (ServerPlayNetworking.canSend(jugador, Paquetes.Resultado.TIPO)) {
			ServerPlayNetworking.send(jugador, new Paquetes.Resultado(Mejoras.mensaje(devolucion).getString(), devolucion.bajados() > 0));
		}
		sincronizar(jugador, false);
		return Optional.of(devolucion);
	}

	/** Lo que se le manda al cliente, con los textos ya armados. */
	public static Paquetes.Estado estado(ServerPlayer jugador) {
		Configuracion config = EstadisticasRPG.config();
		PuntosDelJugador puntos = Mejoras.puntos(jugador);
		List<Paquetes.Fila> filas = new ArrayList<>();
		for (Estadistica estadistica : Estadistica.values()) {
			Configuracion.Mejora mejora = config.de(estadistica);
			int actuales = puntos.de(estadistica);
			filas.add(new Paquetes.Fila(estadistica.id(), estadistica.nombre(), actuales, mejora.tope(),
					actuales > 0 ? estadistica.describir(mejora.efecto(actuales)) : "", estadistica.describir(mejora.porPunto())));
		}
		return new Paquetes.Estado(Mejoras.experiencia(jugador), jugador.experienceLevel, config.experienciaPorPunto(), config.devolucionPorPunto(), Mejoras.puntosDelGrupo(), filas);
	}

	/**
	 * En cada tick, a los que tienen el complemento y les cambió algo: la experiencia (juntó orbes), los puntos
	 * (un op usó {@code dar}, o con los puntos del grupo compró otro) o la config ({@code recargar}).
	 */
	private static void sincronizarATodos(MinecraftServer servidor) {
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			sincronizar(jugador, false);
		}
	}

	private static void sincronizar(ServerPlayer jugador, boolean siempre) {
		if (!ServerPlayNetworking.canSend(jugador, Paquetes.Estado.TIPO)) {
			return;
		}
		Foto foto = Foto.de(jugador);
		if (!siempre && foto.equals(ENVIADO.get(jugador.getUUID()))) {
			return;
		}
		ENVIADO.put(jugador.getUUID(), foto);
		ServerPlayNetworking.send(jugador, estado(jugador));
	}

	private static void olvidar(UUID jugador) {
		ENVIADO.remove(jugador);
		PREFIEREN_PANTALLA.remove(jugador);
	}
}
