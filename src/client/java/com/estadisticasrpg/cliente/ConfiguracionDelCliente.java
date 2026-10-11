package com.estadisticasrpg.cliente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.estadisticasrpg.EstadisticasRPG;

import net.fabricmc.loader.api.FabricLoader;

/**
 * {@code config/estadisticasrpg-cliente.json}, de cada jugador (no del servidor).
 *
 * @param usarPantalla si la tecla, el botón del inventario y {@code /estadisticas} abren la pantalla propia; con
 *                     {@code false} abren el cofre del servidor, como sin el complemento
 * @param botonEnInventario si el inventario (E) muestra el botón que abre las estadísticas
 */
public record ConfiguracionDelCliente(boolean usarPantalla, boolean botonEnInventario) {
	public static final ConfiguracionDelCliente POR_DEFECTO = new ConfiguracionDelCliente(true, true);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/** Un campo que falta o que no es booleano usa el valor por defecto; un JSON roto, todos. Si el archivo no existe, lo crea. */
	public static ConfiguracionDelCliente cargar() {
		Path archivo = archivo();
		if (!Files.exists(archivo)) {
			escribir(archivo, POR_DEFECTO);
			return POR_DEFECTO;
		}
		try {
			JsonElement leido = JsonParser.parseString(Files.readString(archivo, StandardCharsets.UTF_8));
			if (!leido.isJsonObject()) {
				EstadisticasRPG.LOGGER.warn("[config del cliente] {} no es un objeto JSON: se usan los valores por defecto", archivo.getFileName());
				return POR_DEFECTO;
			}
			JsonObject raiz = leido.getAsJsonObject();
			return new ConfiguracionDelCliente(
					booleano(raiz, "usarPantalla", POR_DEFECTO.usarPantalla),
					booleano(raiz, "botonEnInventario", POR_DEFECTO.botonEnInventario));
		} catch (IOException | JsonParseException e) {
			EstadisticasRPG.LOGGER.warn("[config del cliente] No se pudo leer {}: se usan los valores por defecto", archivo.getFileName(), e);
			return POR_DEFECTO;
		}
	}

	private static boolean booleano(JsonObject raiz, String clave, boolean porDefecto) {
		JsonElement valor = raiz.get(clave);
		if (valor != null && valor.isJsonPrimitive() && valor.getAsJsonPrimitive().isBoolean()) {
			return valor.getAsBoolean();
		}
		if (valor != null) {
			EstadisticasRPG.LOGGER.warn("[config del cliente] {} tiene que ser true o false: se usa {}", clave, porDefecto);
		}
		return porDefecto;
	}

	private static void escribir(Path archivo, ConfiguracionDelCliente config) {
		JsonObject raiz = new JsonObject();
		raiz.addProperty("usarPantalla", config.usarPantalla);
		raiz.addProperty("botonEnInventario", config.botonEnInventario);
		try {
			Files.createDirectories(archivo.getParent());
			Files.writeString(archivo, GSON.toJson(raiz), StandardCharsets.UTF_8);
		} catch (IOException e) {
			EstadisticasRPG.LOGGER.warn("[config del cliente] No se pudo escribir {}", archivo.getFileName(), e);
		}
	}

	private static Path archivo() {
		return FabricLoader.getInstance().getConfigDir().resolve(EstadisticasRPG.MOD_ID + "-cliente.json");
	}
}
