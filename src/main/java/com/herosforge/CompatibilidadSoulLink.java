package com.herosforge;

import java.lang.reflect.Method;
import java.util.function.Consumer;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Puente opcional con Soul Link 3.0.0 o más nuevo, por su {@code com.soullink.api.SoulLinkApi}. Sin Soul Link
 * no se carga ninguna de sus clases.
 */
public final class CompatibilidadSoulLink {
	/** El grupo de todos cuando no hay Soul Link: como Soul Link sin grupos armados. */
	public static final String GRUPO_GENERAL = "general";

	private CompatibilidadSoulLink() {
	}

	private static boolean activa() {
		return Mejoras.puntosDelGrupo() && FabricLoader.getInstance().isModLoaded(HerosForge.SOUL_LINK);
	}

	// Se resuelve una sola vez y sólo si está instalado. Los nombres son propios de Soul Link, sin mappings.
	private static final class Acceso {
		static final Method GRUPO = metodo("groupOf", ServerPlayer.class);
		static final Method EXPERIENCIA = metodo("syncExperience", MinecraftServer.class);
		static final Method VIDA = metodo("syncHealth", MinecraftServer.class);
		static final Method IGNORAR_VIDA = metodo("ignoreHealthChange", ServerPlayer.class, float.class);
		static final Method AL_CAMBIAR_DE_GRUPO = metodo("onGroupChange", Consumer.class);
	}

	private static Method metodo(String nombre, Class<?>... parametros) {
		try {
			return Class.forName("com.soullink.api.SoulLinkApi").getMethod(nombre, parametros);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("La versión de Soul Link no es compatible con Hero's Forge (hace falta la 3.0.0 o más nueva)", e);
		}
	}

	private static Object invocar(Method metodo, Object... argumentos) {
		try {
			return metodo.invoke(null, argumentos);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("No se pudo sincronizar Hero's Forge con Soul Link", e);
		}
	}

	/**
	 * El grupo vinculado del jugador: cada grupo de Soul Link tiene sus propios puntos. Sin Soul Link (o con
	 * los puntos individuales) todos están en el general.
	 */
	public static String grupo(ServerPlayer jugador) {
		return activa() ? (String) invocar(Acceso.GRUPO, jugador) : GRUPO_GENERAL;
	}

	/** Al iniciar, si está Soul Link: el que cambia de grupo pasa a tener los puntos del grupo nuevo. */
	public static void alCambiarDeGrupo(Consumer<ServerPlayer> accion) {
		if (FabricLoader.getInstance().isModLoaded(HerosForge.SOUL_LINK)) {
			invocar(Acceso.AL_CAMBIAR_DE_GRUPO, accion);
		}
	}

	/** Antes de validar y después de cobrar/devolver: otra compra en el mismo tick ya ve el saldo nuevo. */
	public static void sincronizarExperiencia(MinecraftServer servidor) {
		if (activa()) invocar(Acceso.EXPERIENCIA, servidor);
	}

	public static void sincronizarVida(MinecraftServer servidor) {
		if (activa()) invocar(Acceso.VIDA, servidor);
	}

	/** El recorte de máximo no es daño: conserva los deltas reales y deja que Soul Link limite la barra común. */
	public static void recortarVida(ServerPlayer jugador) {
		float antes = jugador.getHealth();
		jugador.setHealth(jugador.getMaxHealth());
		if (activa()) {
			invocar(Acceso.IGNORAR_VIDA, jugador, jugador.getHealth() - antes);
		}
	}
}
