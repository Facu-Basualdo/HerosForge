package com.estadisticasrpg;

import java.util.Map;

/**
 * @param experienciaPorPunto puntos de experiencia (no niveles) que cuesta cada punto, siempre los mismos; 0 = gratis
 */
public record Configuracion(int experienciaPorPunto, Map<Estadistica, Mejora> mejoras) {
	/*
	 * Pensados junto con Mobs Poderosos, que llega hasta el nivel 100: todo al máximo son 900 puntos y 27.000 de
	 * experiencia. Se cobra en puntos de experiencia y no en niveles: un nivel bajo vale 7 puntos y uno alto más
	 * de 100, así que cobrando niveles al principio se subía un punto cada pocos mobs, y quien juntaba niveles
	 * pagaba de más. Con 30 de experiencia, el primer punto pide unos 6 zombis y el precio no depende del nivel.
	 */
	public static final Configuracion POR_DEFECTO = new Configuracion(
			30,
			Map.of(
					Estadistica.VIDA, new Mejora(0.6, 100),
					Estadistica.DANIO, new Mejora(0.25, 100),
					// Vanilla corta la armadura en 30 y desde 20 ya frena el 80 %: con netherita (20) más de +8 te vuelve casi inmortal
					Estadistica.ARMADURA, new Mejora(0.08, 100),
					// El cliente agranda el campo de visión con la velocidad: más de +20 % ya marea
					Estadistica.VELOCIDAD, new Mejora(0.002, 100),
					// Se multiplica sobre la base más el arma: una espada pasa de 1,6 a 2,08 golpes por segundo con +30 %
					Estadistica.VELOCIDAD_ATAQUE, new Mejora(0.003, 100),
					Estadistica.MINADO, new Mejora(0.01, 100),
					Estadistica.ALCANCE, new Mejora(0.02, 100),
					// Con armadura de netherita completa (0,4) llega a 0,9: siempre queda algo de empuje
					Estadistica.EMPUJE, new Mejora(0.005, 100),
					// +3 es lo mismo que suma Suerte marina III al pescar, y se acumula con el encantamiento
					Estadistica.SUERTE, new Mejora(0.03, 100)));

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
