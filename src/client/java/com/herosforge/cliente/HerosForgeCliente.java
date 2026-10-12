package com.herosforge.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import org.jspecify.annotations.Nullable;

import com.herosforge.HerosForge;
import com.herosforge.Paquetes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * El complemento del cliente: una pantalla propia para ver y comprar estadísticas, con una tecla y un botón en
 * el inventario. Es opcional: sin esto, o con {@code usarPantalla} en {@code false}, se usa el cofre del servidor.
 */
public class HerosForgeCliente implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(HerosForge.id("estadisticas"));

	private static final KeyMapping TECLA = new KeyMapping("key.herosforge.abrir", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, CATEGORIA);

	private static ConfiguracionDelCliente config = ConfiguracionDelCliente.POR_DEFECTO;
	/** Lo último que mandó el servidor. Null hasta que llega el primero, o si el servidor no tiene el mod. */
	private static Paquetes.@Nullable Estado estado;

	@Override
	public void onInitializeClient() {
		config = ConfiguracionDelCliente.cargar();

		// Los receptores del cliente corren en el hilo de render: se puede tocar la pantalla directo
		ClientPlayNetworking.registerGlobalReceiver(Paquetes.Estado.TIPO, (nuevo, contexto) -> estado = nuevo);
		ClientPlayNetworking.registerGlobalReceiver(Paquetes.Resultado.TIPO, (resultado, contexto) -> {
			if (contexto.client().gui.screen() instanceof PantallaDeEstadisticas pantalla) {
				pantalla.mostrar(resultado);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(Paquetes.AbrirPantalla.TIPO, (pedido, contexto) -> contexto.client().gui.setScreen(new PantallaDeEstadisticas(false)));
		ClientPlayConnectionEvents.JOIN.register((conexion, enviador, cliente) -> {
			if (ClientPlayNetworking.canSend(Paquetes.Preferencia.TIPO)) {
				ClientPlayNetworking.send(new Paquetes.Preferencia(config.usarPantalla()));
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((conexion, cliente) -> estado = null);

		KeyMappingHelper.registerKeyMapping(TECLA);
		ClientTickEvents.END_CLIENT_TICK.register(cliente -> {
			while (TECLA.consumeClick()) {
				abrir(cliente, false);
			}
		});

		ScreenEvents.AFTER_INIT.register((cliente, pantalla, ancho, alto) -> BotonDelInventario.agregar(pantalla));
	}

	public static ConfiguracionDelCliente config() {
		return config;
	}

	public static KeyMapping tecla() {
		return TECLA;
	}

	public static Paquetes.@Nullable Estado estado() {
		return estado;
	}

	/**
	 * Si el servidor tiene esta versión del mod: un servidor vanilla no registra los canales, y uno de una versión
	 * anterior no tiene el de bajar ni manda el estado que lee esta pantalla, que se quedaría esperándolo.
	 */
	public static boolean servidorConElMod() {
		return ClientPlayNetworking.canSend(Paquetes.Bajar.TIPO);
	}

	/** @param desdeElInventario si Esc tiene que volver al inventario */
	public static void abrir(Minecraft cliente, boolean desdeElInventario) {
		if (cliente.player == null) {
			return;
		}
		if (!servidorConElMod()) {
			cliente.player.sendOverlayMessage(IdiomaDelCliente.tr("herosforge.cliente.sin_mod"));
			return;
		}
		if (config.usarPantalla()) {
			cliente.gui.setScreen(new PantallaDeEstadisticas(desdeElInventario));
		} else {
			abrirCofre();
		}
	}

	public static void abrirCofre() {
		ClientPlayNetworking.send(new Paquetes.AbrirCofre());
	}
}
