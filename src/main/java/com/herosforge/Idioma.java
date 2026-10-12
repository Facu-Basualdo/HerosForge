package com.herosforge;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Los textos del mod en inglés y español, en {@code assets/herosforge/lang/<idioma>.json}.
 *
 * <p>Todo lo que ve un jugador lo arma el servidor para él (el menú de cofre, los mensajes, las filas que se le mandan a
 * la pantalla propia), así que va en el idioma que informa su cliente. La pantalla propia arma sus etiquetas en el
 * cliente, con el idioma que tiene elegido (ver {@code PantallaDeEstadisticas}).
 *
 * <p>En las plantillas, {@code %s} o {@code %1$s} son argumentos y {@code %%} es un porcentaje, como en los archivos de
 * idioma de vanilla.
 */
public final class Idioma {
	public static final String INGLES = "en_us";
	public static final String ESPANOL = "es_ar";
	public static final List<String> DISPONIBLES = List.of(INGLES, ESPANOL);

	private static final Pattern MARCA = Pattern.compile("%(?:(\\d+)\\$)?s|%%");
	private static final Map<String, Map<String, String>> TEXTOS = new HashMap<>();

	static {
		for (String idioma : DISPONIBLES) {
			TEXTOS.put(idioma, leer(idioma));
		}
	}

	private Idioma() {
	}

	/** El idioma con el que se le habla a este jugador, o el de la config si es null (la consola). */
	public static String de(@Nullable ServerPlayer jugador) {
		return jugador == null ? HerosForge.config().idioma() : resolver(jugador.clientInformation().language());
	}

	/** {@code es_mx} → {@code es_ar}, {@code en_gb} → {@code en_us}; uno que no tenemos → el de la config. */
	public static String resolver(String delCliente) {
		String idioma = delCliente.toLowerCase(Locale.ROOT);
		if (TEXTOS.containsKey(idioma)) {
			return idioma;
		}
		if (idioma.startsWith("es")) {
			return ESPANOL;
		}
		if (idioma.startsWith("en")) {
			return INGLES;
		}
		return HerosForge.config().idioma();
	}

	public static Set<String> claves(String idioma) {
		return Set.copyOf(TEXTOS.getOrDefault(idioma, Map.of()).keySet());
	}

	/** La plantilla sin reemplazar; si falta en ese idioma, la de inglés, y si falta en los dos, la clave. */
	public static String plantilla(String idioma, String clave) {
		String texto = TEXTOS.getOrDefault(idioma, Map.of()).get(clave);
		if (texto == null) {
			texto = TEXTOS.get(INGLES).get(clave);
		}
		return texto == null ? clave : texto;
	}

	public static MutableComponent tr(@Nullable ServerPlayer jugador, String clave, Object... args) {
		return tr(de(jugador), clave, args);
	}

	public static MutableComponent tr(CommandSourceStack fuente, String clave, Object... args) {
		return tr(de(fuente.getPlayer()), clave, args);
	}

	/** Ya traducido: un {@code Component} literal. Un argumento {@code Component} se agrega tal cual, con su estilo. */
	public static MutableComponent tr(String idioma, String clave, Object... args) {
		String plantilla = plantilla(idioma, clave);
		MutableComponent resultado = Component.empty();
		Matcher marca = MARCA.matcher(plantilla);
		int ultimo = 0;
		int siguiente = 0;
		while (marca.find()) {
			resultado.append(plantilla.substring(ultimo, marca.start()));
			if (marca.group().equals("%%")) {
				resultado.append("%");
			} else {
				int indice = marca.group(1) != null ? Integer.parseInt(marca.group(1)) - 1 : siguiente++;
				Object arg = indice < args.length ? args[indice] : "";
				resultado.append(arg instanceof Component componente ? componente.copy() : Component.literal(String.valueOf(arg)));
			}
			ultimo = marca.end();
		}
		resultado.append(plantilla.substring(ultimo));
		return resultado;
	}

	/** Con dos decimales como mucho y sin ceros de más; coma decimal en español: 2,5 y 2.5. */
	public static String numero(String idioma, double valor) {
		double redondeado = Math.round(valor * 100) / 100.0;
		String texto = redondeado == Math.rint(redondeado) ? Long.toString((long) redondeado) : Double.toString(redondeado);
		return idioma.startsWith("es") ? texto.replace('.', ',') : texto;
	}

	private static Map<String, String> leer(String idioma) {
		Map<String, String> textos = new HashMap<>();
		String ruta = "/assets/" + HerosForge.MOD_ID + "/lang/" + idioma + ".json";
		try (InputStream entrada = Idioma.class.getResourceAsStream(ruta)) {
			if (entrada == null) {
				HerosForge.LOGGER.error("Falta {} en el jar", ruta);
				return textos;
			}
			JsonObject raiz = JsonParser.parseReader(new InputStreamReader(entrada, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entrada2 : raiz.entrySet()) {
				textos.put(entrada2.getKey(), entrada2.getValue().getAsString());
			}
		} catch (IOException | RuntimeException e) {
			HerosForge.LOGGER.error("No se pudo leer {}", ruta, e);
		}
		return textos;
	}
}
