package com.estadisticasrpg.test;

import com.mojang.blaze3d.platform.InputConstants;

import com.estadisticasrpg.Estadistica;
import com.estadisticasrpg.Mejoras;
import com.estadisticasrpg.cliente.PantallaDeEstadisticas;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * La pantalla propia en un cliente de verdad, con el servidor integrado: lo que los gametests del servidor no
 * ven (la tecla, el botón del inventario y que una compra desde la pantalla llegue y se cobre). Deja capturas
 * en {@code build/run/clientGameTest/screenshots}.
 */
public class PantallaClientGameTest implements FabricClientGameTest {
	private static final int COSTO = 30;

	@Override
	public void runTest(ClientGameTestContext contexto) {
		try (TestSingleplayerContext mundo = contexto.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			mundo.getServer().runCommand("gamemode survival @a");
			mundo.getServer().runCommand("xp add @a " + COSTO * 7 + " points");
			mundo.getServer().runCommand("estadisticas dar @p vida 40");
			mundo.getServer().runCommand("estadisticas dar @p danio 100");
			mundo.getConnection().waitForClientboundPackets();

			// El cursor en una esquina, para que ningún tooltip tape la captura
			contexto.getInput().setCursorPos(0, 0);
			contexto.getInput().pressKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));
			contexto.waitForScreen(PantallaDeEstadisticas.class);
			contexto.waitTicks(2);
			contexto.takeScreenshot("estadisticas-pantalla");

			// El primer "+1" es el de la vida: las filas van en el orden del enum
			contexto.clickScreenButton("+1");
			mundo.getConnection().waitForServerboundPackets();
			mundo.getConnection().waitForClientboundPackets();
			contexto.waitTicks(2);
			int vida = mundo.getServer().computeOnServer(servidor -> Mejoras.puntos(servidor.getPlayerList().getPlayers().getFirst()).de(Estadistica.VIDA));
			if (vida != 41) {
				throw new AssertionError("Comprar desde la pantalla tenía que dejar 41 puntos de vida, quedaron " + vida);
			}
			int experiencia = mundo.getServer().computeOnServer(servidor -> Mejoras.experiencia(servidor.getPlayerList().getPlayers().getFirst()));
			if (experiencia != COSTO * 6) {
				throw new AssertionError("Tenía que cobrar " + COSTO + " de experiencia, quedaron " + experiencia);
			}
			contexto.takeScreenshot("estadisticas-despues-de-comprar");

			// El primer "-1" también es el de la vida: baja el punto y devuelve la mitad
			contexto.clickScreenButton("-1");
			mundo.getConnection().waitForServerboundPackets();
			mundo.getConnection().waitForClientboundPackets();
			contexto.waitTicks(2);
			vida = mundo.getServer().computeOnServer(servidor -> Mejoras.puntos(servidor.getPlayerList().getPlayers().getFirst()).de(Estadistica.VIDA));
			if (vida != 40) {
				throw new AssertionError("Bajar desde la pantalla tenía que dejar 40 puntos de vida, quedaron " + vida);
			}
			experiencia = mundo.getServer().computeOnServer(servidor -> Mejoras.experiencia(servidor.getPlayerList().getPlayers().getFirst()));
			if (experiencia != COSTO * 6 + COSTO / 2) {
				throw new AssertionError("Tenía que devolver " + COSTO / 2 + " de experiencia, quedaron " + experiencia);
			}

			// La misma tecla la cierra, y desde el inventario se vuelve a abrir con el botón
			contexto.getInput().pressKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));
			contexto.waitFor(cliente -> cliente.gui.screen() == null);
			contexto.getInput().pressKey(opciones -> opciones.keyInventory);
			contexto.waitForScreen(InventoryScreen.class);
			contexto.waitTicks(2);
			contexto.takeScreenshot("estadisticas-inventario");
			contexto.clickScreenButton("Estadísticas");
			contexto.waitForScreen(PantallaDeEstadisticas.class);
			contexto.waitTicks(2);
			// Abierta desde el inventario, Esc vuelve ahí
			contexto.getInput().pressKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_ESCAPE));
			contexto.waitForScreen(InventoryScreen.class);

			// Con la interfaz más chica que elige la escala automática (426 × 240) tienen que entrar todas las filas.
			// La K también la abre desde el inventario
			contexto.getInput().resizeWindow(852, 480);
			contexto.waitTicks(2);
			contexto.getInput().pressKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_K));
			contexto.waitForScreen(PantallaDeEstadisticas.class);
			contexto.waitTicks(2);
			contexto.takeScreenshot("estadisticas-pantalla-chica");
			// El caso de la captura original: millones de XP no deben invadir la otra columna.
			mundo.getServer().runCommand("xp set @a 989 levels");
			mundo.getConnection().waitForClientboundPackets();
			contexto.waitTicks(4);
			contexto.getInput().setCursorPos(0, 0);
			contexto.takeScreenshot("estadisticas-experiencia-alta");
			contexto.getInput().resizeWindow(1280, 800);
			contexto.waitTicks(4);
			contexto.takeScreenshot("estadisticas-pantalla-amplia");
			// Abierta desde el inventario, "Listo" también vuelve ahí
			contexto.clickScreenButton("Listo");
			contexto.waitForScreen(InventoryScreen.class);
		}
	}
}
