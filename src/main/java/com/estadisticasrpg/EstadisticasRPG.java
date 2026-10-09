package com.estadisticasrpg;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class EstadisticasRPG implements ModInitializer {
	public static final String MOD_ID = "estadisticasrpg";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Viaja en el NBT del jugador ({@code playerdata/<uuid>.dat}) y sobrevive a la muerte. */
	public static final AttachmentType<PuntosDelJugador> PUNTOS = AttachmentRegistry.create(id("puntos"),
			builder -> builder.persistent(PuntosDelJugador.CODEC).copyOnDeath().initializer(PuntosDelJugador::vacio));

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivoDeConfiguracion());
		lectura.avisos().forEach(aviso -> LOGGER.warn("[config] {}", aviso));
		if (lectura.ok()) {
			config = lectura.config();
		} else {
			LOGGER.error("[config] Se usan los valores por defecto");
		}

		// Los modificadores ya vienen en el NBT, pero se recalculan para que un cambio de config se aplique al reconectar
		ServerPlayerEvents.JOIN.register(Mejoras::aplicar);
		ServerPlayerEvents.COPY_FROM.register(Mejoras::alReaparecer);
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoEstadisticas.registrar(dispatcher));
	}

	public static Configuracion config() {
		return config;
	}

	/** Vuelve a leer el archivo. Si está roto, se queda con la config que había. */
	public static ArchivoDeConfiguracion.Lectura recargar(MinecraftServer servidor) {
		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivoDeConfiguracion());
		if (lectura.ok()) {
			usar(lectura.config(), servidor);
		}
		return lectura;
	}

	/** Cambia la config y la aplica a los que están conectados: los demás la toman al entrar ({@code JOIN}). */
	public static void usar(Configuracion nueva, MinecraftServer servidor) {
		config = nueva;
		for (ServerPlayer jugador : servidor.getPlayerList().getPlayers()) {
			Mejoras.aplicar(jugador);
		}
	}

	public static Path archivoDeConfiguracion() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
