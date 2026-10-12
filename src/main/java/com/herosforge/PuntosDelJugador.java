package com.herosforge;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;

/**
 * Inmutable a propósito: es el valor del attachment, y Fabric sólo se entera de un cambio con
 * {@code setAttached}. Si se mutara el mapa adentro, el cambio no se guardaría.
 */
public record PuntosDelJugador(Map<Estadistica, Integer> puntos) {
	/*
	 * Se guarda como mapa de id → puntos y no con un codec del enum: una estadística que ya no exista
	 * (o una de una versión más nueva) se ignora en vez de hacer fallar el mapa entero, que borraría
	 * todos los puntos del jugador.
	 */
	public static final Codec<PuntosDelJugador> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
			.xmap(PuntosDelJugador::desdeIds, PuntosDelJugador::aIds);

	private static final PuntosDelJugador VACIO = new PuntosDelJugador(Map.of());

	public PuntosDelJugador {
		EnumMap<Estadistica, Integer> positivos = new EnumMap<>(Estadistica.class);
		puntos.forEach((estadistica, cantidad) -> {
			if (cantidad > 0) {
				positivos.put(estadistica, cantidad);
			}
		});
		puntos = Map.copyOf(positivos);
	}

	public static PuntosDelJugador vacio() {
		return VACIO;
	}

	public int de(Estadistica estadistica) {
		return puntos.getOrDefault(estadistica, 0);
	}

	public PuntosDelJugador con(Estadistica estadistica, int cantidad) {
		Map<Estadistica, Integer> nuevos = new EnumMap<>(Estadistica.class);
		nuevos.putAll(puntos);
		nuevos.put(estadistica, cantidad);
		return new PuntosDelJugador(nuevos);
	}

	private static PuntosDelJugador desdeIds(Map<String, Integer> porId) {
		Map<Estadistica, Integer> puntos = new EnumMap<>(Estadistica.class);
		porId.forEach((id, cantidad) -> Estadistica.porId(id).ifPresent(estadistica -> puntos.put(estadistica, cantidad)));
		return new PuntosDelJugador(puntos);
	}

	private Map<String, Integer> aIds() {
		Map<String, Integer> porId = new HashMap<>();
		puntos.forEach((estadistica, cantidad) -> porId.put(estadistica.id(), cantidad));
		return porId;
	}
}
