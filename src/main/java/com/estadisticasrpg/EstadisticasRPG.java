package com.estadisticasrpg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import net.minecraft.resources.Identifier;

public class EstadisticasRPG implements ModInitializer {
	public static final String MOD_ID = "estadisticasrpg";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Viaja en el NBT del jugador ({@code playerdata/<uuid>.dat}) y sobrevive a la muerte. */
	public static final AttachmentType<PuntosDelJugador> PUNTOS = AttachmentRegistry.create(id("puntos"),
			builder -> builder.persistent(PuntosDelJugador.CODEC).copyOnDeath().initializer(PuntosDelJugador::vacio));

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		// Los modificadores ya vienen en el NBT, pero se recalculan para que un cambio de config se aplique al reconectar
		ServerPlayerEvents.JOIN.register(Mejoras::aplicar);
		ServerPlayerEvents.COPY_FROM.register(Mejoras::alReaparecer);
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoEstadisticas.registrar(dispatcher));
	}

	public static Configuracion config() {
		return config;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
