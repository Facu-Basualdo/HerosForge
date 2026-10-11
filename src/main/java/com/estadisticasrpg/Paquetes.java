package com.estadisticasrpg;

import java.util.List;

import io.netty.buffer.ByteBuf;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Lo que hablan el servidor y el complemento del cliente (la pantalla propia de {@code src/client}). Un cliente
 * vanilla no registra estos canales: el servidor nunca le manda nada y para él todo sigue siendo el cofre.
 */
public final class Paquetes {
	// Sobra para las 9 estadísticas: es sólo para que un paquete roto no reserve una lista enorme al leerlo
	private static final int MAXIMO_DE_FILAS = 64;

	private Paquetes() {
	}

	public static void registrar() {
		PayloadTypeRegistry.clientboundPlay().register(Estado.TIPO, Estado.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Resultado.TIPO, Resultado.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AbrirPantalla.TIPO, AbrirPantalla.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Comprar.TIPO, Comprar.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Preferencia.TIPO, Preferencia.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AbrirCofre.TIPO, AbrirCofre.CODEC);
	}

	/**
	 * Servidor → cliente: todo lo que muestra la pantalla. Los textos van armados en el servidor, como en el
	 * cofre: así la pantalla dice lo mismo aunque el cliente tenga otra versión del mod u otra config.
	 *
	 * @param experiencia puntos de experiencia del jugador (o del grupo), los mismos que usa {@link Mejoras#comprar}
	 */
	public record Estado(int experiencia, int nivel, int costo, boolean grupo, List<Fila> filas) implements CustomPacketPayload {
		public static final Type<Estado> TIPO = new Type<>(EstadisticasRPG.id("estado"));
		public static final StreamCodec<ByteBuf, Estado> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Estado::experiencia,
				ByteBufCodecs.VAR_INT, Estado::nivel,
				ByteBufCodecs.VAR_INT, Estado::costo,
				ByteBufCodecs.BOOL, Estado::grupo,
				Fila.CODEC.apply(ByteBufCodecs.list(MAXIMO_DE_FILAS)), Estado::filas,
				Estado::new);

		public Estado {
			filas = List.copyOf(filas);
		}

		@Override
		public Type<Estado> type() {
			return TIPO;
		}
	}

	/** Una estadística. {@code ahora} va vacío con 0 puntos; {@code proximo} es lo que suma un punto más. */
	public record Fila(String id, String nombre, int puntos, int tope, String ahora, String proximo) {
		public static final StreamCodec<ByteBuf, Fila> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Fila::id,
				ByteBufCodecs.STRING_UTF8, Fila::nombre,
				ByteBufCodecs.VAR_INT, Fila::puntos,
				ByteBufCodecs.VAR_INT, Fila::tope,
				ByteBufCodecs.STRING_UTF8, Fila::ahora,
				ByteBufCodecs.STRING_UTF8, Fila::proximo,
				Fila::new);

		public boolean enElTope() {
			return puntos >= tope;
		}
	}

	/** Servidor → cliente: cómo salió una compra pedida desde la pantalla, que tapa la barra de acción. */
	public record Resultado(String texto, boolean compro) implements CustomPacketPayload {
		public static final Type<Resultado> TIPO = new Type<>(EstadisticasRPG.id("resultado"));
		public static final StreamCodec<ByteBuf, Resultado> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Resultado::texto,
				ByteBufCodecs.BOOL, Resultado::compro,
				Resultado::new);

		@Override
		public Type<Resultado> type() {
			return TIPO;
		}
	}

	/** Servidor → cliente: {@code /estadisticas} de alguien que prefiere la pantalla al cofre. */
	public record AbrirPantalla() implements CustomPacketPayload {
		public static final Type<AbrirPantalla> TIPO = new Type<>(EstadisticasRPG.id("abrir_pantalla"));
		public static final StreamCodec<ByteBuf, AbrirPantalla> CODEC = StreamCodec.unit(new AbrirPantalla());

		@Override
		public Type<AbrirPantalla> type() {
			return TIPO;
		}
	}

	/** Cliente → servidor: el id es {@link Estadistica#id()}. El servidor no confía en nada de esto (ver {@link InterfazDelCliente#comprar}). */
	public record Comprar(String id, int cantidad) implements CustomPacketPayload {
		public static final Type<Comprar> TIPO = new Type<>(EstadisticasRPG.id("comprar"));
		public static final StreamCodec<ByteBuf, Comprar> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Comprar::id,
				ByteBufCodecs.VAR_INT, Comprar::cantidad,
				Comprar::new);

		@Override
		public Type<Comprar> type() {
			return TIPO;
		}
	}

	/** Cliente → servidor, al entrar: si {@code /estadisticas} le abre la pantalla o el cofre. */
	public record Preferencia(boolean pantalla) implements CustomPacketPayload {
		public static final Type<Preferencia> TIPO = new Type<>(EstadisticasRPG.id("preferencia"));
		public static final StreamCodec<ByteBuf, Preferencia> CODEC = StreamCodec.composite(
				ByteBufCodecs.BOOL, Preferencia::pantalla,
				Preferencia::new);

		@Override
		public Type<Preferencia> type() {
			return TIPO;
		}
	}

	/** Cliente → servidor: abrir el cofre aunque prefiera la pantalla. Con un comando, la preferencia lo mandaría de vuelta a la pantalla. */
	public record AbrirCofre() implements CustomPacketPayload {
		public static final Type<AbrirCofre> TIPO = new Type<>(EstadisticasRPG.id("abrir_cofre"));
		public static final StreamCodec<ByteBuf, AbrirCofre> CODEC = StreamCodec.unit(new AbrirCofre());

		@Override
		public Type<AbrirCofre> type() {
			return TIPO;
		}
	}
}
