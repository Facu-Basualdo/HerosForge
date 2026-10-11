package com.estadisticasrpg.cliente;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.estadisticasrpg.Estadistica;
import com.estadisticasrpg.Paquetes;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * La pantalla propia: una fila por estadística con sus puntos, lo que da y los botones para comprar. No decide
 * nada: muestra el último {@link Paquetes.Estado} que mandó el servidor y le pide las compras, que el servidor
 * valida y cobra igual que desde el cofre.
 */
public class PantallaDeEstadisticas extends Screen {
	private static final Component TITULO = Component.literal("Estadísticas");
	private static final int CON_CINCO = 5;
	// Pensada para lo más chico que elige la escala automática de la interfaz (426 × 240): las filas van en una
	// sola columna, con una línea cada una, y se achican hasta ALTO_FILA_MINIMO para que entren todas
	private static final int ANCHO = 400;
	private static final int ALTO_CABECERA = 28;
	private static final int ALTO_FILA_MAXIMO = 18;
	private static final int ALTO_FILA_MINIMO = 14;
	private static final int ALTO_PIE = 38;
	private static final int ANCHO_BOTON = 20;
	private static final int MARGEN = 6;
	private static final int X_NOMBRE = 26;
	private static final int ANCHO_NOMBRE = 124;
	private static final int TICKS_DEL_MENSAJE = 100;

	private static final int BLANCO = ARGB.opaque(0xFFFFFF);
	private static final int GRIS = ARGB.opaque(0xA0A0A0);
	private static final int GRIS_OSCURO = ARGB.opaque(0x606060);
	private static final int VERDE = ARGB.opaque(0x55FF55);
	private static final int ROJO = ARGB.opaque(0xFF5555);
	private static final int DORADO = ARGB.opaque(0xFFAA00);

	private final boolean volverAlInventario;
	private final List<Renglon> renglones = new ArrayList<>();
	private Paquetes.@Nullable Estado dibujado;
	private int x0;
	private int y0;
	private int alto;
	private int altoFila = ALTO_FILA_MAXIMO;
	private Paquetes.@Nullable Resultado mensaje;
	private int ticksDelMensaje;

	private record Renglon(Paquetes.Fila fila, int y, Button uno, Button cinco) {
	}

	/** @param volverAlInventario si se abrió desde el inventario: Esc vuelve ahí en vez de cerrar todo */
	public PantallaDeEstadisticas(boolean volverAlInventario) {
		super(TITULO);
		this.volverAlInventario = volverAlInventario;
	}

	public static Component teclaDeAbrir() {
		return EstadisticasRPGCliente.tecla().getTranslatedKeyMessage();
	}

	@Override
	protected void init() {
		renglones.clear();
		dibujado = EstadisticasRPGCliente.estado();
		int filas = Math.max(dibujado == null ? 0 : dibujado.filas().size(), 1);
		altoFila = Math.clamp((height - 4 - ALTO_CABECERA - ALTO_PIE) / filas, ALTO_FILA_MINIMO, ALTO_FILA_MAXIMO);
		alto = ALTO_CABECERA + filas * altoFila + ALTO_PIE;
		x0 = (width - ANCHO) / 2;
		y0 = Math.max(2, (height - alto) / 2);

		if (dibujado != null) {
			int y = y0 + ALTO_CABECERA;
			for (Paquetes.Fila fila : dibujado.filas()) {
				Button uno = addRenderableWidget(botonDeCompra(fila, 1, x0 + ANCHO - MARGEN - 2 * ANCHO_BOTON - 2, y + 1));
				Button cinco = addRenderableWidget(botonDeCompra(fila, CON_CINCO, x0 + ANCHO - MARGEN - ANCHO_BOTON, y + 1));
				renglones.add(new Renglon(fila, y, uno, cinco));
				y += altoFila;
			}
		}
		actualizarBotones();

		int yBotones = y0 + alto - 24;
		addRenderableWidget(Button.builder(Component.literal("Ver como cofre"), boton -> EstadisticasRPGCliente.abrirCofre())
				.bounds(width / 2 - 104, yBotones, 100, 20)
				.tooltip(Tooltip.create(Component.literal("El menú de cofre del servidor, como sin el complemento")))
				.build());
		addRenderableWidget(Button.builder(Component.literal("Listo"), boton -> onClose())
				.bounds(width / 2 + 4, yBotones, 100, 20)
				.build());
	}

	private Button botonDeCompra(Paquetes.Fila fila, int cantidad, int x, int y) {
		String texto = cantidad == 1 ? "Comprar 1 punto" : "Comprar " + cantidad + " puntos (o los que alcancen)";
		return Button.builder(Component.literal("+" + cantidad), boton -> comprar(fila, cantidad))
				.bounds(x, y, ANCHO_BOTON, altoFila - 2)
				.tooltip(Tooltip.create(Component.literal(texto)))
				.build();
	}

	private void comprar(Paquetes.Fila fila, int cantidad) {
		if (EstadisticasRPGCliente.servidorConElMod()) {
			ClientPlayNetworking.send(new Paquetes.Comprar(fila.id(), cantidad));
		}
	}

	/** Del hilo de render, cuando el servidor contesta una compra. */
	void mostrar(Paquetes.Resultado resultado) {
		mensaje = resultado;
		ticksDelMensaje = TICKS_DEL_MENSAJE;
	}

	@Override
	public void tick() {
		if (ticksDelMensaje > 0 && --ticksDelMensaje == 0) {
			mensaje = null;
		}
		Paquetes.Estado actual = EstadisticasRPGCliente.estado();
		if (actual == dibujado) {
			return;
		}
		// Con las mismas estadísticas en el mismo orden alcanza con los botones: rearmar todo le saca el foco al
		// que navega con el teclado, y la experiencia cambia con cada orbe
		if (actual != null && dibujado != null && mismasEstadisticas(actual, dibujado)) {
			dibujado = actual;
			for (int i = 0; i < renglones.size(); i++) {
				Renglon viejo = renglones.get(i);
				renglones.set(i, new Renglon(actual.filas().get(i), viejo.y(), viejo.uno(), viejo.cinco()));
			}
			actualizarBotones();
		} else {
			rebuildWidgets();
		}
	}

	private static boolean mismasEstadisticas(Paquetes.Estado a, Paquetes.Estado b) {
		if (a.filas().size() != b.filas().size()) {
			return false;
		}
		for (int i = 0; i < a.filas().size(); i++) {
			if (!a.filas().get(i).id().equals(b.filas().get(i).id())) {
				return false;
			}
		}
		return true;
	}

	private void actualizarBotones() {
		for (Renglon renglon : renglones) {
			boolean puede = puedeComprar(renglon.fila());
			renglon.uno().active = puede;
			renglon.cinco().active = puede;
		}
	}

	private boolean puedeComprar(Paquetes.Fila fila) {
		return dibujado != null && !fila.enElTope() && dibujado.experiencia() >= dibujado.costo();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graficos, int mouseX, int mouseY, float delta) {
		graficos.fill(x0, y0, x0 + ANCHO, y0 + alto, ARGB.color(0xD0, 0x101010));
		graficos.outline(x0, y0, ANCHO, alto, ARGB.color(0xFF, 0x505050));

		graficos.centeredText(font, dibujado != null && dibujado.grupo() ? Component.literal("Estadísticas del grupo") : TITULO, width / 2, y0 + 6, BLANCO);
		if (dibujado == null) {
			graficos.centeredText(font, Component.literal("Esperando al servidor…"), width / 2, y0 + ALTO_CABECERA + 4, GRIS);
		} else {
			graficos.centeredText(font, Component.literal(resumen(dibujado)), width / 2, y0 + 17, GRIS);
			for (int i = 0; i < renglones.size(); i++) {
				extraerRenglon(graficos, renglones.get(i), i, mouseX, mouseY);
			}
		}

		if (mensaje != null) {
			int y = y0 + alto - ALTO_PIE + 3;
			String texto = font.plainSubstrByWidth(mensaje.texto(), ANCHO - 2 * MARGEN);
			graficos.centeredText(font, Component.literal(texto), width / 2, y, mensaje.compro() ? VERDE : ROJO);
		}

		// Los botones van arriba del panel
		super.extractRenderState(graficos, mouseX, mouseY, delta);
	}

	private void extraerRenglon(GuiGraphicsExtractor graficos, Renglon renglon, int indice, int mouseX, int mouseY) {
		Paquetes.Fila fila = renglon.fila();
		int y = renglon.y();
		int yTexto = y + (altoFila - 8) / 2;
		int xBotones = x0 + ANCHO - MARGEN - 2 * ANCHO_BOTON - 2;
		int xFinTexto = xBotones - MARGEN;

		if (indice % 2 == 0) {
			graficos.fill(x0 + 2, y, x0 + ANCHO - 2, y + altoFila, ARGB.color(0x30, 0xFFFFFF));
		}
		// El fondo de la fila se llena con los puntos: se ve de un vistazo qué falta subir
		int largo = xFinTexto - (x0 + X_NOMBRE - 2);
		int lleno = fila.tope() <= 0 ? largo : (int) ((long) largo * Math.min(fila.puntos(), fila.tope()) / fila.tope());
		graficos.fill(x0 + X_NOMBRE - 2, y + altoFila - 2, x0 + X_NOMBRE - 2 + lleno, y + altoFila - 1, fila.enElTope() ? DORADO : VERDE);

		graficos.item(icono(fila), x0 + MARGEN, y + (altoFila - 16) / 2);
		int colorNombre = fila.enElTope() ? DORADO : puedeComprar(fila) ? BLANCO : GRIS;
		graficos.text(font, font.plainSubstrByWidth(fila.nombre(), ANCHO_NOMBRE - 4), x0 + X_NOMBRE, yTexto, colorNombre);

		String puntos = fila.puntos() + "/" + fila.tope();
		int xPuntos = xFinTexto - font.width(puntos);
		graficos.text(font, puntos, xPuntos, yTexto, fila.enElTope() ? DORADO : GRIS);

		String ahora = fila.ahora().isEmpty() ? "—" : fila.ahora();
		int xAhora = x0 + X_NOMBRE + ANCHO_NOMBRE;
		graficos.text(font, font.plainSubstrByWidth(ahora, xPuntos - xAhora - 4), xAhora, yTexto, fila.ahora().isEmpty() ? GRIS_OSCURO : GRIS);

		if (mouseX >= x0 && mouseX < xBotones && mouseY >= y && mouseY < y + altoFila) {
			graficos.setComponentTooltipForNextFrame(font, detalle(fila), mouseX, mouseY);
		}
	}

	private List<Component> detalle(Paquetes.Fila fila) {
		List<Component> lineas = new ArrayList<>();
		lineas.add(Component.literal(fila.nombre()).withStyle(ChatFormatting.WHITE));
		lineas.add(Component.literal("Puntos: " + fila.puntos() + "/" + fila.tope()).withStyle(ChatFormatting.GRAY));
		if (!fila.ahora().isEmpty()) {
			lineas.add(Component.literal("Ahora: " + fila.ahora()).withStyle(ChatFormatting.GRAY));
		}
		if (fila.enElTope()) {
			lineas.add(Component.literal("Llegaste al máximo").withStyle(ChatFormatting.GOLD));
		} else if (dibujado != null) {
			lineas.add(Component.literal("Próximo punto: " + fila.proximo()).withStyle(ChatFormatting.AQUA));
			lineas.add(Component.literal("Cuesta " + dibujado.costo() + " de experiencia")
					.withStyle(dibujado.experiencia() >= dibujado.costo() ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		return lineas;
	}

	private static String resumen(Paquetes.Estado estado) {
		String quien = estado.grupo() ? "El grupo tiene " : "Tenés ";
		String texto = quien + estado.experiencia() + " de experiencia (nivel " + estado.nivel() + ")";
		if (estado.costo() <= 0) {
			return texto + " · los puntos son gratis";
		}
		int alcanzan = estado.experiencia() / estado.costo();
		return texto + " · cada punto cuesta " + estado.costo() + " · alcanza para " + alcanzan;
	}

	/** Una estadística de una versión más nueva del servidor no está en el enum de este cliente: se ve igual, con una barrera. */
	private static ItemStack icono(Paquetes.Fila fila) {
		return new ItemStack(Estadistica.porId(fila.id()).map(Estadistica::icono).orElse(Items.BARRIER));
	}

	@Override
	public boolean keyPressed(KeyEvent evento) {
		// Como el inventario: la misma tecla que la abre la cierra, y la E también
		if (EstadisticasRPGCliente.tecla().matches(evento) || minecraft.options.keyInventory.matches(evento)) {
			minecraft.gui.setScreen(null);
			return true;
		}
		return super.keyPressed(evento);
	}

	@Override
	public void onClose() {
		if (volverAlInventario && minecraft.player != null) {
			minecraft.gui.setScreen(new InventoryScreen(minecraft.player));
		} else {
			super.onClose();
		}
	}

	/** En singleplayer, pausar frenaría el servidor integrado: las compras no se contestarían hasta cerrar. */
	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
