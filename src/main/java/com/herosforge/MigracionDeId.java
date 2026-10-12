package com.herosforge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Hasta la 0.11.1 el id del mod era {@code estadisticasrpg}. Con el id nuevo, todo lo guardado con el viejo quedaría
 * huérfano: la config, los puntos del grupo y los puntos de cada jugador. Esto lo pasa al id nuevo la primera vez que
 * se lo encuentra, sin que el dueño del servidor tenga que hacer nada.
 *
 * <p>El attachment viejo tiene que seguir registrado para siempre: Fabric descarta al cargar un attachment cuyo id no
 * conoce, y un jugador que no entra desde antes del cambio perdería sus puntos la primera vez que vuelva.
 */
public final class MigracionDeId {
	static final String ID_VIEJO = "estadisticasrpg";

	/** El de antes, con el mismo codec: sólo para leerlo y pasarlo a {@link HerosForge#PUNTOS}. */
	public static final AttachmentType<PuntosDelJugador> PUNTOS_VIEJOS = AttachmentRegistry.create(viejo("puntos"),
			builder -> builder.persistent(PuntosDelJugador.CODEC));

	private MigracionDeId() {
	}

	/** En {@code onInitialize}, antes de leer la config: {@code config/estadisticasrpg.json} → {@code config/herosforge.json}. */
	public static void migrarConfig(String sufijo) {
		Path carpeta = FabricLoader.getInstance().getConfigDir();
		if (moverSiFalta(carpeta.resolve(ID_VIEJO + sufijo), carpeta.resolve(HerosForge.MOD_ID + sufijo))) {
			HerosForge.LOGGER.info("Se renombró config/{}{} a config/{}{} (el id del mod cambió)", ID_VIEJO, sufijo, HerosForge.MOD_ID, sufijo);
		}
	}

	/** SERVER_STARTING, antes de que se carguen los mundos: los puntos del grupo, en {@code world/data/<id>/}. */
	public static void migrarMundo(MinecraftServer servidor) {
		Path data = servidor.getWorldPath(LevelResource.DATA);
		String archivo = "puntos_del_grupo.dat";
		if (moverSiFalta(data.resolve(ID_VIEJO).resolve(archivo), data.resolve(HerosForge.MOD_ID).resolve(archivo))) {
			HerosForge.LOGGER.info("Se movió {} de data/{} a data/{} (el id del mod cambió)", archivo, ID_VIEJO, HerosForge.MOD_ID);
		}
	}

	/**
	 * Al entrar, antes de {@link Mejoras#alEntrar}: pasa los puntos al attachment nuevo y saca los modificadores con el
	 * id viejo. Vanilla guarda los modificadores permanentes en el NBT: si quedaran los viejos, {@code aplicar} pondría
	 * los nuevos al lado y cada estadística sumaría dos veces.
	 */
	public static void migrarJugador(ServerPlayer jugador) {
		PuntosDelJugador viejos = jugador.getAttached(PUNTOS_VIEJOS);
		if (viejos != null) {
			// Vacío también cuenta como "no tiene": cualquier lectura con getAttachedOrCreate lo crea vacío
			PuntosDelJugador nuevos = jugador.getAttached(HerosForge.PUNTOS);
			if (nuevos == null || nuevos.equals(PuntosDelJugador.vacio())) {
				jugador.setAttached(HerosForge.PUNTOS, viejos);
			}
			jugador.removeAttached(PUNTOS_VIEJOS);
			HerosForge.LOGGER.info("Se pasaron los puntos de {} al id nuevo", jugador.getName().getString());
		}
		for (Estadistica estadistica : Estadistica.values()) {
			for (Holder<Attribute> atributo : estadistica.atributos()) {
				AttributeInstance instancia = jugador.getAttribute(atributo);
				if (instancia != null) {
					instancia.removeModifier(viejo(estadistica.id()));
				}
			}
		}
	}

	/** Mueve el archivo sólo si el nuevo no existe: si ya hay uno con el id nuevo, es el que vale. */
	public static boolean moverSiFalta(Path viejo, Path nuevo) {
		if (!Files.exists(viejo) || Files.exists(nuevo)) {
			return false;
		}
		try {
			Files.createDirectories(nuevo.getParent());
			Files.move(viejo, nuevo);
			return true;
		} catch (IOException e) {
			// Mejor no arrancar que arrancar sin los puntos y pisar los de verdad al guardar
			throw new UncheckedIOException("No se pudo mover " + viejo + " a " + nuevo, e);
		}
	}

	public static Identifier viejo(String path) {
		return Identifier.fromNamespaceAndPath(ID_VIEJO, path);
	}
}
