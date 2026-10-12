package com.herosforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

/**
 * {@code config/herosforge.json}. Un campo inválido o que falta usa el valor por defecto y deja un aviso,
 * en vez de tirar abajo toda la config: con un tope mal escrito, el resto de los números del dueño del
 * servidor se siguen respetando.
 */
public final class ArchivoDeConfiguracion {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private ArchivoDeConfiguracion() {
	}

	/** {@code config} es null si el archivo no se pudo leer (JSON roto): quien llama se queda con la que tenía. */
	public record Lectura(@Nullable Configuracion config, List<String> avisos) {
		public boolean ok() {
			return config != null;
		}
	}

	/**
	 * Lee el archivo. Si no existe, lo crea con los valores por defecto. Si le faltan estadísticas (por ejemplo,
	 * después de actualizar el mod), las agrega al archivo para que el dueño del servidor vea que existen.
	 */
	public static Lectura cargar(Path archivo) {
		if (!Files.exists(archivo)) {
			escribir(archivo, Configuracion.POR_DEFECTO);
			return new Lectura(Configuracion.POR_DEFECTO, List.of());
		}

		JsonObject raiz;
		try {
			JsonElement leido = JsonParser.parseString(Files.readString(archivo, StandardCharsets.UTF_8));
			if (!leido.isJsonObject()) {
				return new Lectura(null, List.of(archivo.getFileName() + " no es un objeto JSON"));
			}
			raiz = leido.getAsJsonObject();
		} catch (IOException | JsonParseException e) {
			return new Lectura(null, List.of("No se pudo leer " + archivo.getFileName() + ": " + e.getMessage()));
		}

		List<String> avisos = new ArrayList<>();
		Configuracion defecto = Configuracion.POR_DEFECTO;
		int experiencia = entero(raiz, "experienciaPorPunto", defecto.experienciaPorPunto(), 0, "experienciaPorPunto", avisos);
		double devolucion = fraccion(raiz, "devolucionAlBajar", defecto.devolucionAlBajar(), avisos);

		JsonObject estadisticas = raiz.has("estadisticas") && raiz.get("estadisticas").isJsonObject()
				? raiz.getAsJsonObject("estadisticas")
				: new JsonObject();
		for (String id : estadisticas.keySet()) {
			if (Estadistica.porId(id).isEmpty()) {
				avisos.add("Se ignora \"" + id + "\": no es una estadística (las que hay: " + ids() + ")");
			}
		}

		// Un archivo de antes de la 0.7.0 tiene costoPorPunto y puntosPorAumento (en niveles): al reescribirlo se van
		boolean faltaAlgo = !raiz.has("experienciaPorPunto") || !raiz.has("devolucionAlBajar") || !raiz.has("estadisticas") || !raiz.has("idioma");
		String idioma = idioma(raiz, defecto.idioma(), avisos);
		Map<Estadistica, Configuracion.Mejora> mejoras = new EnumMap<>(Estadistica.class);
		for (Estadistica estadistica : Estadistica.values()) {
			Configuracion.Mejora porDefecto = defecto.de(estadistica);
			JsonElement elemento = estadisticas.get(estadistica.id());
			if (elemento == null || !elemento.isJsonObject()) {
				faltaAlgo = true;
				mejoras.put(estadistica, porDefecto);
				continue;
			}
			JsonObject campos = elemento.getAsJsonObject();
			String donde = "estadisticas." + estadistica.id();
			double porPunto = decimal(campos, "porPunto", porDefecto.porPunto(), donde + ".porPunto", avisos);
			int tope = entero(campos, "tope", porDefecto.tope(), 0, donde + ".tope", avisos);
			mejoras.put(estadistica, new Configuracion.Mejora(porPunto, tope));
		}

		Configuracion config = new Configuracion(experiencia, devolucion, mejoras, idioma);
		// Se reescribe con lo leído, no con los valores por defecto, así no se pisa nada que el dueño haya cambiado.
		// Con avisos no: se perdería el valor que escribió mal en vez de dejárselo para que lo corrija
		if (faltaAlgo && avisos.isEmpty()) {
			escribir(archivo, config);
		}
		return new Lectura(config, avisos);
	}

	public static void escribir(Path archivo, Configuracion config) {
		JsonObject raiz = new JsonObject();
		raiz.addProperty("experienciaPorPunto", config.experienciaPorPunto());
		raiz.addProperty("devolucionAlBajar", config.devolucionAlBajar());
		JsonObject estadisticas = new JsonObject();
		for (Estadistica estadistica : Estadistica.values()) {
			Configuracion.Mejora mejora = config.de(estadistica);
			JsonObject campos = new JsonObject();
			campos.addProperty("porPunto", mejora.porPunto());
			campos.addProperty("tope", mejora.tope());
			estadisticas.add(estadistica.id(), campos);
		}
		raiz.add("estadisticas", estadisticas);
		raiz.addProperty("idioma", config.idioma());
		try {
			Files.createDirectories(archivo.getParent());
			Files.writeString(archivo, GSON.toJson(raiz) + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			HerosForge.LOGGER.error("No se pudo escribir {}", archivo, e);
		}
	}

	private static String idioma(JsonObject raiz, String porDefecto, List<String> avisos) {
		JsonElement valor = raiz.get("idioma");
		if (valor == null) {
			return porDefecto;
		}
		String idioma = valor.isJsonPrimitive() ? valor.getAsString().toLowerCase(java.util.Locale.ROOT) : "";
		if (!Idioma.DISPONIBLES.contains(idioma)) {
			avisos.add("idioma tiene que ser " + String.join(" o ", Idioma.DISPONIBLES) + " (dice " + valor + "): se usa " + porDefecto);
			return porDefecto;
		}
		return idioma;
	}

	private static int entero(JsonObject objeto, String campo, int porDefecto, int minimo, String donde, List<String> avisos) {
		JsonPrimitive valor = numero(objeto, campo);
		if (valor == null) {
			if (objeto.has(campo)) {
				avisos.add(donde + " tiene que ser un número: se usa " + porDefecto);
			}
			return porDefecto;
		}
		double leido = valor.getAsDouble();
		if (leido != Math.rint(leido) || leido < minimo || leido > Integer.MAX_VALUE) {
			avisos.add(donde + " tiene que ser un entero desde " + minimo + " (dice " + valor + "): se usa " + porDefecto);
			return porDefecto;
		}
		return (int) leido;
	}

	private static double decimal(JsonObject objeto, String campo, double porDefecto, String donde, List<String> avisos) {
		JsonPrimitive valor = numero(objeto, campo);
		if (valor == null) {
			if (objeto.has(campo)) {
				avisos.add(donde + " tiene que ser un número: se usa " + porDefecto);
			}
			return porDefecto;
		}
		double leido = valor.getAsDouble();
		// Un porPunto negativo restaría al comprar: el jugador pagaría para empeorar
		if (!Double.isFinite(leido) || leido <= 0) {
			avisos.add(donde + " tiene que ser mayor que 0 (dice " + valor + "): se usa " + porDefecto);
			return porDefecto;
		}
		return leido;
	}

	/** De 0 a 1: más de 1 devolvería más de lo que costó, y bajar y subir de nuevo fabricaría experiencia. */
	private static double fraccion(JsonObject objeto, String campo, double porDefecto, List<String> avisos) {
		JsonPrimitive valor = numero(objeto, campo);
		if (valor == null) {
			if (objeto.has(campo)) {
				avisos.add(campo + " tiene que ser un número: se usa " + porDefecto);
			}
			return porDefecto;
		}
		double leido = valor.getAsDouble();
		if (!Double.isFinite(leido) || leido < 0 || leido > 1) {
			avisos.add(campo + " tiene que estar entre 0 y 1 (dice " + valor + "): se usa " + porDefecto);
			return porDefecto;
		}
		return leido;
	}

	private static @Nullable JsonPrimitive numero(JsonObject objeto, String campo) {
		JsonElement elemento = objeto.get(campo);
		return elemento != null && elemento.isJsonPrimitive() && elemento.getAsJsonPrimitive().isNumber() ? elemento.getAsJsonPrimitive() : null;
	}

	private static String ids() {
		List<String> ids = new ArrayList<>();
		for (Estadistica estadistica : Estadistica.values()) {
			ids.add(estadistica.id());
		}
		return String.join(", ", ids);
	}
}
