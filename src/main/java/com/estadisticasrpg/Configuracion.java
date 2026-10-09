package com.estadisticasrpg;

import java.util.Map;

public record Configuracion(int costoPorPunto, Map<Estadistica, Mejora> mejoras) {
	public static final Configuracion POR_DEFECTO = new Configuracion(
			1,
			Map.of(
					Estadistica.VIDA, new Mejora(2.0, 10),
					Estadistica.DANIO, new Mejora(0.5, 10),
					// Vanilla corta la armadura en 30: con armadura completa de netherita (20) los 10 puntos todavía suman
					Estadistica.ARMADURA, new Mejora(1.0, 10),
					// El cliente agranda el campo de visión con la velocidad: más de +20 % ya marea
					Estadistica.VELOCIDAD, new Mejora(0.02, 10)));

	public Configuracion {
		mejoras = Map.copyOf(mejoras);
	}

	public Mejora de(Estadistica estadistica) {
		return mejoras.get(estadistica);
	}

	public record Mejora(double porPunto, int tope) {
		/** Si se baja el tope en la config, los puntos de más se conservan pero dejan de sumar. */
		public double efecto(int puntos) {
			return porPunto * Math.min(puntos, tope);
		}
	}
}
