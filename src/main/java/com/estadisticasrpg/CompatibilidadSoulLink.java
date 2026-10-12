package com.estadisticasrpg;

import java.lang.reflect.Method;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Puente opcional con Soul Link 1.2.1; sin él, no se cargan sus clases. */
public final class CompatibilidadSoulLink {
	private CompatibilidadSoulLink() {
	}

	private static boolean activa() {
		return Mejoras.puntosDelGrupo() && FabricLoader.getInstance().isModLoaded(EstadisticasRPG.INVENTARIO_COMPARTIDO);
	}

	// Se resuelve una sola vez y sólo si está instalado. Los nombres son propios de Soul Link, sin mappings.
	private static final class Acceso {
		static final Method EXPERIENCIA = metodo("SharedExperience", "sync", MinecraftServer.class);
		static final Method VIDA = metodo("SharedVitals", "syncHealth", MinecraftServer.class);
		static final Method ESTADO = metodo("SharedInventoryState", "get", MinecraftServer.class);
		static final Method VIDA_SINCRONIZADA = metodo("SharedInventoryState", "syncedHealth");
	}

	private static Method metodo(String clase, String nombre, Class<?>... parametros) {
		try {
			Method metodo = Class.forName("com.inventariocompartido." + clase).getDeclaredMethod(nombre, parametros);
			metodo.setAccessible(true);
			return metodo;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("La versión de Soul Link no es compatible con Hero's Forge", e);
		}
	}

	private static Object invocar(Method metodo, Object instancia, Object... argumentos) {
		try {
			return metodo.invoke(instancia, argumentos);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No se pudo sincronizar Hero's Forge con Soul Link", e);
		}
	}

	/** Antes de validar y después de cobrar/devolver: otra compra en el mismo tick ya ve el saldo nuevo. */
	public static void sincronizarExperiencia(MinecraftServer servidor) {
		if (activa()) invocar(Acceso.EXPERIENCIA, null, servidor);
	}

	public static void sincronizarVida(MinecraftServer servidor) {
		if (activa()) invocar(Acceso.VIDA, null, servidor);
	}

	/** El recorte de máximo no es daño: conserva los deltas reales y deja que Soul Link limite la barra común. */
	@SuppressWarnings("unchecked")
	public static void recortarVida(ServerPlayer jugador) {
		float antes = jugador.getHealth();
		jugador.setHealth(jugador.getMaxHealth());
		if (activa()) {
			Object estado = invocar(Acceso.ESTADO, null, jugador.level().getServer());
			Map<ServerPlayer, Float> sincronizados = (Map<ServerPlayer, Float>) invocar(Acceso.VIDA_SINCRONIZADA, estado);
			sincronizados.computeIfPresent(jugador, (quien, ultima) -> ultima + jugador.getHealth() - antes);
		}
	}
}
