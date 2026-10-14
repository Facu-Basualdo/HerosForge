package com.herosforge.cliente;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/** {@code config/herosforge-cliente.json}: lo de la pantalla propia, que es de cada jugador y no del servidor. */
public final class PantallaDeConfiguracionDelCliente extends PantallaDeConfiguracion {
	public PantallaDeConfiguracionDelCliente(@Nullable Screen anterior) {
		super(IdiomaDelCliente.tr("herosforge.config.pantalla.titulo_cliente"), anterior, ConfiguracionDelCliente.archivo());
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
		HerosForgeCliente.recargarConfig();
		return List.of();
	}

	@Override
	protected boolean esDelServidor() {
		return false;
	}
}
