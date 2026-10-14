package com.herosforge;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public class HerosForge implements ModInitializer {
	public static final String MOD_ID = "herosforge";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	/** El id de mod de Soul Link. No se depende de sus clases: alcanza con saber si está. */
	public static final String SOUL_LINK = "soullink";

	/** Viaja en el NBT del jugador ({@code playerdata/<uuid>.dat}) y sobrevive a la muerte. */
	public static final AttachmentType<PuntosDelJugador> PUNTOS = AttachmentRegistry.create(id("puntos"),
			builder -> builder.persistent(PuntosDelJugador.CODEC).copyOnDeath().initializer(PuntosDelJugador::vacio));

	private static volatile Configuracion config = Configuracion.POR_DEFECTO;

	@Override
	public void onInitialize() {
		// Antes de leer nada: hasta la 0.11.1 el id era estadisticasrpg (ver MigracionDeId)
		MigracionDeId.migrarConfig(".json");
		ServerLifecycleEvents.SERVER_STARTING.register(MigracionDeId::migrarMundo);

		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivoDeConfiguracion());
		lectura.avisos().forEach(aviso -> LOGGER.warn("[config] {}", aviso));
		if (lectura.ok()) {
			config = lectura.config();
		} else {
			LOGGER.error("[config] Se usan los valores por defecto");
		}

		// Con Soul Link la experiencia y la vida son de todos: con puntos individuales, el que tiene
		// menos vida máxima le cortaría la vida al grupo, y cualquiera gastaría niveles de todos para sí mismo
		if (FabricLoader.getInstance().isModLoaded(SOUL_LINK)) {
			Mejoras.usarPuntosDelGrupo(true);
			LOGGER.info("Soul Link está cargado: los puntos son de todo el grupo");
		}

		// Los modificadores ya vienen en el NBT, pero se recalculan para que un cambio de config se aplique al reconectar
		// En un solo listener y en este orden: alEntrar tiene que ver los puntos ya pasados al id nuevo
		ServerPlayerEvents.JOIN.register(jugador -> {
			MigracionDeId.migrarJugador(jugador);
			Mejoras.alEntrar(jugador);
		});
		ServerPlayerEvents.COPY_FROM.register(Mejoras::alReaparecer);
		// Con Soul Link, el que cambia de grupo pasa a tener los puntos de su grupo nuevo
		CompatibilidadSoulLink.alCambiarDeGrupo(Mejoras::aplicar);
		CommandRegistrationCallback.EVENT.register((dispatcher, contexto, seleccion) -> ComandoEstadisticas.registrar(dispatcher));
		InterfazDelCliente.registrar();
		Curacion.registrar();
		Combate.registrar();
		Oficios.registrar();
	}

	public static Configuracion config() {
		return config;
	}

	/**
	 * Vuelve a leer el archivo. Si está roto, se queda con la config que había. {@code servidor} es null desde la
	 * pantalla de Mod Menu sin un mundo abierto: no hay jugadores a los que aplicársela.
	 */
	public static ArchivoDeConfiguracion.Lectura recargar(@Nullable MinecraftServer servidor) {
		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivoDeConfiguracion());
		if (lectura.ok() && servidor != null) {
			usar(lectura.config(), servidor);
		} else if (lectura.ok()) {
			config = lectura.config();
		}
		return lectura;
	}

	/** Cambia la config y la aplica a los que están conectados: los demás la toman al entrar ({@code JOIN}). */
	public static void usar(Configuracion nueva, MinecraftServer servidor) {
		config = nueva;
		Mejoras.aplicarATodos(servidor);
	}

	public static Path archivoDeConfiguracion() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
