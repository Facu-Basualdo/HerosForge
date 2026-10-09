package com.estadisticasrpg.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jspecify.annotations.Nullable;

import com.estadisticasrpg.Estadistica;
import com.estadisticasrpg.EstadisticasRPG;
import com.estadisticasrpg.MenuDeEstadisticas;
import com.estadisticasrpg.Mejoras;
import com.estadisticasrpg.PuntosDelJugador;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public class EstadisticasRPGGameTest {
	@GameTest
	public void modCargado(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded(EstadisticasRPG.MOD_ID), Component.literal("El mod tiene que estar cargado"));
		helper.succeed();
	}

	@GameTest
	public void comprarVidaCobraUnNivelYLlenaElCorazon(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "comprador");
		jugador.setExperienceLevels(5);

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.OK, "Resultado");
		helper.assertValueEqual(compra.comprados(), 1, "Puntos comprados");
		helper.assertValueEqual(jugador.experienceLevel, 4, "Niveles después de comprar");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 1, "Puntos de vida");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 22.0, "Vida máxima");
		helper.assertValueEqual(jugador.getHealth(), 22.0f, "El corazón nuevo aparece lleno");

		Mejoras.aplicar(jugador);
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 22.0, "Aplicar dos veces no apila");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void sinNivelesNoCambiaNada(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pobre");

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.SIN_NIVELES, "Resultado");
		helper.assertValueEqual(compra.comprados(), 0, "Puntos comprados");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 0, "Puntos de vida");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 20.0, "Vida máxima");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void enElTopeNoCobra(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "topeado");
		Mejoras.dar(jugador, Estadistica.VIDA, 10);
		jugador.setExperienceLevels(5);

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.TOPE, "Resultado");
		helper.assertValueEqual(compra.comprados(), 0, "Puntos comprados");
		helper.assertValueEqual(jugador.experienceLevel, 5, "No cobra niveles");
		helper.assertValueEqual(0, Mejoras.dar(jugador, Estadistica.VIDA, 3), "Dar tampoco pasa el tope");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void compraLoQueEntra(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "parcial");

		// Corta el tope: 7 de 10, pide 5
		Mejoras.dar(jugador, Estadistica.DANIO, 7);
		jugador.setExperienceLevels(30);
		Mejoras.Compra porTope = Mejoras.comprar(jugador, Estadistica.DANIO, 5);
		helper.assertValueEqual(porTope.comprados(), 3, "Con 3 de margen compra 3");
		helper.assertValueEqual(porTope.resultado(), Mejoras.Resultado.TOPE, "Lo cortó el tope");
		helper.assertValueEqual(jugador.experienceLevel, 27, "Cobra sólo los 3");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.ATTACK_DAMAGE), 6.0, "Daño: 1 de base + 10 × 0,5");

		// Cortan los niveles: margen 10, 2 niveles, pide 5
		jugador.setExperienceLevels(2);
		Mejoras.Compra porNiveles = Mejoras.comprar(jugador, Estadistica.ARMADURA, 5);
		helper.assertValueEqual(porNiveles.comprados(), 2, "Con 2 niveles compra 2");
		helper.assertValueEqual(porNiveles.resultado(), Mejoras.Resultado.SIN_NIVELES, "Lo cortaron los niveles");
		helper.assertValueEqual(jugador.experienceLevel, 0, "Gastó los 2 niveles");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.ARMOR), 2.0, "Armadura");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void velocidadEsPorcentajeDeLaBase(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "veloz");
		double base = jugador.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

		Mejoras.dar(jugador, Estadistica.VELOCIDAD, 10);
		double velocidad = jugador.getAttributeValue(Attributes.MOVEMENT_SPEED);
		helper.assertTrue(Math.abs(velocidad - base * 1.2) < 1e-9, "Velocidad +20 % (" + velocidad + ")");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void estadisticasDeLaFase3AlMaximo(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "completo");
		for (Estadistica estadistica : List.of(Estadistica.VELOCIDAD_ATAQUE, Estadistica.MINADO, Estadistica.ALCANCE, Estadistica.EMPUJE, Estadistica.SUERTE)) {
			Mejoras.dar(jugador, estadistica, 100);
		}

		// Con la mano vacía: 4 de base × 1,3
		assertCerca(helper, jugador.getAttributeValue(Attributes.ATTACK_SPEED), 5.2, "Velocidad de ataque +30 %");
		assertCerca(helper, jugador.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 1.5, "Minado +50 %");
		assertCerca(helper, jugador.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), 6.5, "Alcance a bloques: 4,5 + 2");
		assertCerca(helper, jugador.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), 5.0, "Alcance a entidades: 3 + 2");
		assertCerca(helper, jugador.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.5, "Resistencia al empuje");
		assertCerca(helper, jugador.getLuck(), 3.0, "Suerte (la que usa la pesca)");

		salir(helper, jugador);
		helper.succeed();
	}

	/** Si uno no se sincronizara, el cliente vanilla no vería el efecto: minaría, alcanzaría o caminaría como antes. */
	@GameTest
	public void losAtributosQueUsaElClienteSeSincronizan(GameTestHelper helper) {
		for (var atributo : List.of(Attributes.MOVEMENT_SPEED, Attributes.ATTACK_SPEED, Attributes.BLOCK_BREAK_SPEED,
				Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE)) {
			helper.assertTrue(atributo.value().isClientSyncable(), atributo.getRegisteredName() + " se manda al cliente");
		}
		helper.succeed();
	}

	@GameTest
	public void cadaEstadisticaTieneConfigYSlot(GameTestHelper helper) {
		Set<Integer> slots = new HashSet<>();
		for (Estadistica estadistica : Estadistica.values()) {
			helper.assertTrue(EstadisticasRPG.config().de(estadistica) != null, estadistica + " está en Configuracion.POR_DEFECTO");
			int slot = MenuDeEstadisticas.slotDe(estadistica);
			helper.assertTrue(slot >= 9 && slot < 18, estadistica + " está en la fila del medio (slot " + slot + ")");
			helper.assertTrue(slots.add(slot), estadistica + " no comparte slot con otra (slot " + slot + ")");
		}
		helper.succeed();
	}

	@GameTest
	public void alReaparecerConservaPuntosYVida(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "renacido");
		Mejoras.dar(jugador, Estadistica.VIDA, 10);
		PlayerList jugadores = helper.getLevel().getServer().getPlayerList();

		// Al morir, restoreFrom pierde los modificadores y deja la vida en el máximo sin los puntos
		ServerPlayer reaparecido = jugadores.respawn(jugador, false, Entity.RemovalReason.KILLED);
		helper.assertValueEqual(Mejoras.puntos(reaparecido).de(Estadistica.VIDA), 10, "Los puntos sobreviven a la muerte");
		helper.assertValueEqual(reaparecido.getAttributeValue(Attributes.MAX_HEALTH), 40.0, "La vida máxima sobrevive a la muerte");
		helper.assertValueEqual(reaparecido.getHealth(), 40.0f, "Reaparece con la vida máxima con los puntos");

		// Volver del End copia todo, incluida la vida actual
		reaparecido.setHealth(30);
		ServerPlayer delEnd = jugadores.respawn(reaparecido, true, Entity.RemovalReason.CHANGED_DIMENSION);
		helper.assertValueEqual(delEnd.getAttributeValue(Attributes.MAX_HEALTH), 40.0, "Volver del End no apila ni pierde");
		helper.assertValueEqual(delEnd.getHealth(), 30.0f, "Volver del End conserva la vida actual");

		salir(helper, delEnd);
		helper.succeed();
	}

	@GameTest
	public void guardarYCargarConservaPuntosYVida(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "guardado");
		Mejoras.dar(jugador, Estadistica.VIDA, 10);
		jugador.setHealth(33);

		TagValueOutput salida = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, jugador.registryAccess());
		jugador.saveWithoutId(salida);
		CompoundTag guardado = salida.buildResult();
		guardado.remove("UUID");
		salir(helper, jugador);

		ServerPlayer cargado = entrar(helper, "guardado-otra-vez", guardado);
		helper.assertValueEqual(Mejoras.puntos(cargado).de(Estadistica.VIDA), 10, "Los puntos se guardan");
		helper.assertValueEqual(cargado.getAttributeValue(Attributes.MAX_HEALTH), 40.0, "La vida máxima se guarda");
		// Si los atributos se leyeran después que Health, la vida quedaría cortada en 20
		helper.assertValueEqual(cargado.getHealth(), 33.0f, "La vida actual se guarda");

		salir(helper, cargado);
		helper.succeed();
	}

	@GameTest
	public void unaEstadisticaDesconocidaNoBorraLasDemas(GameTestHelper helper) {
		CompoundTag guardado = new CompoundTag();
		guardado.putInt("vida", 3);
		guardado.putInt("de-una-version-futura", 5);

		PuntosDelJugador puntos = PuntosDelJugador.CODEC.parse(NbtOps.INSTANCE, guardado).getOrThrow();
		helper.assertValueEqual(puntos.de(Estadistica.VIDA), 3, "La vida se lee igual");
		helper.succeed();
	}

	@GameTest
	public void reiniciarDejaTodoEnCero(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "reiniciado");
		Mejoras.dar(jugador, Estadistica.VIDA, 10);
		Mejoras.dar(jugador, Estadistica.VELOCIDAD, 4);
		helper.assertValueEqual(jugador.getHealth(), 40.0f, "Arranca con 40 de vida");

		Mejoras.reiniciar(jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador), PuntosDelJugador.vacio(), "Todos los puntos en 0");
		helper.assertTrue(!jugador.getAttribute(Attributes.MAX_HEALTH).hasModifier(Estadistica.VIDA.idModificador()), "Sin modificador de vida");
		helper.assertTrue(!jugador.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Estadistica.VELOCIDAD.idModificador()), "Sin modificador de velocidad");
		// En el momento, no en el próximo tick: los jugadores de prueba no tickean y vanilla recorta recién ahí
		helper.assertValueEqual(jugador.getHealth(), 20.0f, "La vida se corta al máximo sin puntos");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void comandos(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "comandante");
		jugador.setExperienceLevels(5);

		ejecutar(helper, jugador, "estadisticas subir vida 2");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 2, "subir compra");
		helper.assertValueEqual(jugador.experienceLevel, 3, "subir cobra");

		ejecutar(helper, jugador, "stats subir velocidad");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VELOCIDAD), 1, "El alias /stats funciona");
		helper.assertValueEqual(jugador.experienceLevel, 2, "El alias también cobra");

		ejecutar(helper, jugador, "estadisticas dar comandante vida 5");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 2, "dar es sólo para op");

		ejecutar(helper, jugador, "estadisticas subir inexistente");
		helper.assertValueEqual(jugador.experienceLevel, 2, "Una estadística que no existe no cobra");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void menuCompraConClick(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "cliqueador");
		jugador.setExperienceLevels(10);

		ejecutar(helper, jugador, "estadisticas");
		helper.assertTrue(jugador.containerMenu instanceof MenuDeEstadisticas, "/estadisticas abre el menú");
		AbstractContainerMenu menu = jugador.containerMenu;
		int vida = MenuDeEstadisticas.slotDe(Estadistica.VIDA);

		menu.clicked(vida, 0, ContainerInput.PICKUP, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 1, "Un click compra 1");
		helper.assertValueEqual(jugador.experienceLevel, 9, "Un click cobra 1 nivel");
		helper.assertTrue(menu.getCarried().isEmpty(), "El cursor queda vacío");
		helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), "El ícono sigue en su lugar");

		menu.clicked(vida, 0, ContainerInput.QUICK_MOVE, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 6, "Shift + click compra 5");
		helper.assertValueEqual(jugador.experienceLevel, 4, "Shift + click cobra 5 niveles");

		// Tecla numérica, tirar (Q) y click del medio no compran ni sacan el ítem
		for (ContainerInput entrada : List.of(ContainerInput.SWAP, ContainerInput.THROW, ContainerInput.CLONE)) {
			menu.clicked(vida, 0, entrada, jugador);
			helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 6, entrada + " no compra");
			helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), entrada + " no saca el ícono");
		}
		helper.assertTrue(jugador.getInventory().getItem(0).isEmpty(), "La tecla numérica no pasa el ícono a la barra");

		// Un op le da puntos con el menú abierto: se redibuja en el próximo broadcastChanges
		Mejoras.dar(jugador, Estadistica.VIDA, 10);
		menu.broadcastChanges();
		helper.assertTrue(menu.getSlot(vida).getItem().is(Items.STAINED_GLASS_PANE.gray()), "En el máximo se ve vidrio gris");

		jugador.closeContainer();
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void menuNoDejaSacarNiPonerItems(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "tramposo");
		MenuDeEstadisticas.abrir(jugador);
		AbstractContainerMenu menu = jugador.containerMenu;
		int vida = MenuDeEstadisticas.slotDe(Estadistica.VIDA);
		int primeroDelJugador = 27;

		// Doble click en el inventario con una copia exacta del ícono en el cursor: vanilla junta los ítems con los
		// mismos componentes de todos los slots (una manzana común no cuenta: le faltan el nombre y el lore)
		menu.setCarried(menu.getSlot(vida).getItem().copy());
		menu.clicked(primeroDelJugador, 0, ContainerInput.PICKUP_ALL, jugador);
		helper.assertValueEqual(menu.getCarried().getCount(), 1, "El doble click no junta la manzana del menú");
		helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), "La manzana del menú sigue ahí");

		// Arrastrar tierra por un slot del jugador, el de vida y otro del jugador
		menu.setCarried(new ItemStack(Items.DIRT, 2));
		menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_START, AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE), ContainerInput.QUICK_CRAFT, jugador);
		for (int slot : new int[] {primeroDelJugador + 1, vida, primeroDelJugador + 2}) {
			menu.clicked(slot, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_CONTINUE, AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE), ContainerInput.QUICK_CRAFT, jugador);
		}
		menu.clicked(-999, AbstractContainerMenu.getQuickcraftMask(AbstractContainerMenu.QUICKCRAFT_HEADER_END, AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE), ContainerInput.QUICK_CRAFT, jugador);
		helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), "Arrastrar no pone nada en el menú");
		helper.assertValueEqual(menu.getSlot(primeroDelJugador + 1).getItem().getCount() + menu.getSlot(primeroDelJugador + 2).getItem().getCount(), 2, "La tierra se reparte sólo en el inventario");
		helper.assertTrue(!menu.canDragTo(menu.getSlot(vida)) && !menu.getSlot(vida).mayPlace(new ItemStack(Items.DIRT)), "Los slots del menú no aceptan nada");

		// Shift + click desde el inventario no mete nada en el menú
		menu.clicked(primeroDelJugador + 1, 0, ContainerInput.QUICK_MOVE, jugador);
		helper.assertTrue(menu.getSlot(primeroDelJugador + 1).getItem().is(Items.DIRT), "Shift + click desde el inventario no mueve nada");

		jugador.closeContainer();
		salir(helper, jugador);
		helper.succeed();
	}

	private static void assertCerca(GameTestHelper helper, double actual, double esperado, String mensaje) {
		helper.assertTrue(Math.abs(actual - esperado) < 1e-9, mensaje + ": esperaba " + esperado + ", dio " + actual);
	}

	private static void ejecutar(GameTestHelper helper, ServerPlayer jugador, String comando) {
		helper.getLevel().getServer().getCommands().performPrefixedCommand(jugador.createCommandSourceStack(), comando);
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
