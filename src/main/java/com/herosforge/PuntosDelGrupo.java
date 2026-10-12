package com.herosforge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Los puntos de cada grupo vinculado cuando está Soul Link: ahí la experiencia y la vida son del grupo,
 * así que los puntos también. Sin grupos armados en Soul Link, todos están en el general. Se guarda en
 * {@code data/herosforge/puntos_del_grupo.dat} dentro del mundo: el general en {@code puntos}, como cuando
 * había un solo grupo, y los demás en {@code grupos}. El attachment de cada jugador no se toca: si se saca
 * Soul Link, cada uno vuelve a tener los suyos.
 */
public class PuntosDelGrupo extends SavedData {
	private static final Codec<PuntosDelGrupo> CODEC = RecordCodecBuilder.create(instancia -> instancia.group(
			PuntosDelJugador.CODEC.optionalFieldOf("puntos", PuntosDelJugador.vacio()).forGetter(grupo -> grupo.puntos(CompatibilidadSoulLink.GRUPO_GENERAL)),
			Codec.unboundedMap(Codec.STRING, PuntosDelJugador.CODEC).optionalFieldOf("grupos", Map.of()).forGetter(PuntosDelGrupo::otrosGrupos),
			UUIDUtil.CODEC_SET.optionalFieldOf("jugadores_sumados", Set.of()).forGetter(grupo -> grupo.sumados)
	).apply(instancia, PuntosDelGrupo::new));

	// DataFixTypes null: Fabric API se encarga de que no se intente aplicar data fixers a datos de mods
	private static final SavedDataType<PuntosDelGrupo> TIPO = new SavedDataType<>(
			HerosForge.id("puntos_del_grupo"), PuntosDelGrupo::new, CODEC, null);

	private final Map<String, PuntosDelJugador> porGrupo = new HashMap<>();
	// De todo el servidor y no por grupo: si alguien cambia de grupo, sus puntos individuales no se suman otra vez
	private final Set<UUID> sumados;

	private PuntosDelGrupo() {
		this(PuntosDelJugador.vacio(), Map.of(), Set.of());
	}

	private PuntosDelGrupo(PuntosDelJugador general, Map<String, PuntosDelJugador> otros, Set<UUID> sumados) {
		this.porGrupo.putAll(otros);
		this.porGrupo.put(CompatibilidadSoulLink.GRUPO_GENERAL, general);
		this.sumados = new HashSet<>(sumados);
	}

	public static PuntosDelGrupo de(MinecraftServer servidor) {
		return servidor.getDataStorage().computeIfAbsent(TIPO);
	}

	/** Los de ese grupo; uno que nunca compró nada no tiene puntos. */
	public PuntosDelJugador puntos(String grupo) {
		return porGrupo.getOrDefault(grupo, PuntosDelJugador.vacio());
	}

	public void cambiar(String grupo, PuntosDelJugador nuevos) {
		porGrupo.put(grupo, nuevos);
		setDirty();
	}

	/**
	 * La primera vez que alguien entra con los puntos compartidos, los que había comprado por su cuenta se
	 * suman al grupo: por estadística, el máximo entre lo del grupo y lo suyo (no la suma, que dejaría pasar
	 * el tope). Una sola vez por jugador: si después se reinicia el grupo, sus puntos viejos no vuelven.
	 *
	 * @return si cambiaron los puntos del grupo
	 */
	public boolean sumarIndividuales(String grupo, UUID jugador, PuntosDelJugador individuales) {
		if (!sumados.add(jugador)) {
			return false;
		}
		setDirty();
		PuntosDelJugador puntos = puntos(grupo);
		PuntosDelJugador combinados = puntos;
		for (Estadistica estadistica : Estadistica.values()) {
			if (individuales.de(estadistica) > combinados.de(estadistica)) {
				combinados = combinados.con(estadistica, individuales.de(estadistica));
			}
		}
		if (combinados.equals(puntos)) {
			return false;
		}
		porGrupo.put(grupo, combinados);
		return true;
	}

	/** Para los tests: vuelve al estado de un mundo nuevo. */
	public void olvidarTodo() {
		porGrupo.clear();
		porGrupo.put(CompatibilidadSoulLink.GRUPO_GENERAL, PuntosDelJugador.vacio());
		sumados.clear();
		setDirty();
	}

	private Map<String, PuntosDelJugador> otrosGrupos() {
		Map<String, PuntosDelJugador> otros = new HashMap<>(porGrupo);
		otros.remove(CompatibilidadSoulLink.GRUPO_GENERAL);
		return otros;
	}
}
