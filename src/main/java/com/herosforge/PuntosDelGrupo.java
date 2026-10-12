package com.herosforge;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Los puntos de todo el servidor cuando está Soul Link: ahí la experiencia y la vida son de
 * todos, así que los puntos también. Se guarda en {@code data/herosforge/puntos_del_grupo.dat} dentro
 * del mundo. El attachment de cada jugador no se toca: si se saca Soul Link, cada uno vuelve a
 * tener los suyos.
 */
public class PuntosDelGrupo extends SavedData {
	private static final Codec<PuntosDelGrupo> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
			PuntosDelJugador.CODEC.optionalFieldOf("puntos", PuntosDelJugador.vacio()).forGetter(grupo -> grupo.puntos),
			UUIDUtil.CODEC_SET.optionalFieldOf("jugadores_sumados", Set.of()).forGetter(grupo -> grupo.sumados)
	).apply(instancia, PuntosDelGrupo::new));

	// DataFixTypes null: Fabric API se encarga de que no se intente aplicar data fixers a datos de mods
	private static final SavedDataType<PuntosDelGrupo> TIPO = new SavedDataType<>(
			HerosForge.id("puntos_del_grupo"), PuntosDelGrupo::new, CODEC, null);

	private PuntosDelJugador puntos;
	private final Set<UUID> sumados;

	private PuntosDelGrupo() {
		this(PuntosDelJugador.vacio(), Set.of());
	}

	private PuntosDelGrupo(PuntosDelJugador puntos, Set<UUID> sumados) {
		this.puntos = puntos;
		this.sumados = new HashSet<>(sumados);
	}

	public static PuntosDelGrupo de(MinecraftServer servidor) {
		return servidor.getDataStorage().computeIfAbsent(TIPO);
	}

	public PuntosDelJugador puntos() {
		return puntos;
	}

	public void cambiar(PuntosDelJugador nuevos) {
		puntos = nuevos;
		setDirty();
	}

	/**
	 * La primera vez que alguien entra con los puntos compartidos, los que había comprado por su cuenta se
	 * suman al grupo: por estadística, el máximo entre lo del grupo y lo suyo (no la suma, que dejaría pasar
	 * el tope). Una sola vez por jugador: si después se reinicia el grupo, sus puntos viejos no vuelven.
	 *
	 * @return si cambiaron los puntos del grupo
	 */
	public boolean sumarIndividuales(UUID jugador, PuntosDelJugador individuales) {
		if (!sumados.add(jugador)) {
			return false;
		}
		setDirty();
		PuntosDelJugador combinados = puntos;
		for (Estadistica estadistica : Estadistica.values()) {
			if (individuales.de(estadistica) > combinados.de(estadistica)) {
				combinados = combinados.con(estadistica, individuales.de(estadistica));
			}
		}
		if (combinados.equals(puntos)) {
			return false;
		}
		puntos = combinados;
		return true;
	}

	/** Para los tests: vuelve al estado de un mundo nuevo. */
	public void olvidarTodo() {
		puntos = PuntosDelJugador.vacio();
		sumados.clear();
		setDirty();
	}
}
