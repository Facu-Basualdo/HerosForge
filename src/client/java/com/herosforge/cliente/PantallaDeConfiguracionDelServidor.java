package com.herosforge.cliente;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.herosforge.Estadistica;
import com.herosforge.HerosForge;
import com.herosforge.Idioma;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/**
 * {@code config/herosforge.json} desde Mod Menu. Con un mundo abierto en esta instancia se aplica como
 * {@code /estadisticas recargar}: los jugadores conectados reciben los valores nuevos enseguida.
 */
public final class PantallaDeConfiguracionDelServidor extends PantallaDeConfiguracion {
	public PantallaDeConfiguracionDelServidor(@Nullable Screen anterior) {
		super(IdiomaDelCliente.tr("herosforge.config.pantalla.titulo"), anterior, HerosForge.archivoDeConfiguracion());
	}

	@Override
	protected @Nullable MutableComponent traducir(String clave, Object... args) {
		return IdiomaDelCliente.siExiste(clave, args);
	}

	@Override
	protected String prefijo() {
		return "herosforge.config";
	}

	@Override
	protected List<Component> recargar(@Nullable MinecraftServer servidor) {
		return HerosForge.recargar(servidor).avisos().stream().<Component>map(Component::literal).toList();
	}

	@Override
	protected List<String> opciones(String ruta) {
		return ruta.equals("idioma") ? Idioma.DISPONIBLES : List.of();
	}

	/** Cada estadística con su nombre, no con su id ({@code robo_de_vida}). */
	@Override
	protected Component nombre(String ruta, String clave) {
		if (ruta.equals("estadisticas." + clave)) {
			return Estadistica.porId(clave)
					.<Component>map(estadistica -> Component.literal(estadistica.nombre(IdiomaDelCliente.idioma())))
					.orElseGet(() -> super.nombre(ruta, clave));
		}
		return super.nombre(ruta, clave);
	}

	@Override
	protected void agregarBotones(LinearLayout pie) {
		pie.addChild(Button.builder(IdiomaDelCliente.tr("herosforge.config.pantalla.cliente"),
				boton -> minecraft.gui.setScreen(new PantallaDeConfiguracionDelCliente(this))).width(100).build());
	}
}
