package com.herosforge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Un cofre de 3 filas armado en el servidor: el cliente vanilla lo ve como {@code GENERIC_9x3}, así que no
 * hace falta el mod del lado del cliente. Los ítems son de muestra: un click compra, baja o cambia de página,
 * nunca se mueven.
 *
 * <p>Las 40 estadísticas no entran en un cofre: se abre en una portada con las 5 ramas, y cada rama es una
 * página con sus estadísticas en la fila del medio y las 5 ramas abajo, como pestañas.
 */
public class MenuDeEstadisticas extends AbstractContainerMenu {
	private static final int FILAS = 3;
	private static final int TAMANIO = FILAS * 9;
	private static final int SLOT_INFO = 4;
	private static final int SLOT_VOLVER = 0;
	private static final int FILA_DEL_MEDIO = 9;
	private static final int FILA_DE_ABAJO = 18;
	private static final int CON_SHIFT = 5;
	private static final int CLICK_DERECHO = 1;
	private static final Component TITULO = Component.literal("Estadísticas");

	private final ServerPlayer jugador;
	private final SimpleContainer muestra = new SimpleContainer(TAMANIO);
	/** {@code null} es la portada. */
	private @Nullable Rama abierta;
	private int experienciaMostrada = -1;
	private @Nullable PuntosDelJugador puntosMostrados;
	private @Nullable Configuracion configMostrada;

	private MenuDeEstadisticas(int id, ServerPlayer jugador) {
		super(MenuType.GENERIC_9x3, id);
		this.jugador = jugador;
		for (int fila = 0; fila < FILAS; fila++) {
			for (int columna = 0; columna < 9; columna++) {
				addSlot(new SlotDeMuestra(muestra, columna + fila * 9, 8 + columna * 18, 18 + fila * 18));
			}
		}
		addStandardInventorySlots(jugador.getInventory(), 8, 18 + FILAS * 18 + 13);
		redibujar();
	}

	public static void abrir(ServerPlayer jugador) {
		jugador.openMenu(new SimpleMenuProvider((id, inventario, quien) -> new MenuDeEstadisticas(id, jugador), TITULO));
	}

	/** El slot de una estadística en la página de su rama: centradas en la fila del medio, en el orden de {@link Estadistica}. */
	public static int slotDe(Estadistica estadistica) {
		List<Estadistica> deLaRama = estadistica.rama().estadisticas();
		return FILA_DEL_MEDIO + enLaFila(deLaRama.indexOf(estadistica), deLaRama.size());
	}

	/** El slot de una rama en la portada (fila del medio). En las páginas, las pestañas van en la de abajo, en la misma columna. */
	public static int slotDe(Rama rama) {
		return FILA_DEL_MEDIO + enLaFila(rama.ordinal(), Rama.values().length);
	}

	private static int pestania(Rama rama) {
		return FILA_DE_ABAJO + enLaFila(rama.ordinal(), Rama.values().length);
	}

	/** La columna dentro de una fila de 9 con {@code cantidad} centradas. */
	private static int enLaFila(int indice, int cantidad) {
		// Con pocas, separadas por un hueco; con más de 5 no entran separadas en 9 slots
		if (cantidad <= 5) {
			return (9 - (2 * cantidad - 1)) / 2 + 2 * indice;
		}
		return (9 - cantidad) / 2 + indice;
	}

	private @Nullable Estadistica estadisticaEn(int slot) {
		if (abierta == null) {
			return null;
		}
		for (Estadistica estadistica : abierta.estadisticas()) {
			if (slotDe(estadistica) == slot) {
				return estadistica;
			}
		}
		return null;
	}

	/** La rama a la que lleva un slot: las de la portada o las pestañas de una página. */
	private @Nullable Rama ramaEn(int slot) {
		for (Rama rama : Rama.values()) {
			if (abierta == null ? slotDe(rama) == slot : pestania(rama) == slot) {
				return rama;
			}
		}
		return null;
	}

	/** {@code null} para volver a la portada. */
	public void mostrar(@Nullable Rama rama) {
		abierta = rama;
		redibujar();
	}

	public @Nullable Rama abierta() {
		return abierta;
	}

	@Override
	public void clicked(int slotId, int boton, ContainerInput entrada, Player quien) {
		if (slotId < 0 || slotId >= TAMANIO) {
			super.clicked(slotId, boton, entrada, quien);
			return;
		}
		boolean click = entrada == ContainerInput.PICKUP || entrada == ContainerInput.QUICK_MOVE;
		Estadistica estadistica = estadisticaEn(slotId);
		Rama rama = ramaEn(slotId);
		if (click && rama != null) {
			mostrar(rama);
		} else if (click && abierta != null && slotId == SLOT_VOLVER) {
			mostrar(null);
		} else if (estadistica != null && click) {
			int cantidad = entrada == ContainerInput.QUICK_MOVE ? CON_SHIFT : 1;
			// Con shift, vanilla también manda el botón: 1 es el derecho
			if (boton == CLICK_DERECHO) {
				jugador.sendSystemMessage(Mejoras.mensaje(Mejoras.bajar(jugador, estadistica, cantidad)), true);
			} else {
				jugador.sendSystemMessage(Mejoras.mensaje(Mejoras.comprar(jugador, estadistica, cantidad)), true);
			}
			redibujar();
		}
		// El cliente vanilla ya movió el ítem en pantalla antes de mandar el click: sin reenviar todo,
		// el ítem queda "fantasma" en su cursor
		sendAllDataToRemote();
	}

	/** El doble click en el inventario del jugador junta los ítems iguales de todos los slots, también los de muestra. */
	@Override
	public boolean canTakeItemForPickAll(ItemStack cursor, Slot slot) {
		return !(slot instanceof SlotDeMuestra) && super.canTakeItemForPickAll(cursor, slot);
	}

	/** Arrastrar empieza en un slot del jugador y puede pasar por los del menú. */
	@Override
	public boolean canDragTo(Slot slot) {
		return !(slot instanceof SlotDeMuestra) && super.canDragTo(slot);
	}

	@Override
	public ItemStack quickMoveStack(Player quien, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player quien) {
		return true;
	}

	/** Si cambia la experiencia (juntó o gastó), los puntos (un op usó {@code dar}) o la config ({@code recargar}) con el menú abierto. */
	@Override
	public void broadcastChanges() {
		if (Mejoras.experiencia(jugador) != experienciaMostrada || !Mejoras.puntos(jugador).equals(puntosMostrados) || HerosForge.config() != configMostrada) {
			redibujar();
		}
		super.broadcastChanges();
	}

	private void redibujar() {
		Configuracion config = HerosForge.config();
		PuntosDelJugador puntos = Mejoras.puntos(jugador);
		for (int slot = 0; slot < TAMANIO; slot++) {
			muestra.setItem(slot, relleno());
		}
		muestra.setItem(SLOT_INFO, info(config));
		if (abierta == null) {
			for (Rama rama : Rama.values()) {
				muestra.setItem(slotDe(rama), rama(rama, config, puntos, false));
			}
		} else {
			muestra.setItem(SLOT_VOLVER, item(new ItemStack(Items.ARROW), texto("← Todas las ramas", ChatFormatting.YELLOW), List.of()));
			for (Estadistica estadistica : abierta.estadisticas()) {
				muestra.setItem(slotDe(estadistica), icono(estadistica, config, puntos.de(estadistica)));
			}
			for (Rama rama : Rama.values()) {
				muestra.setItem(pestania(rama), rama(rama, config, puntos, rama == abierta));
			}
		}
		experienciaMostrada = Mejoras.experiencia(jugador);
		puntosMostrados = puntos;
		configMostrada = config;
	}

	/** En la portada y como pestaña. La que está abierta brilla, como un ítem encantado. */
	private static ItemStack rama(Rama rama, Configuracion config, PuntosDelJugador puntos, boolean abierta) {
		List<Component> lore = new ArrayList<>();
		lore.add(texto(rama.resumen(), ChatFormatting.GRAY));
		int total = 0;
		for (Estadistica estadistica : rama.estadisticas()) {
			int actuales = puntos.de(estadistica);
			total += actuales;
			lore.add(texto(" " + estadistica.nombre() + ": " + actuales + "/" + config.de(estadistica).tope(), actuales > 0 ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));
		}
		lore.add(1, texto(total + (total == 1 ? " punto" : " puntos") + " en la rama", ChatFormatting.GOLD));
		lore.add(texto(abierta ? "Es la página abierta" : "Click: ver la rama", ChatFormatting.DARK_GRAY));
		ItemStack icono = item(new ItemStack(rama.icono()), texto(rama.nombre(), abierta ? ChatFormatting.GOLD : ChatFormatting.YELLOW), lore);
		icono.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, new LinkedHashSet<>(List.of(DataComponents.ATTRIBUTE_MODIFIERS))));
		if (abierta) {
			icono.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return icono;
	}

	private ItemStack info(Configuracion config) {
		List<Component> lore = new ArrayList<>();
		lore.add(texto("Nivel " + jugador.experienceLevel, ChatFormatting.GRAY));
		lore.add(texto("Cada punto cuesta " + Mejoras.experiencia(config.experienciaPorPunto()), ChatFormatting.GRAY));
		if (Mejoras.puntosDelGrupo()) {
			lore.add(texto("Los puntos son de todo el grupo:", ChatFormatting.GOLD));
			lore.add(texto("lo que compra uno lo tienen todos", ChatFormatting.GOLD));
		}
		if (abierta == null) {
			lore.add(texto("Elegí una rama para ver sus estadísticas", ChatFormatting.DARK_GRAY));
			return item(new ItemStack(Items.EXPERIENCE_BOTTLE), texto("Tenés " + Mejoras.experiencia(Mejoras.experiencia(jugador)), ChatFormatting.GREEN), lore);
		}
		lore.add(texto("Click: comprar 1 punto", ChatFormatting.DARK_GRAY));
		lore.add(texto("Shift + click: comprar " + CON_SHIFT, ChatFormatting.DARK_GRAY));
		lore.add(texto("Click derecho: bajar 1 y te devuelve " + Mejoras.experiencia(config.devolucionPorPunto()), ChatFormatting.DARK_GRAY));
		lore.add(texto("Shift + click derecho: bajar " + CON_SHIFT, ChatFormatting.DARK_GRAY));
		return item(new ItemStack(Items.EXPERIENCE_BOTTLE), texto("Tenés " + Mejoras.experiencia(Mejoras.experiencia(jugador)), ChatFormatting.GREEN), lore);
	}

	private ItemStack icono(Estadistica estadistica, Configuracion config, int puntos) {
		Configuracion.Mejora mejora = config.de(estadistica);
		List<Component> lore = new ArrayList<>();
		lore.add(texto("Puntos: " + puntos + "/" + mejora.tope(), ChatFormatting.GRAY));
		if (puntos > 0) {
			lore.add(texto("Ahora: " + estadistica.describir(mejora.efecto(puntos)), ChatFormatting.GRAY));
		}
		if (puntos > 0) {
			lore.add(texto("Click derecho: bajar 1 (devuelve " + Mejoras.experiencia(config.devolucionPorPunto()) + ")", ChatFormatting.DARK_GRAY));
		}
		if (puntos >= mejora.tope()) {
			return item(new ItemStack(Items.STAINED_GLASS_PANE.gray()), texto(estadistica.nombre() + " · máximo", ChatFormatting.GRAY), lore);
		}
		int costo = config.experienciaPorPunto();
		boolean alcanza = Mejoras.experiencia(jugador) >= costo;
		lore.add(texto("Próximo punto: " + estadistica.describir(mejora.porPunto()), ChatFormatting.AQUA));
		lore.add(texto("Cuesta " + Mejoras.experiencia(costo), alcanza ? ChatFormatting.GREEN : ChatFormatting.RED));
		ItemStack icono = item(new ItemStack(estadistica.icono()), texto(estadistica.nombre(), alcanza ? ChatFormatting.GREEN : ChatFormatting.RED), lore);
		// La espada y el peto mostrarían su daño y su armadura de vanilla, que no tienen nada que ver con los puntos
		icono.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, new LinkedHashSet<>(List.of(DataComponents.ATTRIBUTE_MODIFIERS))));
		return icono;
	}

	private static ItemStack relleno() {
		ItemStack relleno = new ItemStack(Items.STAINED_GLASS_PANE.black());
		relleno.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, new LinkedHashSet<>()));
		return relleno;
	}

	private static ItemStack item(ItemStack stack, Component nombre, List<Component> lore) {
		stack.set(DataComponents.ITEM_NAME, nombre);
		stack.set(DataComponents.LORE, new ItemLore(lore));
		return stack;
	}

	/** Vanilla pone el lore en cursiva: sin el {@code withItalic(false)} todo el menú se ve torcido. */
	private static Component texto(String texto, ChatFormatting color) {
		return Component.literal(texto).withStyle(color).withStyle(estilo -> estilo.withItalic(false));
	}

	/** No se saca ni se pone nada: un click se convierte en una compra en {@link #clicked}. */
	private static final class SlotDeMuestra extends Slot {
		SlotDeMuestra(SimpleContainer contenedor, int indice, int x, int y) {
			super(contenedor, indice, x, y);
		}

		@Override
		public boolean mayPickup(Player quien) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
