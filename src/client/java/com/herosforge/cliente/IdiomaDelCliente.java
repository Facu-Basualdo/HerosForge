package com.herosforge.cliente;

import org.jspecify.annotations.Nullable;

import com.herosforge.Idioma;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;

/**
 * Las etiquetas que arma el cliente (botones, títulos, tooltips de la pantalla propia), en el idioma que el jugador
 * tiene elegido. Se lee cada vez: se puede cambiar de idioma con la pantalla abierta. Lo que viene del servidor (los
 * nombres de las estadísticas, los efectos) ya llega en ese mismo idioma, porque el servidor lee el que informa el
 * cliente.
 */
final class IdiomaDelCliente {
	private IdiomaDelCliente() {
	}

	static String idioma() {
		return Idioma.resolver(Minecraft.getInstance().getLanguageManager().getSelected());
	}

	static MutableComponent tr(String clave, Object... args) {
		return Idioma.tr(idioma(), clave, args);
	}

	static String texto(String clave, Object... args) {
		return tr(clave, args).getString();
	}

	/** Null si el mod no tiene esa clave: la pantalla de configuración arma entonces un nombre con la clave del JSON. */
	static @Nullable MutableComponent siExiste(String clave, Object... args) {
		String idioma = idioma();
		return Idioma.claves(idioma).contains(clave) ? Idioma.tr(idioma, clave, args) : null;
	}
}
