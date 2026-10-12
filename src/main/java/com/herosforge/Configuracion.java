package com.herosforge;

import java.util.Map;

/**
 * @param experienciaPorPunto puntos de experiencia (no niveles) que cuesta cada punto, siempre los mismos; 0 = gratis
 * @param devolucionAlBajar qué parte de {@code experienciaPorPunto} se devuelve al bajar un punto, de 0 a 1
 */
public record Configuracion(int experienciaPorPunto, double devolucionAlBajar, Map<Estadistica, Mejora> mejoras) {
	/*
	 * Pensados junto con Bloodmoon & Bosses, que llega hasta el nivel 100: todo al máximo son 1000 puntos y 30.000 de
	 * experiencia. Se cobra en puntos de experiencia y no en niveles: un nivel bajo vale 7 puntos y uno alto más
	 * de 100, así que cobrando niveles al principio se subía un punto cada pocos mobs, y quien juntaba niveles
	 * pagaba de más. Con 30 de experiencia, el primer punto pide unos 6 zombis y el precio no depende del nivel.
	 */
	public static final Configuracion POR_DEFECTO = new Configuracion(
			30,
			// Menos de 1 para que bajar y volver a subir cueste: si no, los puntos se mueven gratis según la pelea
			0.5,
			Map.ofEntries(
					Map.entry(Estadistica.VIDA, new Mejora(0.6, 100)),
					Map.entry(Estadistica.DANIO, new Mejora(0.25, 100)),
					// Un zombi de nivel 100 (180 de vida) pide ~11 flechas de Poder V: con +100 % baja a 5 o 6, como la espada
					Map.entry(Estadistica.DANIO_DISTANCIA, new Mejora(0.01, 100)),
					// Vanilla corta la armadura en 30 y desde 20 ya frena el 80 %: con netherita (20) más de +8 te vuelve casi inmortal
					Map.entry(Estadistica.ARMADURA, new Mejora(0.08, 100)),
					// El cliente agranda el campo de visión con la velocidad: más de +20 % ya marea
					Map.entry(Estadistica.VELOCIDAD, new Mejora(0.002, 100)),
					// Se multiplica sobre la base más el arma: una espada pasa de 1,6 a 2,08 golpes por segundo con +30 %
					Map.entry(Estadistica.VELOCIDAD_ATAQUE, new Mejora(0.003, 100)),
					Map.entry(Estadistica.MINADO, new Mejora(0.01, 100)),
					Map.entry(Estadistica.ALCANCE, new Mejora(0.02, 100)),
					// Con armadura de netherita completa (0,4) llega a 0,9: siempre queda algo de empuje
					Map.entry(Estadistica.EMPUJE, new Mejora(0.005, 100)),
					// +3 es lo mismo que suma Suerte marina III al pescar, y se acumula con el encantamiento
					Map.entry(Estadistica.SUERTE, new Mejora(0.03, 100)),
					// 9 corazones por minuto al máximo: entre peleas se nota, pero en una pelea no le gana a un mob de nivel 100
					Map.entry(Estadistica.REGENERACION, new Mejora(0.003, 100)),
					// 10 % al máximo: un golpe de 25 cura 2,5, menos de lo que pega un zombi de nivel 50
					Map.entry(Estadistica.ROBO_DE_VIDA, new Mejora(0.001, 100)),
					// +20 % de fuerza salta 1,8 bloques: no llega a 2, así que no saltea las paredes de 2 de una base
					Map.entry(Estadistica.SALTO, new Mejora(0.002, 100)),
					Map.entry(Estadistica.CAIDA, new Mejora(0.1, 100)),
					// +3 es lo mismo que Respiración III
					Map.entry(Estadistica.RESPIRACION, new Mejora(0.03, 100))));

	public Configuracion {
		mejoras = Map.copyOf(mejoras);
	}

	public Mejora de(Estadistica estadistica) {
		return mejoras.get(estadistica);
	}

	/** La experiencia que se devuelve por cada punto que se baja, redondeada para abajo: nunca más de lo que costó. */
	public int devolucionPorPunto() {
		return (int) Math.floor(experienciaPorPunto * devolucionAlBajar);
	}

	public record Mejora(double porPunto, int tope) {
		/** Si se baja el tope en la config, los puntos de más se conservan pero dejan de sumar. */
		public double efecto(int puntos) {
			return porPunto * Math.min(puntos, tope);
		}
	}
}
