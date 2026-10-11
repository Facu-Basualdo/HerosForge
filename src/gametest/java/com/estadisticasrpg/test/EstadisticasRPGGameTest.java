package com.estadisticasrpg.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jspecify.annotations.Nullable;

import com.estadisticasrpg.ArchivoDeConfiguracion;
import com.estadisticasrpg.Configuracion;
import com.estadisticasrpg.Curacion;
import com.estadisticasrpg.Estadistica;
import com.estadisticasrpg.EstadisticasRPG;
import com.estadisticasrpg.InterfazDelCliente;
import com.estadisticasrpg.MenuDeEstadisticas;
import com.estadisticasrpg.Mejoras;
import com.estadisticasrpg.Paquetes;
import com.estadisticasrpg.PuntosDelGrupo;
import com.estadisticasrpg.PuntosDelJugador;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public class EstadisticasRPGGameTest {
	private static final int COSTO = Configuracion.POR_DEFECTO.experienciaPorPunto();
	private static final int DEVOLUCION = Configuracion.POR_DEFECTO.devolucionPorPunto();

	@GameTest
	public void modCargado(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded(EstadisticasRPG.MOD_ID), Component.literal("El mod tiene que estar cargado"));
		helper.succeed();
	}

	@GameTest
	public void comprarVidaCobraLaExperienciaYLlenaLaVida(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "comprador");
		ponerExperiencia(jugador, COSTO * 3 + 10);

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.OK, "Resultado");
		helper.assertValueEqual(compra.comprados(), 1, "Puntos comprados");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 2 + 10, "Experiencia después de comprar");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 1, "Puntos de vida");
		assertCerca(helper, jugador.getAttributeValue(Attributes.MAX_HEALTH), 20.6, "Vida máxima");
		assertCerca(helper, jugador.getHealth(), 20.6, "La vida nueva aparece llena");

		Mejoras.aplicar(jugador);
		assertCerca(helper, jugador.getAttributeValue(Attributes.MAX_HEALTH), 20.6, "Aplicar dos veces no apila");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void sinExperienciaNoCambiaNada(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pobre");
		ponerExperiencia(jugador, COSTO - 1);

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.SIN_EXPERIENCIA, "Resultado");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO - 1, "No cobra nada");
		helper.assertValueEqual(compra.comprados(), 0, "Puntos comprados");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 0, "Puntos de vida");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 20.0, "Vida máxima");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void enElTopeNoCobra(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "topeado");
		Mejoras.dar(jugador, Estadistica.VIDA, 100);
		ponerExperiencia(jugador, COSTO * 5);

		Mejoras.Compra compra = Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.TOPE, "Resultado");
		helper.assertValueEqual(compra.comprados(), 0, "Puntos comprados");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 5, "No cobra experiencia");
		helper.assertValueEqual(0, Mejoras.dar(jugador, Estadistica.VIDA, 3), "Dar tampoco pasa el tope");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void compraLoQueEntra(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "parcial");

		// Corta el tope: 97 de 100, pide 5
		Mejoras.dar(jugador, Estadistica.DANIO, 97);
		ponerExperiencia(jugador, COSTO * 10);
		Mejoras.Compra porTope = Mejoras.comprar(jugador, Estadistica.DANIO, 5);
		helper.assertValueEqual(porTope.comprados(), 3, "Con 3 de margen compra 3");
		helper.assertValueEqual(porTope.resultado(), Mejoras.Resultado.TOPE, "Lo cortó el tope");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 7, "Cobra sólo los 3");
		assertCerca(helper, jugador.getAttributeValue(Attributes.ATTACK_DAMAGE), 26.0, "Daño: 1 de base + 100 × 0,25");

		// Corta la experiencia: margen 100, alcanza para 2 y sobra un poco, pide 5
		ponerExperiencia(jugador, COSTO * 2 + 5);
		Mejoras.Compra porExperiencia = Mejoras.comprar(jugador, Estadistica.ARMADURA, 5);
		helper.assertValueEqual(porExperiencia.comprados(), 2, "Alcanza para 2");
		helper.assertValueEqual(porExperiencia.resultado(), Mejoras.Resultado.SIN_EXPERIENCIA, "Lo cortó la experiencia");
		helper.assertValueEqual(Mejoras.experiencia(jugador), 5, "Queda lo que sobraba");
		assertCerca(helper, jugador.getAttributeValue(Attributes.ARMOR), 0.16, "Armadura");

		salir(helper, jugador);
		helper.succeed();
	}

	/** La experiencia de cada nivel la da vanilla: si la cuenta de Mejoras.experiencia se corriera, se cobraría mal. */
	@GameTest
	public void elCostoEsElMismoEnCualquierNivel(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "veterano");
		// Los totales de vanilla en los cambios de fórmula (nivel 16 y 31)
		for (int[] nivelYTotal : new int[][] {{15, 315}, {16, 352}, {17, 394}, {30, 1395}, {31, 1507}, {32, 1628}}) {
			jugador.setExperienceLevels(nivelYTotal[0]);
			jugador.setExperiencePoints(0);
			helper.assertValueEqual(Mejoras.experiencia(jugador), nivelYTotal[1], "Experiencia en el nivel " + nivelYTotal[0]);
		}

		// En el nivel 30 un nivel vale 112, pero un punto cuesta lo mismo que en el nivel 0
		jugador.setExperienceLevels(30);
		jugador.setExperiencePoints(0);
		Mejoras.comprar(jugador, Estadistica.VIDA, 5);
		helper.assertValueEqual(Mejoras.experiencia(jugador), 1395 - COSTO * 5, "En el nivel 30, 5 puntos cuestan 5 × el costo");
		helper.assertTrue(jugador.experienceLevel < 30, "Bajar la experiencia baja de nivel cuando hace falta");

		ponerExperiencia(jugador, COSTO);
		Mejoras.comprar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(Mejoras.experiencia(jugador), 0, "Con lo justo para uno, queda en 0");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 6, "Compró los 6");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void velocidadEsPorcentajeDeLaBase(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "veloz");
		double base = jugador.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);

		Mejoras.dar(jugador, Estadistica.VELOCIDAD, 100);
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
		assertCerca(helper, jugador.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), 2.0, "Minado +100 %");
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
				Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE, Attributes.JUMP_STRENGTH)) {
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
			// La fila de arriba es de la botella de experiencia
			helper.assertTrue(estadistica.combate() ? slot >= 9 && slot < 18 : slot >= 18 && slot < 27,
					estadistica + " está en su fila: combate en la del medio, el resto abajo (slot " + slot + ")");
			helper.assertTrue(slots.add(slot), estadistica + " no comparte slot con otra (slot " + slot + ")");
		}
		helper.succeed();
	}

	@GameTest
	public void bajarDevuelveLaMitad(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "arrepentido");
		ponerExperiencia(jugador, COSTO * 3);
		Mejoras.comprar(jugador, Estadistica.VIDA, 3);
		helper.assertValueEqual(Mejoras.experiencia(jugador), 0, "Gastó todo");

		Mejoras.Devolucion una = Mejoras.bajar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(una.bajados(), 1, "Baja 1");
		helper.assertValueEqual(una.experiencia(), COSTO / 2, "Devuelve la mitad de lo que costó");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO / 2, "La experiencia vuelve al jugador");
		assertCerca(helper, jugador.getAttributeValue(Attributes.MAX_HEALTH), 21.2, "La vida máxima baja con el punto");

		Mejoras.Devolucion resto = Mejoras.bajar(jugador, Estadistica.VIDA, 5);
		helper.assertValueEqual(resto.bajados(), 2, "Pidiendo 5, baja los 2 que quedan");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO / 2 * 3, "Y devuelve sólo por esos 2");
		helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 20.0, "Sin puntos, la vida de vanilla");
		helper.assertTrue(jugador.getHealth() <= 20, "La vida no queda por encima del máximo nuevo");
		helper.assertValueEqual(Mejoras.mensaje(resto).getString(), "Vida −2 puntos: te devolvió 30 de experiencia. No tenía los 5", "El mensaje");

		Mejoras.Devolucion nada = Mejoras.bajar(jugador, Estadistica.VIDA, 1);
		helper.assertValueEqual(nada.bajados(), 0, "Sin puntos no baja nada");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO / 2 * 3, "Ni devuelve");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void estadisticasNuevasAlMaximo(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "atleta");
		for (Estadistica estadistica : List.of(Estadistica.SALTO, Estadistica.CAIDA, Estadistica.RESPIRACION)) {
			Mejoras.dar(jugador, estadistica, 100);
		}
		assertCerca(helper, jugador.getAttributeValue(Attributes.JUMP_STRENGTH), 0.42 * 1.2, "Salto +20 %");
		assertCerca(helper, jugador.getAttributeValue(Attributes.SAFE_FALL_DISTANCE), 13, "Caída segura: 3 + 10");
		assertCerca(helper, jugador.getAttributeValue(Attributes.OXYGEN_BONUS), 3, "Respiración +3, como Respiración III");
		helper.assertValueEqual(Estadistica.REGENERACION.describir(0.3), "+9 corazones por minuto", "La regeneración se lee por minuto");
		helper.assertValueEqual(Estadistica.ROBO_DE_VIDA.describir(0.1), "+10 % del daño que hacés", "El robo de vida en porcentaje");

		salir(helper, jugador);
		helper.succeed();
	}

	/** Se llama a regenerar a mano en el mismo tick: el tick del servidor también cura cada segundo y movería la cuenta. */
	@GameTest
	public void regeneracionCuraCadaSegundo(GameTestHelper helper) {
		ServerPlayer conPuntos = entrar(helper, "regenerado");
		ServerPlayer sinPuntos = entrar(helper, "herido");
		Mejoras.dar(conPuntos, Estadistica.REGENERACION, 100);
		conPuntos.setHealth(10);
		sinPuntos.setHealth(10);

		Curacion.regenerar(helper.getLevel().getServer());
		assertCerca(helper, conPuntos.getHealth(), 10.3, "Con 100 puntos cura 0,3 por segundo");
		assertCerca(helper, sinPuntos.getHealth(), 10, "Sin puntos no cura");

		conPuntos.setHealth(conPuntos.getMaxHealth());
		Curacion.regenerar(helper.getLevel().getServer());
		assertCerca(helper, conPuntos.getHealth(), conPuntos.getMaxHealth(), "Con la vida llena no pasa del máximo");

		salir(helper, conPuntos);
		salir(helper, sinPuntos);
		helper.succeed();
	}

	@GameTest
	public void roboDeVidaCuraAlPegar(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "vampiro");
		Mejoras.dar(jugador, Estadistica.ROBO_DE_VIDA, 100);
		jugador.setHealth(10);
		IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));

		blanco.hurtServer(helper.getLevel(), jugador.damageSources().playerAttack(jugador), 8);
		assertCerca(helper, blanco.getMaxHealth() - blanco.getHealth(), 8, "El golpe pega entero (el gólem no tiene armadura)");
		assertCerca(helper, jugador.getHealth(), 10.8, "Con 100 puntos cura el 10 % del daño");

		// Un golpe que no es del jugador no lo cura
		IronGolem otro = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(4, 2, 2));
		otro.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 8);
		assertCerca(helper, jugador.getHealth(), 10.8, "Un golpe de otro no cura al jugador");

		blanco.discard();
		otro.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	/** El mixin se aplica recién en runtime: esto dispara flechas y tridentes de verdad contra un blanco. */
	@GameTest
	public void danioADistanciaMultiplicaFlechasYTridentes(GameTestHelper helper) {
		ServerPlayer sinPuntos = entrar(helper, "arquero-novato");
		ServerPlayer conPuntos = entrar(helper, "arquero-experto");
		Mejoras.dar(conPuntos, Estadistica.DANIO_DISTANCIA, 100);
		ServerLevel level = helper.getLevel();

		float flecha = danioAlBlanco(helper, new Arrow(level, sinPuntos, new ItemStack(Items.ARROW), new ItemStack(Items.BOW)));
		helper.assertTrue(flecha > 0, "La flecha sin puntos pega (" + flecha + ")");
		assertCerca(helper, danioAlBlanco(helper, new Arrow(level, conPuntos, new ItemStack(Items.ARROW), new ItemStack(Items.BOW))), flecha * 2, "Con 100 puntos, la flecha pega el doble");
		assertCerca(helper, danioAlBlanco(helper, new Arrow(level, 0, 0, 0, new ItemStack(Items.ARROW), new ItemStack(Items.BOW))), flecha, "Sin dueño, el daño de vanilla");

		float tridente = danioAlBlanco(helper, new ThrownTrident(level, sinPuntos, new ItemStack(Items.TRIDENT)));
		helper.assertTrue(tridente > 0, "El tridente sin puntos pega (" + tridente + ")");
		assertCerca(helper, danioAlBlanco(helper, new ThrownTrident(level, conPuntos, new ItemStack(Items.TRIDENT))), tridente * 2, "Con 100 puntos, el tridente pega el doble");

		salir(helper, sinPuntos);
		salir(helper, conPuntos);
		helper.succeed();
	}

	@GameTest
	public void multiplicadorADistanciaSegunLosPuntos(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "punteria");
		assertCerca(helper, Mejoras.multiplicadorADistancia(null), 1, "Sin dueño");
		assertCerca(helper, Mejoras.multiplicadorADistancia(jugador), 1, "Sin puntos");
		Mejoras.dar(jugador, Estadistica.DANIO_DISTANCIA, 50);
		assertCerca(helper, Mejoras.multiplicadorADistancia(jugador), 1.5, "Con 50 puntos, +50 %");
		Mejoras.dar(jugador, Estadistica.DANIO_DISTANCIA, 80);
		assertCerca(helper, Mejoras.multiplicadorADistancia(jugador), 2, "El tope es 100: +100 %");
		helper.assertTrue(!jugador.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(Estadistica.DANIO_DISTANCIA.idModificador()), "No toca el daño cuerpo a cuerpo");

		ServerPlayer otro = entrar(helper, "punteria-grupo");
		conPuntosDelGrupo(helper, () -> {
			Mejoras.dar(jugador, Estadistica.DANIO_DISTANCIA, 20);
			assertCerca(helper, Mejoras.multiplicadorADistancia(otro), 1.2, "Con los puntos del grupo, vale para todos");
		});
		salir(helper, jugador);
		salir(helper, otro);
		helper.succeed();
	}

	/**
	 * Un gólem de hierro quieto (sin armadura, que no es lineal, y con vida de sobra) y el proyectil a 2 bloques,
	 * tirado hacia él. El proyectil no se agrega al mundo: se tickea a mano, así cada medición es una sola.
	 */
	private static float danioAlBlanco(GameTestHelper helper, AbstractArrow proyectil) {
		IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));
		proyectil.setCritArrow(false);
		proyectil.setPos(blanco.getX() - 2, blanco.getY() + 1.3, blanco.getZ());
		proyectil.shoot(1, 0, 0, 1.5f, 0);
		for (int tick = 0; tick < 5 && blanco.getHealth() == blanco.getMaxHealth(); tick++) {
			proyectil.tick();
		}
		float danio = blanco.getMaxHealth() - blanco.getHealth();
		blanco.discard();
		proyectil.discard();
		return danio;
	}

	@GameTest
	public void alReaparecerConservaPuntosYVida(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "renacido");
		Mejoras.dar(jugador, Estadistica.VIDA, 50);
		PlayerList jugadores = helper.getLevel().getServer().getPlayerList();

		// Al morir, restoreFrom pierde los modificadores y deja la vida en el máximo sin los puntos
		ServerPlayer reaparecido = jugadores.respawn(jugador, false, Entity.RemovalReason.KILLED);
		helper.assertValueEqual(Mejoras.puntos(reaparecido).de(Estadistica.VIDA), 50, "Los puntos sobreviven a la muerte");
		assertCerca(helper, reaparecido.getAttributeValue(Attributes.MAX_HEALTH), 50.0, "La vida máxima sobrevive a la muerte");
		assertCerca(helper, reaparecido.getHealth(), 50.0, "Reaparece con la vida máxima con los puntos");

		// Volver del End copia todo, incluida la vida actual
		reaparecido.setHealth(30);
		ServerPlayer delEnd = jugadores.respawn(reaparecido, true, Entity.RemovalReason.CHANGED_DIMENSION);
		assertCerca(helper, delEnd.getAttributeValue(Attributes.MAX_HEALTH), 50.0, "Volver del End no apila ni pierde");
		helper.assertValueEqual(delEnd.getHealth(), 30.0f, "Volver del End conserva la vida actual");

		salir(helper, delEnd);
		helper.succeed();
	}

	@GameTest
	public void guardarYCargarConservaPuntosYVida(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "guardado");
		Mejoras.dar(jugador, Estadistica.VIDA, 50);
		jugador.setHealth(33);

		TagValueOutput salida = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, jugador.registryAccess());
		jugador.saveWithoutId(salida);
		CompoundTag guardado = salida.buildResult();
		guardado.remove("UUID");
		salir(helper, jugador);

		ServerPlayer cargado = entrar(helper, "guardado-otra-vez", guardado);
		helper.assertValueEqual(Mejoras.puntos(cargado).de(Estadistica.VIDA), 50, "Los puntos se guardan");
		assertCerca(helper, cargado.getAttributeValue(Attributes.MAX_HEALTH), 50.0, "La vida máxima se guarda");
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
		Mejoras.dar(jugador, Estadistica.VIDA, 50);
		Mejoras.dar(jugador, Estadistica.VELOCIDAD, 4);
		assertCerca(helper, jugador.getHealth(), 50.0, "Arranca con 50 de vida");

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
		ponerExperiencia(jugador, COSTO * 5);

		ejecutar(helper, jugador, "estadisticas subir vida 2");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 2, "subir compra");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 3, "subir cobra");

		ejecutar(helper, jugador, "stats subir velocidad");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VELOCIDAD), 1, "El alias /stats funciona");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 2, "El alias también cobra");

		ejecutar(helper, jugador, "estadisticas bajar velocidad");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VELOCIDAD), 0, "bajar baja");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 2 + DEVOLUCION, "bajar devuelve la mitad");
		ejecutar(helper, jugador, "stats bajar velocidad 3");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 2 + DEVOLUCION, "Sin puntos, bajar no devuelve nada");
		ponerExperiencia(jugador, COSTO * 2);

		ejecutar(helper, jugador, "estadisticas dar comandante vida 5");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 2, "dar es sólo para op");

		ejecutar(helper, jugador, "estadisticas subir inexistente");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 2, "Una estadística que no existe no cobra");

		// Desde la consola (op): dar funciona, y recargar lee el archivo del servidor de prueba sin romper nada
		MinecraftServer servidor = helper.getLevel().getServer();
		servidor.getCommands().performPrefixedCommand(servidor.createCommandSourceStack(), "estadisticas dar comandante vida 5");
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 7, "dar desde la consola");
		servidor.getCommands().performPrefixedCommand(servidor.createCommandSourceStack(), "estadisticas recargar");
		assertCerca(helper, jugador.getAttributeValue(Attributes.MAX_HEALTH), 24.2, "Después de recargar, los puntos siguen aplicados");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void menuCompraConClick(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "cliqueador");
		ponerExperiencia(jugador, COSTO * 10);

		ejecutar(helper, jugador, "estadisticas");
		helper.assertTrue(jugador.containerMenu instanceof MenuDeEstadisticas, "/estadisticas abre el menú");
		AbstractContainerMenu menu = jugador.containerMenu;
		int vida = MenuDeEstadisticas.slotDe(Estadistica.VIDA);

		menu.clicked(vida, 0, ContainerInput.PICKUP, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 1, "Un click compra 1");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 9, "Un click cobra 1 punto");
		helper.assertTrue(menu.getCarried().isEmpty(), "El cursor queda vacío");
		helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), "El ícono sigue en su lugar");

		menu.clicked(vida, 0, ContainerInput.QUICK_MOVE, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 6, "Shift + click compra 5");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 4, "Shift + click cobra 5 puntos");

		menu.clicked(vida, 1, ContainerInput.PICKUP, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 5, "Click derecho baja 1");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 4 + DEVOLUCION, "Click derecho devuelve la mitad");
		menu.clicked(vida, 1, ContainerInput.QUICK_MOVE, jugador);
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 0, "Shift + click derecho baja 5");
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 4 + DEVOLUCION * 6, "Y devuelve la mitad de los 5");
		helper.assertTrue(menu.getCarried().isEmpty(), "Bajar tampoco deja nada en el cursor");
		menu.clicked(vida, 0, ContainerInput.PICKUP, jugador);

		// Tecla numérica, tirar (Q) y click del medio no compran ni sacan el ítem
		for (ContainerInput entrada : List.of(ContainerInput.SWAP, ContainerInput.THROW, ContainerInput.CLONE)) {
			menu.clicked(vida, 0, entrada, jugador);
			helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 1, entrada + " no compra");
			helper.assertTrue(menu.getSlot(vida).getItem().is(Items.GOLDEN_APPLE), entrada + " no saca el ícono");
		}
		helper.assertTrue(jugador.getInventory().getItem(0).isEmpty(), "La tecla numérica no pasa el ícono a la barra");

		// Un op le da puntos con el menú abierto: se redibuja en el próximo broadcastChanges
		Mejoras.dar(jugador, Estadistica.VIDA, 100);
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

	@GameTest
	public void configNuevaSeCreaConLosValoresPorDefecto(GameTestHelper helper) throws IOException {
		Path archivo = Files.createTempDirectory("estadisticasrpg").resolve("estadisticasrpg.json");

		ArchivoDeConfiguracion.Lectura creada = ArchivoDeConfiguracion.cargar(archivo);
		helper.assertTrue(Files.exists(archivo), "Se crea el archivo");
		helper.assertValueEqual(creada.config(), Configuracion.POR_DEFECTO, "Con los valores por defecto");

		ArchivoDeConfiguracion.Lectura releida = ArchivoDeConfiguracion.cargar(archivo);
		helper.assertValueEqual(releida.config(), Configuracion.POR_DEFECTO, "Leer lo que se escribió da lo mismo");
		helper.assertValueEqual(releida.avisos(), List.of(), "Sin avisos");
		helper.succeed();
	}

	@GameTest
	public void configRespetaLoCambiadoYAvisaLoInvalido(GameTestHelper helper) throws IOException {
		Path archivo = Files.createTempDirectory("estadisticasrpg").resolve("estadisticasrpg.json");
		String escrito = """
				{
				  "experienciaPorPunto": 25,
				  "devolucionAlBajar": 1.5,
				  "estadisticas": {
				    "vida": { "porPunto": 4, "tope": 5 },
				    "danio": { "porPunto": -1, "tope": 3 },
				    "armadura": { "tope": "mucho" },
				    "volar": { "porPunto": 1, "tope": 1 }
				  }
				}
				""";
		Files.writeString(archivo, escrito);

		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivo);
		Configuracion config = lectura.config();
		helper.assertValueEqual(config.experienciaPorPunto(), 25, "experienciaPorPunto");
		helper.assertValueEqual(config.devolucionAlBajar(), Configuracion.POR_DEFECTO.devolucionAlBajar(), "Devolver más de lo que costó vuelve al de por defecto");
		helper.assertValueEqual(config.de(Estadistica.VIDA), new Configuracion.Mejora(4, 5), "vida, como la escribió");
		helper.assertValueEqual(config.de(Estadistica.DANIO), new Configuracion.Mejora(Configuracion.POR_DEFECTO.de(Estadistica.DANIO).porPunto(), 3), "daño: el porPunto negativo vuelve al de por defecto, el tope se respeta");
		helper.assertValueEqual(config.de(Estadistica.ARMADURA), Configuracion.POR_DEFECTO.de(Estadistica.ARMADURA), "armadura: un tope que no es número vuelve al de por defecto");
		helper.assertValueEqual(config.de(Estadistica.SUERTE), Configuracion.POR_DEFECTO.de(Estadistica.SUERTE), "Una que falta usa la de por defecto");
		helper.assertValueEqual(lectura.avisos().size(), 4, "Avisa de la devolución, el porPunto negativo, el tope que no es número y \"volar\" (" + lectura.avisos() + ")");
		helper.assertValueEqual(Files.readString(archivo), escrito, "Con avisos no se reescribe: el dueño corrige lo que escribió mal");
		helper.succeed();
	}

	@GameTest
	public void configSinAvisosAgregaLasQueFaltan(GameTestHelper helper) throws IOException {
		Path archivo = Files.createTempDirectory("estadisticasrpg").resolve("estadisticasrpg.json");
		// Como lo dejaba la 0.6.0: el costo en niveles, que ya no existe
		Files.writeString(archivo, "{ \"costoPorPunto\": 3, \"puntosPorAumento\": 150, \"estadisticas\": { \"vida\": { \"porPunto\": 1, \"tope\": 20 } } }");

		ArchivoDeConfiguracion.cargar(archivo);
		Configuracion releida = ArchivoDeConfiguracion.cargar(archivo).config();
		helper.assertValueEqual(releida.experienciaPorPunto(), COSTO, "El costo en experiencia que faltaba toma el de por defecto");
		helper.assertValueEqual(releida.de(Estadistica.VIDA), new Configuracion.Mejora(1, 20), "Se conserva la vida");
		String texto = Files.readString(archivo);
		helper.assertTrue(texto.contains("\"experienciaPorPunto\""), "El archivo ahora tiene experienciaPorPunto");
		helper.assertTrue(texto.contains("\"devolucionAlBajar\""), "Y devolucionAlBajar");
		helper.assertValueEqual(releida.devolucionAlBajar(), Configuracion.POR_DEFECTO.devolucionAlBajar(), "La devolución que faltaba toma la de por defecto");
		helper.assertTrue(!texto.contains("costoPorPunto") && !texto.contains("puntosPorAumento"), "Y ya no tiene el costo en niveles");
		for (Estadistica estadistica : Estadistica.values()) {
			helper.assertTrue(texto.contains("\"" + estadistica.id() + "\""), "El archivo ahora tiene " + estadistica.id());
		}
		helper.succeed();
	}

	@GameTest
	public void configRotaNoSeUsa(GameTestHelper helper) throws IOException {
		Path archivo = Files.createTempDirectory("estadisticasrpg").resolve("estadisticasrpg.json");
		Files.writeString(archivo, "{ \"costoPorPunto\": 2, ");

		ArchivoDeConfiguracion.Lectura lectura = ArchivoDeConfiguracion.cargar(archivo);
		helper.assertTrue(!lectura.ok(), "Un JSON roto no da config");
		helper.assertValueEqual(lectura.avisos().size(), 1, "Dice por qué");
		helper.assertValueEqual(Files.readString(archivo), "{ \"costoPorPunto\": 2, ", "No se pisa el archivo del dueño");
		helper.succeed();
	}

	/** Cambia la config global: se restaura en el finally antes de que corra cualquier otro test. */
	@GameTest
	public void cambiarLaConfigSeAplicaALosConectados(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "reconfigurado");
		Mejoras.dar(jugador, Estadistica.VIDA, 2);
		MinecraftServer servidor = helper.getLevel().getServer();
		Map<Estadistica, Configuracion.Mejora> mejoras = new EnumMap<>(Configuracion.POR_DEFECTO.mejoras());
		mejoras.put(Estadistica.VIDA, new Configuracion.Mejora(4, 1));
		try {
			EstadisticasRPG.usar(new Configuracion(10, 0.25, mejoras), servidor);
			helper.assertValueEqual(jugador.getAttributeValue(Attributes.MAX_HEALTH), 24.0, "Con el tope en 1, los 2 puntos valen como 1 de 4");
			helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 2, "Bajar el tope no borra puntos");

			ponerExperiencia(jugador, 50);
			Mejoras.comprar(jugador, Estadistica.DANIO, 1);
			helper.assertValueEqual(Mejoras.experiencia(jugador), 40, "Cobra el costo de la config");
			Mejoras.bajar(jugador, Estadistica.DANIO, 1);
			helper.assertValueEqual(Mejoras.experiencia(jugador), 42, "Devuelve la parte de la config, para abajo (2,5 → 2)");
		} finally {
			EstadisticasRPG.usar(Configuracion.POR_DEFECTO, servidor);
		}
		assertCerca(helper, jugador.getAttributeValue(Attributes.MAX_HEALTH), 21.2, "De vuelta: 2 puntos de 0,6");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void grupoLoQueCompraUnoLoTienenTodos(GameTestHelper helper) {
		ServerPlayer a = entrar(helper, "grupo-a");
		ServerPlayer b = entrar(helper, "grupo-b");
		conPuntosDelGrupo(helper, () -> {
			ponerExperiencia(a, COSTO * 2);
			Mejoras.comprar(a, Estadistica.VIDA, 1);
			helper.assertValueEqual(Mejoras.experiencia(a), COSTO, "Paga el que compra (Soul Link después lo reparte)");
			helper.assertValueEqual(Mejoras.puntos(b).de(Estadistica.VIDA), 1, "B ve el punto que compró A");
			assertCerca(helper, b.getAttributeValue(Attributes.MAX_HEALTH), 20.6, "B tiene la vida extra");
			helper.assertValueEqual(a.getAttachedOrCreate(EstadisticasRPG.PUNTOS).de(Estadistica.VIDA), 0, "El attachment de A no se toca");

			Mejoras.reiniciar(b);
			helper.assertValueEqual(a.getAttributeValue(Attributes.MAX_HEALTH), 20.0, "Reiniciar desde B le saca los puntos a A también");
		});
		salir(helper, a);
		salir(helper, b);
		helper.succeed();
	}

	@GameTest
	public void grupoSumaLosPuntosIndividualesUnaSolaVez(GameTestHelper helper) {
		ServerPlayer p = entrar(helper, "individual-p");
		ServerPlayer q = entrar(helper, "individual-q");
		// Antes de compartir, cada uno compró los suyos
		Mejoras.dar(p, Estadistica.VIDA, 3);
		Mejoras.dar(p, Estadistica.DANIO, 2);
		Mejoras.dar(q, Estadistica.VIDA, 5);
		conPuntosDelGrupo(helper, () -> {
			Mejoras.alEntrar(p);
			helper.assertValueEqual(Mejoras.puntos(p).de(Estadistica.VIDA), 3, "Entra P: el grupo toma su vida");
			Mejoras.alEntrar(q);
			helper.assertValueEqual(Mejoras.puntos(p).de(Estadistica.VIDA), 5, "Entra Q: el máximo, no la suma (que podría pasar el tope)");
			helper.assertValueEqual(Mejoras.puntos(p).de(Estadistica.DANIO), 2, "El daño de P sigue");
			assertCerca(helper, p.getAttributeValue(Attributes.MAX_HEALTH), 23.0, "A P le llega la vida de Q");

			Mejoras.reiniciar(p);
			Mejoras.alEntrar(q);
			helper.assertValueEqual(Mejoras.puntos(p).de(Estadistica.VIDA), 0, "Después de reiniciar, reconectar no devuelve los puntos viejos");
			helper.assertValueEqual(q.getAttachedOrCreate(EstadisticasRPG.PUNTOS).de(Estadistica.VIDA), 5, "El attachment de Q queda intacto");
		});
		salir(helper, p);
		salir(helper, q);
		helper.succeed();
	}

	@GameTest
	public void grupoAlReaparecerConservaLosPuntos(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "grupo-renacido");
		ServerPlayer[] reaparecido = new ServerPlayer[1];
		conPuntosDelGrupo(helper, () -> {
			Mejoras.dar(jugador, Estadistica.VIDA, 50);
			reaparecido[0] = helper.getLevel().getServer().getPlayerList().respawn(jugador, false, Entity.RemovalReason.KILLED);
			assertCerca(helper, reaparecido[0].getAttributeValue(Attributes.MAX_HEALTH), 50.0, "Los puntos del grupo siguen después de morir");
			assertCerca(helper, reaparecido[0].getHealth(), 50.0, "Reaparece con la vida llena");
		});
		salir(helper, reaparecido[0]);
		helper.succeed();
	}

	@GameTest
	public void elEstadoParaLaPantallaTieneTodo(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pantalla-estado");
		ponerExperiencia(jugador, COSTO * 4);
		Mejoras.dar(jugador, Estadistica.VIDA, 10);

		Paquetes.Estado estado = InterfazDelCliente.estado(jugador);
		helper.assertValueEqual(estado.experiencia(), COSTO * 4, "Experiencia");
		helper.assertValueEqual(estado.costo(), COSTO, "Costo por punto");
		helper.assertValueEqual(estado.devolucion(), DEVOLUCION, "Lo que devuelve bajar un punto");
		helper.assertFalse(estado.grupo(), "Sin Soul Link los puntos son de cada uno");
		helper.assertValueEqual(estado.filas().size(), Estadistica.values().length, "Una fila por estadística");
		Paquetes.Fila vida = estado.filas().get(Estadistica.VIDA.ordinal());
		helper.assertValueEqual(vida.id(), Estadistica.VIDA.id(), "Las filas van en el orden del enum");
		helper.assertValueEqual(vida.puntos(), 10, "Puntos de vida");
		helper.assertValueEqual(vida.tope(), Configuracion.POR_DEFECTO.de(Estadistica.VIDA).tope(), "Tope de vida");
		helper.assertValueEqual(vida.ahora(), "+3 corazones", "Lo que dan los puntos, armado en el servidor");
		helper.assertValueEqual(vida.proximo(), "+0,3 corazones", "Lo que suma el próximo punto");
		helper.assertValueEqual(estado.filas().get(Estadistica.DANIO.ordinal()).ahora(), "", "Sin puntos, \"ahora\" va vacío");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void elEstadoViajaPorLaRedSinPerderNada(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pantalla-red");
		ponerExperiencia(jugador, COSTO * 2 + 7);
		Mejoras.dar(jugador, Estadistica.ALCANCE, 25);
		Paquetes.Estado estado = InterfazDelCliente.estado(jugador);

		ByteBuf buffer = Unpooled.buffer();
		Paquetes.Estado.CODEC.encode(buffer, estado);
		helper.assertValueEqual(Paquetes.Estado.CODEC.decode(buffer), estado, "El estado decodificado");
		helper.assertValueEqual(buffer.readableBytes(), 0, "No sobran bytes");

		ByteBuf pedido = Unpooled.buffer();
		Paquetes.Comprar.CODEC.encode(pedido, new Paquetes.Comprar("vida", 5));
		helper.assertValueEqual(Paquetes.Comprar.CODEC.decode(pedido), new Paquetes.Comprar("vida", 5), "El pedido de compra decodificado");
		ByteBuf baja = Unpooled.buffer();
		Paquetes.Bajar.CODEC.encode(baja, new Paquetes.Bajar("salto", 1));
		helper.assertValueEqual(Paquetes.Bajar.CODEC.decode(baja), new Paquetes.Bajar("salto", 1), "El pedido de bajar decodificado");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void losPedidosDelClienteNoSeCreen(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pantalla-tramposo");
		ponerExperiencia(jugador, COSTO * 3);

		// Con -5, Mejoras.comprar cobraría experiencia negativa: le regalaría 150 y le restaría puntos
		for (Paquetes.Comprar trampa : List.of(new Paquetes.Comprar("vida", -5), new Paquetes.Comprar("vida", 0), new Paquetes.Comprar("volar", 1))) {
			helper.assertTrue(InterfazDelCliente.comprar(jugador, trampa).isEmpty(), "No se intenta comprar con " + trampa);
		}
		helper.assertValueEqual(Mejoras.experiencia(jugador), COSTO * 3, "No cambia la experiencia");
		helper.assertValueEqual(Mejoras.puntos(jugador), PuntosDelJugador.vacio(), "No cambian los puntos");

		Mejoras.Compra compra = InterfazDelCliente.comprar(jugador, new Paquetes.Comprar("vida", Integer.MAX_VALUE)).orElseThrow();
		helper.assertValueEqual(compra.comprados(), 3, "Pidiendo de más, compra lo que alcanza");
		helper.assertValueEqual(compra.resultado(), Mejoras.Resultado.SIN_EXPERIENCIA, "Lo cortó la experiencia");
		helper.assertValueEqual(Mejoras.experiencia(jugador), 0, "Cobra exactamente los 3");

		// Con -5, bajar sumaría puntos gratis
		for (Paquetes.Bajar trampa : List.of(new Paquetes.Bajar("vida", -5), new Paquetes.Bajar("vida", 0), new Paquetes.Bajar("volar", 1))) {
			helper.assertTrue(InterfazDelCliente.bajar(jugador, trampa).isEmpty(), "No se intenta bajar con " + trampa);
		}
		helper.assertValueEqual(Mejoras.puntos(jugador).de(Estadistica.VIDA), 3, "No cambian los puntos");
		Mejoras.Devolucion devolucion = InterfazDelCliente.bajar(jugador, new Paquetes.Bajar("vida", Integer.MAX_VALUE)).orElseThrow();
		helper.assertValueEqual(devolucion.bajados(), 3, "Pidiendo de más, baja los que hay");
		helper.assertValueEqual(Mejoras.experiencia(jugador), DEVOLUCION * 3, "Devuelve por los 3");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void sinElComplementoEstadisticasAbreElCofre(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "pantalla-vanilla");
		// Aunque dijera que prefiere la pantalla: si no registró los canales, no tiene con qué mostrarla
		InterfazDelCliente.preferir(jugador, true);

		ejecutar(helper, jugador, "estadisticas");
		helper.assertTrue(jugador.containerMenu instanceof MenuDeEstadisticas, "Abre el menú de cofre");

		jugador.closeContainer();
		salir(helper, jugador);
		helper.succeed();
	}

	/**
	 * Fuerza los puntos compartidos (en los tests no está Soul Link) y los deja como en un mundo
	 * nuevo. Se restaura todo en el finally: el modo y los puntos del grupo son globales del servidor.
	 */
	private static void conPuntosDelGrupo(GameTestHelper helper, Runnable prueba) {
		MinecraftServer servidor = helper.getLevel().getServer();
		PuntosDelGrupo.de(servidor).olvidarTodo();
		Mejoras.usarPuntosDelGrupo(true);
		try {
			prueba.run();
		} finally {
			Mejoras.usarPuntosDelGrupo(false);
			PuntosDelGrupo.de(servidor).olvidarTodo();
			Mejoras.aplicarATodos(servidor);
		}
	}

	/** Deja al jugador con exactamente esa experiencia, como {@code /xp set 0 levels} y {@code /xp add <n> points}. */
	private static void ponerExperiencia(ServerPlayer jugador, int puntos) {
		jugador.setExperienceLevels(0);
		jugador.setExperiencePoints(0);
		jugador.giveExperiencePoints(puntos);
	}

	private static void assertCerca(GameTestHelper helper, double actual, double esperado, String mensaje) {
		// La vida es float: con 1e-9, 20,6f no daría igual a 20,6
		helper.assertTrue(Math.abs(actual - esperado) < 1e-4, mensaje + ": esperaba " + esperado + ", dio " + actual);
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
