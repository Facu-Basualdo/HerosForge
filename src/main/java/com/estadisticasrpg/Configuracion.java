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
					Estadistica.VELOCIDAD, new Mejora(0.02, 10),
					// Se multiplica sobre la base más el arma: una espada pasa de 1,6 a 2,08 golpes por segundo con +30 %
					Estadistica.VELOCIDAD_ATAQUE, new Mejora(0.03, 10),
					Estadistica.MINADO, new Mejora(0.05, 10),
					Estadistica.ALCANCE, new Mejora(0.25, 8),
					// Con armadura de netherita completa (0,4) llega a 0,9: siempre queda algo de empuje
					Estadistica.EMPUJE, new Mejora(0.05, 10),
					// +3 es lo mismo que suma Suerte marina III al pescar, y se acumula con el encantamiento
					Estadistica.SUERTE, new Mejora(0.5, 6)));

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
