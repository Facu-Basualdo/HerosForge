package com.estadisticasrpg.test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jspecify.annotations.Nullable;

import com.estadisticasrpg.Configuracion;
import com.estadisticasrpg.Estadistica;
import com.estadisticasrpg.EstadisticasRPG;
import com.estadisticasrpg.Mejoras;
import com.estadisticasrpg.PuntosDelGrupo;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

public class SoulLinkGameTest {
	@GameTest
	public void transaccionesConSoulLinkReal(GameTestHelper helper) {
		if (!FabricLoader.getInstance().isModLoaded(EstadisticasRPG.INVENTARIO_COMPARTIDO)) {
			throw new AssertionError("Esta prueba requiere el jar real de Soul Link");
		}
		MinecraftServer servidor = helper.getLevel().getServer();
		java.util.ArrayList<ServerPlayer> jugadores = new java.util.ArrayList<>();
		Configuracion anterior = EstadisticasRPG.config();
		try {
			for (int i = 0; i < 4; i++) jugadores.add(entrar(helper, "grupo" + i));
			ServerPlayer a = jugadores.get(0), b = jugadores.get(1);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarExperiencia(servidor);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarVida(servidor);
			Mejoras.dar(a, Estadistica.VIDA, 100);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarVida(servidor);
			for (ServerPlayer j : jugadores) cerca(helper, j.getHealth(), 80, "Vida inicial compartida");

			Mejoras.bajar(a, Estadistica.VIDA, 34);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarVida(servidor);
			for (ServerPlayer j : jugadores) {
				helper.assertTrue(j.isAlive(), "Bajar vida no mata al grupo");
				cerca(helper, j.getHealth(), 59.6, "El máximo se recorta una sola vez");
			}
			// Daño y curación pendientes no deben borrarse al compensar el recorte del máximo.
			a.setHealth(a.getHealth() - 2);
			b.setHealth(b.getHealth() - 1);
			b.heal(0.5f);
			Mejoras.bajar(a, Estadistica.VIDA, 1);
			for (ServerPlayer j : jugadores) cerca(helper, j.getHealth(), 57.1, "Conserva el daño y la curación pendientes");
			Mejoras.reiniciar(a);
			for (ServerPlayer j : jugadores) cerca(helper, j.getHealth(), 20, "Reiniciar tampoco multiplica el recorte");

			Mejoras.dar(a, Estadistica.VIDA, 100);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarVida(servidor);
			Map<Estadistica, Configuracion.Mejora> mejoras = new EnumMap<>(anterior.mejoras());
			mejoras.put(Estadistica.VIDA, new Configuracion.Mejora(0.6, 50));
			EstadisticasRPG.usar(new Configuracion(anterior.experienciaPorPunto(), anterior.devolucionAlBajar(), mejoras), servidor);
			for (ServerPlayer j : jugadores) cerca(helper, j.getHealth(), 50, "Recargar topes limita una sola vez");
			EstadisticasRPG.usar(anterior, servidor);
			Mejoras.reiniciar(a);

			// Vacía lo devuelto por las bajas en un único participante y sincroniza.
			a.giveExperiencePoints(-Mejoras.experiencia(a));
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarExperiencia(servidor);
			a.giveExperiencePoints(30);
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarExperiencia(servidor);
			helper.assertValueEqual(Mejoras.comprar(a, Estadistica.DANIO, 1).comprados(), 1, "Primera compra");
			helper.assertValueEqual(Mejoras.comprar(b, Estadistica.DANIO, 1).comprados(), 0, "Segunda compra del mismo tick no duplica el saldo");
			for (ServerPlayer j : jugadores) helper.assertValueEqual(Mejoras.experiencia(j), 0, "Saldo compartido agotado");
			helper.assertValueEqual(Mejoras.puntos(a).de(Estadistica.DANIO), 1, "Sólo se obtuvo un punto");
			Mejoras.bajar(a, Estadistica.DANIO, 1);
			for (ServerPlayer j : jugadores) helper.assertValueEqual(Mejoras.experiencia(j), 15, "Devolución inmediata compartida");
			b.giveExperiencePoints(15);
			helper.assertValueEqual(Mejoras.comprar(a, Estadistica.DANIO, 1).comprados(), 1, "Incluye XP pendiente de otro jugador");
			com.estadisticasrpg.CompatibilidadSoulLink.sincronizarExperiencia(servidor);
			for (ServerPlayer j : jugadores) helper.assertValueEqual(Mejoras.experiencia(j), 0, "No vuelve a cobrar al sincronizar");
		} finally {
			for (ServerPlayer j : jugadores) salir(helper, j);
			PuntosDelGrupo.de(servidor).olvidarTodo();
			EstadisticasRPG.usar(anterior, servidor);
		}
		helper.succeed();
	}

	private static void cerca(GameTestHelper helper, double actual, double esperado, String mensaje) {
		helper.assertTrue(Math.abs(actual - esperado) < 0.001, mensaje + ": " + actual + " != " + esperado);
	}
	/** Un jugador en supervivencia: makeMockServerPlayerInLevel fuerza creativo. */
	private static ServerPlayer entrar(GameTestHelper helper, String nombre) {
		return entrar(helper, nombre, null);
	}

	/** Igual, pero cargándole antes un archivo guardado, como hace vanilla en el login. */
	private static ServerPlayer entrar(GameTestHelper helper, String nombre, @Nullable CompoundTag guardado) {
		ServerLevel level = helper.getLevel();
		CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), nombre), false);
		ServerPlayer jugador = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
		if (guardado != null) {
			jugador.load(TagValueInput.create(ProblemReporter.DISCARDING, jugador.registryAccess(), guardado));
		}
		Connection conexion = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(conexion);
		level.getServer().getPlayerList().placeNewPlayer(conexion, jugador, cookie);
		jugador.setGameMode(GameType.SURVIVAL);
		return jugador;
	}

	private static void salir(GameTestHelper helper, ServerPlayer jugador) {
		helper.getLevel().getServer().getPlayerList().remove(jugador);
	}
}
