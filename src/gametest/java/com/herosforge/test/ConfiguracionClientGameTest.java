package com.herosforge.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import com.herosforge.HerosForge;
import com.herosforge.cliente.HerosForgeCliente;
import com.herosforge.cliente.PantallaDeConfiguracionDelCliente;
import com.herosforge.cliente.PantallaDeConfiguracionDelServidor;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;

/**
 * La pantalla de configuración de Mod Menu, abierta directamente (Mod Menu no está en el entorno de prueba): que
 * guardar cambie el archivo y la config del servidor integrado, que un valor que el mod rechaza deje el archivo como
 * estaba, y que la del cliente se aplique. Con el mundo abierto la pantalla lo pausa: también prueba que el servidor
 * pausado igual atiende la recarga. Deja capturas en {@code build/run/clientGameTest/screenshots}.
 */
public class ConfiguracionClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext contexto) {
		// Los botones se buscan por su texto en español: el cliente de prueba arranca en inglés
		contexto.runOnClient(cliente -> {
			cliente.options.languageCode = "es_ar";
			cliente.getLanguageManager().setSelected("es_ar");
		});
		try (TestSingleplayerContext mundo = contexto.worldBuilder().create()) {
			mundo.getConnection().waitForChunksRender();
			contexto.getInput().setCursorPos(0, 0);

			contexto.setScreen(() -> new PantallaDeConfiguracionDelServidor(null));
			contexto.waitForScreen(PantallaDeConfiguracionDelServidor.class);
			contexto.waitTicks(2);
			contexto.takeScreenshot("configuracion");

			// Un texto que no es un número no se puede guardar
			contexto.runOnClient(cliente -> campo(cliente, "Experiencia por punto").setValue("treinta"));
			if (contexto.computeOnClient(cliente -> boton(cliente, "Guardar").active)) {
				throw new AssertionError("Con \"treinta\" en un número, Guardar tenía que quedar apagado");
			}
			contexto.runOnClient(cliente -> campo(cliente, "Experiencia por punto").setValue("45"));
			contexto.clickScreenButton("Guardar");
			contexto.waitFor(cliente -> cliente.gui.screen() == null);
			int costo = mundo.getServer().computeOnServer(servidor -> HerosForge.config().experienciaPorPunto());
			if (costo != 45) {
				throw new AssertionError("Guardar tenía que dejar experienciaPorPunto en 45, quedó " + costo);
			}

			// Fuera de rango: el mod lo reemplazaría por el valor por defecto. No se guarda y el archivo queda igual
			contexto.setScreen(() -> new PantallaDeConfiguracionDelServidor(null));
			contexto.waitForScreen(PantallaDeConfiguracionDelServidor.class);
			contexto.runOnClient(cliente -> campo(cliente, "Devolución al bajar").setValue("5"));
			contexto.clickScreenButton("Guardar");
			contexto.waitTicks(10);
			if (!contexto.computeOnClient(cliente -> cliente.gui.screen() instanceof PantallaDeConfiguracionDelServidor)) {
				throw new AssertionError("Con un valor rechazado la pantalla tenía que quedar abierta, con el aviso");
			}
			contexto.takeScreenshot("configuracion-rechazada");
			String archivo = leer();
			if (!archivo.contains("\"devolucionAlBajar\": 0.5") || !archivo.contains("\"experienciaPorPunto\": 45")) {
				throw new AssertionError("El archivo tenía que quedar como estaba antes del valor rechazado:\n" + archivo);
			}
			double devolucion = mundo.getServer().computeOnServer(servidor -> HerosForge.config().devolucionAlBajar());
			if (devolucion != 0.5) {
				throw new AssertionError("La config tenía que seguir con devolucionAlBajar 0.5, quedó " + devolucion);
			}

			// La del cliente, desde su botón: apagar el botón del inventario
			contexto.clickScreenButton("Pantalla propia…");
			contexto.waitForScreen(PantallaDeConfiguracionDelCliente.class);
			contexto.waitTicks(2);
			contexto.takeScreenshot("configuracion-cliente");
			contexto.runOnClient(cliente -> widgets(cliente, CycleButton.class).get(1).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
			contexto.clickScreenButton("Guardar");
			contexto.waitForScreen(PantallaDeConfiguracionDelServidor.class);
			if (contexto.computeOnClient(cliente -> HerosForgeCliente.config().botonEnInventario())) {
				throw new AssertionError("Guardar tenía que apagar botonEnInventario");
			}
		}
	}

	private static String leer() {
		try {
			return Files.readString(HerosForge.archivoDeConfiguracion(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static EditBox campo(Minecraft cliente, String nombre) {
		return widgets(cliente, EditBox.class).stream()
				.filter(caja -> caja.getMessage().getString().equals(nombre))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No está el campo " + nombre));
	}

	private static Button boton(Minecraft cliente, String texto) {
		return widgets(cliente, Button.class).stream()
				.filter(boton -> boton.getMessage().getString().equals(texto))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No está el botón " + texto));
	}

	/** Los de la lista están adentro del contenedor con scroll, no en la pantalla directamente. */
	private static <T> List<T> widgets(Minecraft cliente, Class<T> tipo) {
		List<T> encontrados = new ArrayList<>();
		for (GuiEventListener hijo : cliente.gui.screen().children()) {
			recorrer(hijo, tipo, encontrados);
		}
		return encontrados;
	}

	private static <T> void recorrer(GuiEventListener elemento, Class<T> tipo, List<T> encontrados) {
		if (elemento instanceof AbstractWidget && tipo.isInstance(elemento)) {
			encontrados.add(tipo.cast(elemento));
		}
		if (elemento instanceof ContainerEventHandler contenedor) {
			for (GuiEventListener hijo : contenedor.children()) {
				recorrer(hijo, tipo, encontrados);
			}
		}
	}
}
