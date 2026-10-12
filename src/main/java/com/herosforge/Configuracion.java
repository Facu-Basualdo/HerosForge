package com.herosforge;

import java.util.Map;

/**
 * @param experienciaPorPunto puntos de experiencia (no niveles) que cuesta cada punto, siempre los mismos; 0 = gratis
 * @param devolucionAlBajar qué parte de {@code experienciaPorPunto} se devuelve al bajar un punto, de 0 a 1
 * @param idioma el de la consola y el de los jugadores con un idioma que el mod no tiene; cada jugador ve los textos en
 *        el de su cliente (ver {@link Idioma})
 */
public record Configuracion(int experienciaPorPunto, double devolucionAlBajar, Map<Estadistica, Mejora> mejoras, String idioma) {
	/** Cuánto pega un crítico de {@link Estadistica#CRITICO}: lo que se compra con puntos es la probabilidad. */
	public static final double MULTIPLICADOR_CRITICO = 1.5;
	/** Debajo de esta parte de su vida máxima, un enemigo recibe el daño extra de {@link Estadistica#EJECUCION}. */
	public static final double UMBRAL_EJECUCION = 0.3;
	/** Sin recibir daño durante esto, {@link Estadistica#ESCUDO} empieza a recargarse. */
	public static final int SEGUNDOS_PARA_RECARGAR_ESCUDO = 5;
	/** Lo que se recarga el escudo por segundo, en medios corazones. */
	public static final float ESCUDO_POR_SEGUNDO = 1;

	/*
	 * Pensados junto con Bloodmoon & Bosses, que llega hasta el nivel 100: las 40 al máximo son 4000 puntos y 120.000 de
	 * experiencia, así que nadie las llena todas y hay que elegir. Se cobra en puntos de experiencia y no en niveles: un nivel bajo vale 7 puntos y uno alto más
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
					Map.entry(Estadistica.RESPIRACION, new Mejora(0.03, 100)),

					// Guerrero. +40 %: el arco llega a la carga completa en 14 ticks en vez de 20
					Map.entry(Estadistica.DESTREZA, new Mejora(0.004, 100)),
					// Filo arrasador III da 0,75: al máximo queda en 0,5 sin el libro, y se suma si lo tiene
					Map.entry(Estadistica.BARRIDO, new Mejora(0.005, 100)),
					// +1 es como Empuje I
					Map.entry(Estadistica.GOLPE, new Mejora(0.01, 100)),
					// 20 % al máximo: uno de cada cinco golpes pega +50 %, en promedio +10 % de daño
					Map.entry(Estadistica.CRITICO, new Mejora(0.002, 100)),
					// +50 % para rematar: contra un jefe de 1000 de vida se nota en los últimos 300
					Map.entry(Estadistica.EJECUCION, new Mejora(0.005, 100)),

					// Guardián. La netherita completa da 12 de dureza: +8 es casi otro juego de armadura de diamante
					Map.entry(Estadistica.DUREZA, new Mejora(0.08, 100)),
					Map.entry(Estadistica.ANTIEXPLOSION, new Mejora(0.005, 100)),
					// 30 % al máximo: se suma con Protección contra el fuego, que ya llega lejos
					Map.entry(Estadistica.RESISTENCIAS, new Mejora(0.003, 100)),
					// 30 % del daño que entra después de la armadura: con Bloodmoon, un zombi de nivel 100 se lastima solo
					Map.entry(Estadistica.REPRESALIA, new Mejora(0.003, 100)),
					// 4 corazones al máximo, como una manzana dorada, y se recarga solo
					Map.entry(Estadistica.ESCUDO, new Mejora(0.08, 100)),
					// 15 % al máximo: más que eso ya es invulnerabilidad contra hordas
					Map.entry(Estadistica.ESQUIVA, new Mejora(0.0015, 100)),

					// Superviviente. La mitad de hambre al máximo
					Map.entry(Estadistica.METABOLISMO, new Mejora(0.005, 100)),
					Map.entry(Estadistica.NUTRICION, new Mejora(0.005, 100)),
					// Comer en la mitad de tiempo: un filete pasa de 1,6 a 0,8 segundos
					Map.entry(Estadistica.GLOTON, new Mejora(0.005, 100)),
					Map.entry(Estadistica.TENACIDAD, new Mejora(0.005, 100)),
					Map.entry(Estadistica.IGNIFUGO, new Mejora(0.005, 100)),

					// Explorador. +0,5: con los 0,6 de vanilla sube bloques enteros caminando
					Map.entry(Estadistica.PASO, new Mejora(0.005, 100)),
					// La mitad de Agilidad acuática III, y se suma con el encantamiento
					Map.entry(Estadistica.NADO, new Mejora(0.005, 100)),
					// Agachado pasa del 30 % al 70 % de la velocidad normal: Sigilo veloz III llega al 75 %
					Map.entry(Estadistica.SIGILO, new Mejora(0.004, 100)),
					// −25 %: salta un poco más y cae más lento, sin llegar a planear
					Map.entry(Estadistica.LIGEREZA, new Mejora(0.0025, 100)),

					// Artesano. +80 puntos es Afinidad acuática
					Map.entry(Estadistica.MINADO_SUBMARINO, new Mejora(0.008, 100)),
					// 6 bloques al máximo: alcanza para limpiar una granja de mobs sin caminarla
					Map.entry(Estadistica.IMAN, new Mejora(0.06, 100)),
					// 50 % al máximo: como Irrompibilidad I en todo el equipo, y se suma con el encantamiento
					Map.entry(Estadistica.CUIDADO_DEL_EQUIPO, new Mejora(0.005, 100)),
					// 30 %, lo mismo que Héroe de la aldea I
					Map.entry(Estadistica.REGATEO, new Mejora(0.003, 100)),
					// +25 % al máximo: la experiencia es la moneda de los puntos, así que más que eso se dispara sola
					Map.entry(Estadistica.SABIDURIA, new Mejora(0.0025, 100))),
			Idioma.INGLES);

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
