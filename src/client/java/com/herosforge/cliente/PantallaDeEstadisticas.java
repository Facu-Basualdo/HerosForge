package com.herosforge.cliente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.herosforge.Estadistica;
import com.herosforge.Paquetes;

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
 * La pantalla propia: una pestaña por rama y, de la rama elegida, una fila por estadística con sus puntos, lo
 * que da y los botones para bajar y comprar. No
 * decide nada: muestra el último {@link Paquetes.Estado} que mandó el servidor y le pide las compras y las bajas,
 * que el servidor valida, cobra y devuelve igual que desde el cofre.
 */
public class PantallaDeEstadisticas extends Screen {
	private static final int CON_CINCO = 5;
	// Dos columnas estables: a escala chica el efecto pasa al tooltip para conservar botones legibles.
	private static final int ANCHO = 620;
	private static final int ANCHO_MAXIMO_DOS_COLUMNAS = 620;
	private static final int ALTO_CABECERA = 72;
	private static final int Y_PESTANIAS = 46;
	private static final int ALTO_PESTANIA = 18;
	private static final int ALTO_FILA_MAXIMO = 30;
	private static final int ALTO_FILA_MINIMO = 16;
	private static final int ALTO_PIE = 38;
	private static final int ANCHO_BOTON = 20;
	private static final int BOTONES = 3;
	private static final int ANCHO_BOTONES = BOTONES * ANCHO_BOTON + (BOTONES - 1) * 2;
	private static final int MARGEN = 6;
	private static final int X_NOMBRE = 26;
	private static final int ANCHO_NOMBRE = 124;
	private static final int TICKS_DEL_MENSAJE = 100;

	private static final int BLANCO = ARGB.opaque(0xEEE6D5);
	private static final int GRIS = ARGB.opaque(0xA2ADB7);
	private static final int GRIS_OSCURO = ARGB.opaque(0x606060);
	private static final int VERDE = ARGB.opaque(0x8FCBA2);
	private static final int ROJO = ARGB.opaque(0xFF5555);
	private static final int DORADO = ARGB.opaque(0xD8B777);

	/** La rama que se estaba mirando: al volver a abrir la pantalla, sigue en la misma. */
	private static @Nullable String ramaElegida;

	private final boolean volverAlInventario;
	private final List<Renglon> renglones = new ArrayList<>();
	private Paquetes.@Nullable Estado dibujado;
	private int ancho = ANCHO;
	private int x0;
	private int y0;
	private int alto;
	private int altoFila = ALTO_FILA_MAXIMO;
	private Paquetes.@Nullable Resultado mensaje;
	private int ticksDelMensaje;

	/** {@code x} y {@code ancho} son los de su columna: con una sola, todo el panel. */
	private record Renglon(Paquetes.Fila fila, int x, int ancho, int y, Button menos, Button uno, Button cinco) {
		Renglon con(Paquetes.Fila nueva) {
			return new Renglon(nueva, x, ancho, y, menos, uno, cinco);
		}
	}

	/** @param volverAlInventario si se abrió desde el inventario: Esc vuelve ahí en vez de cerrar todo */
	public PantallaDeEstadisticas(boolean volverAlInventario) {
		super(IdiomaDelCliente.tr("herosforge.titulo"));
		this.volverAlInventario = volverAlInventario;
	}

	public static Component teclaDeAbrir() {
		return HerosForgeCliente.tecla().getTranslatedKeyMessage();
	}

	@Override
	protected void init() {
		renglones.clear();
		dibujado = HerosForgeCliente.estado();
		List<Paquetes.Fila> filas = dibujado == null ? List.of() : dibujado.filas();
		// Las ramas en el orden en que llegan: un servidor más nuevo puede mandar una que este cliente no conoce
		Map<String, List<Paquetes.Fila>> porRama = new LinkedHashMap<>();
		Map<String, String> nombres = new LinkedHashMap<>();
		for (Paquetes.Fila fila : filas) {
			porRama.computeIfAbsent(fila.rama(), rama -> new ArrayList<>()).add(fila);
			nombres.putIfAbsent(fila.rama(), fila.nombreDeLaRama());
		}
		if (!porRama.isEmpty() && (ramaElegida == null || !porRama.containsKey(ramaElegida))) {
			ramaElegida = porRama.keySet().iterator().next();
		}
		List<Paquetes.Fila> deLaRama = ramaElegida == null ? List.of() : porRama.getOrDefault(ramaElegida, List.of());
		// La primera mitad a la izquierda: con 9, quedan 5 y 4
		int mitad = (deLaRama.size() + 1) / 2;
		List<Paquetes.Fila> izquierda = deLaRama.subList(0, mitad);
		List<Paquetes.Fila> derecha = deLaRama.subList(mitad, deLaRama.size());

		int disponible = height - 4 - ALTO_CABECERA - ALTO_PIE;
		// El alto sale de la rama más larga, no de la elegida: si no, el panel cambia de tamaño con cada pestaña
		int porColumna = 1;
		for (List<Paquetes.Fila> rama : porRama.values()) {
			porColumna = Math.max(porColumna, (rama.size() + 1) / 2);
		}
		altoFila = Math.clamp(disponible / porColumna, ALTO_FILA_MINIMO, ALTO_FILA_MAXIMO);
		alto = ALTO_CABECERA + porColumna * altoFila + ALTO_PIE;
		// Se usa todo el ancho que haya: en 426 cada columna tiene apenas lo justo
		ancho = Math.min(width - 8, ANCHO_MAXIMO_DOS_COLUMNAS);
		x0 = (width - ancho) / 2;
		y0 = Math.max(2, (height - alto) / 2);

		agregarPestanias(nombres);
		int anchoColumna = (ancho - 12) / 2;
		agregarColumna(izquierda, x0 + 4, anchoColumna);
		agregarColumna(derecha, x0 + 8 + anchoColumna, anchoColumna);
		actualizarBotones();

		int yBotones = y0 + alto - 24;
		addRenderableWidget(new BotonForja(width / 2 - 104, yBotones, 100, 20, IdiomaDelCliente.texto("herosforge.pantalla.ver_cofre"), HerosForgeCliente::abrirCofre, false));
		addRenderableWidget(new BotonForja(width / 2 + 4, yBotones, 100, 20, IdiomaDelCliente.texto("herosforge.pantalla.listo"), this::onClose, true));
	}

	/** Una pestaña por rama, a lo ancho del panel. La elegida se dibuja como botón principal. */
	private void agregarPestanias(Map<String, String> nombres) {
		int cantidad = nombres.size();
		if (cantidad == 0) {
			return;
		}
		int separacion = 3;
		int anchoPestania = (ancho - 16 - (cantidad - 1) * separacion) / cantidad;
		int x = x0 + 8;
		for (Map.Entry<String, String> rama : nombres.entrySet()) {
			String id = rama.getKey();
			BotonForja pestania = new BotonForja(x, y0 + Y_PESTANIAS, anchoPestania, ALTO_PESTANIA, rama.getValue(), () -> elegir(id), id.equals(ramaElegida));
			pestania.setTooltip(Tooltip.create(IdiomaDelCliente.tr("herosforge.pantalla.ver_rama", rama.getValue())));
			addRenderableWidget(pestania);
			x += anchoPestania + separacion;
		}
	}

	private void elegir(String rama) {
		if (!rama.equals(ramaElegida)) {
			ramaElegida = rama;
			rebuildWidgets();
		}
	}

	private void agregarColumna(List<Paquetes.Fila> filas, int x, int ancho) {
		int y = y0 + ALTO_CABECERA;
		int xBotones = x + ancho - MARGEN - ANCHO_BOTONES;
		for (Paquetes.Fila fila : filas) {
			String id = fila.id();
			Button menos = addRenderableWidget(boton("-1", IdiomaDelCliente.texto("herosforge.pantalla.bajar"),
					() -> ClientPlayNetworking.send(new Paquetes.Bajar(id, 1)), xBotones, y + 1));
			Button uno = addRenderableWidget(boton("+1", IdiomaDelCliente.texto("herosforge.pantalla.comprar_uno"),
					() -> ClientPlayNetworking.send(new Paquetes.Comprar(id, 1)), xBotones + ANCHO_BOTON + 2, y + 1));
			Button cinco = addRenderableWidget(boton("+" + CON_CINCO, IdiomaDelCliente.texto("herosforge.pantalla.comprar_varios", CON_CINCO),
					() -> ClientPlayNetworking.send(new Paquetes.Comprar(id, CON_CINCO)), xBotones + 2 * (ANCHO_BOTON + 2), y + 1));
			renglones.add(new Renglon(fila, x, ancho, y, menos, uno, cinco));
			y += altoFila;
		}
	}

	private Button boton(String texto, String ayuda, Runnable pedido, int x, int y) {
		Button boton = new BotonForja(x, y + Math.max(0, (altoFila - 20) / 2), ANCHO_BOTON, Math.min(20, altoFila - 3), texto, () -> {
					if (HerosForgeCliente.servidorConElMod()) {
						pedido.run();
					}
				}, !texto.startsWith("-"));
		boton.setTooltip(Tooltip.create(Component.literal(ayuda)));
		return boton;
	}

	/** Conserva navegación, narración y clicks de vanilla. */
	private static final class BotonForja extends Button {
		private final boolean principal;

		BotonForja(int x, int y, int ancho, int alto, String texto, Runnable accion, boolean principal) {
			super(x, y, ancho, alto, Component.literal(texto), boton -> accion.run(), DEFAULT_NARRATION);
			this.principal = principal;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			boolean foco = active && isHoveredOrFocused();
			int borde = !active ? 0x34404A : foco ? 0xEEE6D5 : principal ? 0xA58A58 : 0x60717E;
			int fondo = !active ? 0x1C252E : foco ? 0x485647 : principal ? 0x354438 : 0x293540;
			g.fill(getX(), getY(), getRight(), getBottom(), ARGB.opaque(fondo));
			g.outline(getX(), getY(), getWidth(), getHeight(), ARGB.opaque(borde));
			// Recortado: en 426 de ancho, "Superviviente" no entra en su pestaña
			var font = net.minecraft.client.Minecraft.getInstance().font;
			String texto = getMessage().getString();
			if (font.width(texto) > getWidth() - 4) {
				texto = font.plainSubstrByWidth(texto, Math.max(0, getWidth() - 4 - font.width("…"))) + "…";
			}
			g.centeredText(font, Component.literal(texto), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, active ? BLANCO : GRIS_OSCURO);
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
		Paquetes.Estado actual = HerosForgeCliente.estado();
		if (actual == dibujado) {
			return;
		}
		// Con las mismas estadísticas en el mismo orden alcanza con los botones: rearmar todo le saca el foco al
		// que navega con el teclado, y la experiencia cambia con cada orbe
		if (actual != null && dibujado != null && mismasEstadisticas(actual, dibujado)) {
			dibujado = actual;
			for (int i = 0; i < renglones.size(); i++) {
				Renglon viejo = renglones.get(i);
				renglones.set(i, viejo.con(fila(actual, viejo.fila().id())));
			}
			actualizarBotones();
		} else {
			rebuildWidgets();
		}
	}

	/** Los renglones son sólo los de la rama elegida: se buscan por id. */
	private static Paquetes.Fila fila(Paquetes.Estado estado, String id) {
		for (Paquetes.Fila fila : estado.filas()) {
			if (fila.id().equals(id)) {
				return fila;
			}
		}
		throw new IllegalStateException("No está la estadística " + id);
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
			renglon.menos().active = renglon.fila().puntos() > 0;
		}
	}

	private boolean puedeComprar(Paquetes.Fila fila) {
		return dibujado != null && !fila.enElTope() && dibujado.experiencia() >= dibujado.costo();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graficos, int mouseX, int mouseY, float delta) {
		graficos.fill(x0 + 3, y0 + 3, x0 + ancho + 3, y0 + alto + 3, ARGB.color(0x80, 0x000000));
		graficos.fill(x0, y0, x0 + ancho, y0 + alto, ARGB.color(0xF5, 0x17212B));
		graficos.outline(x0, y0, ancho, alto, ARGB.opaque(0x8C7652));
		graficos.fill(x0 + 1, y0 + 1, x0 + ancho - 1, y0 + 25, ARGB.opaque(0x24313C));
		graficos.text(font, "HERO'S FORGE", x0 + 10, y0 + 9, DORADO);
		String titulo = IdiomaDelCliente.texto(dibujado != null && dibujado.grupo() ? "herosforge.pantalla.titulo_grupo" : "herosforge.titulo");
		graficos.text(font, titulo, x0 + ancho - 10 - font.width(titulo), y0 + 9, BLANCO);
		if (dibujado == null) {
			graficos.centeredText(font, IdiomaDelCliente.tr("herosforge.pantalla.esperando"), width / 2, y0 + 35, GRIS);
		} else {
			int mitad = ancho / 2;
			String resumen = ajustar(resumen(dibujado), mitad - 20);
			graficos.text(font, resumen, x0 + 10, y0 + 32, VERDE);
			// Centrado en el panel, salvo que la experiencia llegue hasta ahí (millones de XP en 426 de ancho): ahí se corre
			String costo = ajustar(dibujado.costo() == 0
					? IdiomaDelCliente.texto("herosforge.pantalla.gratis")
					: IdiomaDelCliente.texto("herosforge.pantalla.costo", numero(dibujado.costo())), mitad - 20);
			int xCosto = Math.max(x0 + mitad - font.width(costo) / 2, x0 + 10 + font.width(resumen) + 12);
			graficos.text(font, costo, xCosto, y0 + 32, BLANCO);
			graficos.fill(x0 + 8, y0 + 43, x0 + ancho - 8, y0 + 44, ARGB.opaque(0x394653));
			for (Renglon renglon : renglones) extraerRenglon(graficos, renglon, mouseX, mouseY);
		}

		if (mensaje != null) {
			int y = y0 + alto - ALTO_PIE + 3;
			String texto = font.plainSubstrByWidth(mensaje.texto(), ancho - 2 * MARGEN);
			graficos.centeredText(font, Component.literal(texto), width / 2, y, mensaje.compro() ? VERDE : ROJO);
		}

		// Los botones van arriba del panel
		super.extractRenderState(graficos, mouseX, mouseY, delta);
	}

	private void extraerRenglon(GuiGraphicsExtractor g, Renglon renglon, int mouseX, int mouseY) {
		Paquetes.Fila fila = renglon.fila();
		int x = renglon.x(), y = renglon.y();
		int xBotones = x + renglon.ancho() - MARGEN - ANCHO_BOTONES;
		boolean amplio = altoFila >= 26 && renglon.ancho() >= 260;
		boolean hover = mouseX >= x && mouseX < x + renglon.ancho() && mouseY >= y && mouseY < y + altoFila;
		g.fill(x, y, x + renglon.ancho(), y + altoFila - 2, ARGB.opaque(hover ? 0x303E49 : 0x202D38));
		g.item(icono(fila), x + 4, y + (altoFila - 16) / 2 - 1);
		String puntos = amplio ? fila.puntos() + "/" + fila.tope() : Integer.toString(fila.puntos());
		int xPuntos = xBotones - 6 - font.width(puntos);
		String nombre = Estadistica.porId(fila.id()).map(estadistica -> estadistica.nombreCorto(IdiomaDelCliente.idioma())).orElse(fila.nombre());
		int yTexto = amplio ? y + 4 : y + (altoFila - 8) / 2 - 1;
		g.text(font, ajustar(nombre, xPuntos - x - X_NOMBRE - 5), x + X_NOMBRE, yTexto, BLANCO);
		g.text(font, puntos, xPuntos, yTexto, fila.enElTope() ? DORADO : GRIS);
		if (amplio) {
			g.text(font, ajustar(fila.ahora().isEmpty() ? IdiomaDelCliente.texto("herosforge.pantalla.sin_mejoras") : fila.ahora(), xBotones - x - X_NOMBRE - 6),
					x + X_NOMBRE, y + 15, GRIS);
		}
		int largo = xBotones - x - X_NOMBRE - 6;
		int lleno = fila.tope() <= 0 ? 0 : (int) ((long) largo * Math.min(fila.puntos(), fila.tope()) / fila.tope());
		g.fill(x + X_NOMBRE, y + altoFila - 4, x + X_NOMBRE + largo, y + altoFila - 3, ARGB.opaque(0x3A4854));
		g.fill(x + X_NOMBRE, y + altoFila - 4, x + X_NOMBRE + lleno, y + altoFila - 3, fila.enElTope() ? DORADO : VERDE);
		if (hover && mouseX < xBotones) g.setComponentTooltipForNextFrame(font, detalle(fila), mouseX, mouseY);
	}

	private String ajustar(String texto, int anchoDisponible) {
		if (font.width(texto) <= anchoDisponible) return texto;
		return font.plainSubstrByWidth(texto, Math.max(0, anchoDisponible - font.width("…"))) + "…";
	}

	private List<Component> detalle(Paquetes.Fila fila) {
		List<Component> lineas = new ArrayList<>();
		lineas.add(Component.literal(fila.nombre()).withStyle(ChatFormatting.WHITE));
		lineas.add(IdiomaDelCliente.tr("herosforge.menu.puntos", fila.puntos(), fila.tope()).withStyle(ChatFormatting.GRAY));
		if (!fila.ahora().isEmpty()) {
			lineas.add(IdiomaDelCliente.tr("herosforge.menu.ahora", fila.ahora()).withStyle(ChatFormatting.GRAY));
		}
		if (fila.enElTope()) {
			lineas.add(IdiomaDelCliente.tr("herosforge.pantalla.en_el_tope").withStyle(ChatFormatting.GOLD));
		} else if (dibujado != null) {
			lineas.add(IdiomaDelCliente.tr("herosforge.menu.proximo", fila.proximo()).withStyle(ChatFormatting.AQUA));
			lineas.add(IdiomaDelCliente.tr("herosforge.menu.cuesta", IdiomaDelCliente.texto("herosforge.experiencia", dibujado.costo()))
					.withStyle(dibujado.experiencia() >= dibujado.costo() ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		if (fila.puntos() > 0 && dibujado != null) {
			lineas.add(IdiomaDelCliente.tr("herosforge.pantalla.devuelve", IdiomaDelCliente.texto("herosforge.experiencia", dibujado.devolucion()))
					.withStyle(ChatFormatting.DARK_GRAY));
		}
		return lineas;
	}

	/** Con separador de miles: 12.345 en español, 12,345 en inglés. */
	private static String numero(int valor) {
		boolean espanol = IdiomaDelCliente.idioma().startsWith("es");
		return String.format(java.util.Locale.forLanguageTag(espanol ? "es-AR" : "en-US"), "%,d", valor);
	}

	private static String resumen(Paquetes.Estado estado) {
		return IdiomaDelCliente.texto("herosforge.pantalla.resumen", numero(estado.experiencia()), numero(estado.nivel()));
	}

	/** Una estadística de una versión más nueva del servidor no está en el enum de este cliente: se ve igual, con una barrera. */
	private static ItemStack icono(Paquetes.Fila fila) {
		return new ItemStack(Estadistica.porId(fila.id()).map(Estadistica::icono).orElse(Items.BARRIER));
	}

	@Override
	public boolean keyPressed(KeyEvent evento) {
		// Como el inventario: la misma tecla que la abre la cierra, y la E también
		if (HerosForgeCliente.tecla().matches(evento) || minecraft.options.keyInventory.matches(evento)) {
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
