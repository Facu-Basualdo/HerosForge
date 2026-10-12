package com.herosforge.cliente;

import com.herosforge.cliente.mixin.AccesoAPantallaDeContenedor;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Un botón con una botella de experiencia en el inventario de supervivencia, al lado del del libro de recetas.
 * El creativo usa otra pantalla y no lo tiene: ahí queda la tecla.
 */
final class BotonDelInventario extends Button {
	private static final Component TEXTO = Component.literal("Estadísticas");
	// Mismo tamaño y altura que el del libro de recetas (InventoryScreen.getRecipeBookButtonPosition: leftPos + 104),
	// a su derecha: abajo de la grilla de crafteo hay lugar libre hasta el inventario
	private static final int ANCHO = 20;
	private static final int ALTO = 18;
	private static final int X_EN_EL_INVENTARIO = 104 + ANCHO + 2;

	private final InventoryScreen inventario;
	// No puede ser estático: la clase se carga en la pantalla de título (AFTER_INIT), antes de que vanilla
	// cargue los componentes de los ítems, y un ItemStack ahí revienta con "Components not bound yet"
	private final ItemStack icono = new ItemStack(Items.EXPERIENCE_BOTTLE);

	/**
	 * Cierra el inventario como lo cierra la E antes de abrir: sin el paquete de cerrar, el servidor dejaría los
	 * ítems en la grilla de crafteo y el del cursor quedaría en el aire.
	 */
	private BotonDelInventario(InventoryScreen inventario) {
		super(0, 0, ANCHO, ALTO, TEXTO, boton -> abrirDesde(inventario), DEFAULT_NARRATION);
		this.inventario = inventario;
		setTooltip(Tooltip.create(Component.literal("Estadísticas (").append(PantallaDeEstadisticas.teclaDeAbrir()).append(")")));
		acomodar();
	}

	static void agregar(Screen pantalla) {
		if (!(pantalla instanceof InventoryScreen inventario) || !HerosForgeCliente.config().botonEnInventario()
				|| !HerosForgeCliente.servidorConElMod()) {
			return;
		}
		BotonDelInventario boton = new BotonDelInventario(inventario);
		Screens.getWidgets(pantalla).add(boton);
		// Abrir o cerrar el libro de recetas corre el inventario sin volver a llamar a init
		ScreenEvents.beforeExtract(pantalla).register((p, graficos, mouseX, mouseY, delta) -> boton.acomodar());
		// Con una pantalla abierta las teclas no llegan a las KeyMapping del juego: sin esto, la K no haría nada en
		// el inventario. Escribiendo en el buscador del libro de recetas, la K es una letra
		ScreenKeyboardEvents.afterKeyPress(pantalla).register((p, evento) -> {
			if (HerosForgeCliente.tecla().matches(evento) && !inventario.isInputCaptured()) {
				abrirDesde(inventario);
			}
		});
	}

	private static void abrirDesde(InventoryScreen inventario) {
		inventario.onClose();
		HerosForgeCliente.abrir(Minecraft.getInstance(), true);
	}

	private void acomodar() {
		setX(((AccesoAPantallaDeContenedor) inventario).herosforge$leftPos() + X_EN_EL_INVENTARIO);
		setY(inventario.height / 2 - 22);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graficos, int mouseX, int mouseY, float delta) {
		extractDefaultSprite(graficos);
		graficos.item(icono, getX() + (ANCHO - 16) / 2, getY() + (ALTO - 16) / 2);
	}
}
