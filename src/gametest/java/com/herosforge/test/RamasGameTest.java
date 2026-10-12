package com.herosforge.test;

import static com.herosforge.test.HerosForgeGameTest.assertCerca;
import static com.herosforge.test.HerosForgeGameTest.entrar;
import static com.herosforge.test.HerosForgeGameTest.ponerExperiencia;
import static com.herosforge.test.HerosForgeGameTest.salir;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.herosforge.Combate;
import com.herosforge.Configuracion;
import com.herosforge.Estadistica;
import com.herosforge.HerosForge;
import com.herosforge.Idioma;
import com.herosforge.Mejoras;
import com.herosforge.Oficios;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;

/**
 * Las 25 estadísticas de las ramas (0.13.0): los atributos nuevos y lo que aplican Combate, Supervivencia, Oficios
 * y sus mixins. Las que dependen del azar se prueban con la config en 100 %, así cada medición es una sola.
 */
public class RamasGameTest {
	/** Para que una probabilidad llegue a 1 con 100 puntos. */
	private static final Configuracion.Mejora SEGURO = new Configuracion.Mejora(0.01, 100);

	@GameTest
	public void atributosNuevosAlMaximo(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "atributos");
		record Caso(Estadistica estadistica, Holder<Attribute> atributo, double suma, double multiplica) {
		}
		List<Caso> casos = List.of(
				new Caso(Estadistica.DUREZA, Attributes.ARMOR_TOUGHNESS, 8, 1),
				new Caso(Estadistica.BARRIDO, Attributes.SWEEPING_DAMAGE_RATIO, 0.5, 1),
				new Caso(Estadistica.GOLPE, Attributes.ATTACK_KNOCKBACK, 1, 1),
				new Caso(Estadistica.ANTIEXPLOSION, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 0.5, 1),
				new Caso(Estadistica.ESCUDO, Attributes.MAX_ABSORPTION, 8, 1),
				new Caso(Estadistica.IGNIFUGO, Attributes.BURNING_TIME, 0, 0.5),
				new Caso(Estadistica.PASO, Attributes.STEP_HEIGHT, 0.5, 1),
				new Caso(Estadistica.NADO, Attributes.WATER_MOVEMENT_EFFICIENCY, 0.5, 1),
				new Caso(Estadistica.SIGILO, Attributes.SNEAKING_SPEED, 0.4, 1),
				new Caso(Estadistica.LIGEREZA, Attributes.GRAVITY, 0, 0.75),
				new Caso(Estadistica.MINADO_SUBMARINO, Attributes.SUBMERGED_MINING_SPEED, 0.8, 1));
		for (Caso caso : casos) {
			helper.assertTrue(jugador.getAttribute(caso.atributo()) != null, "El jugador tiene " + caso.atributo().getRegisteredName());
			double base = jugador.getAttributeBaseValue(caso.atributo());
			Mejoras.dar(jugador, caso.estadistica(), 100);
			assertCerca(helper, jugador.getAttributeValue(caso.atributo()), base * caso.multiplica() + caso.suma(), caso.estadistica().id());
		}

		Mejoras.reiniciar(jugador);
		for (Caso caso : casos) {
			assertCerca(helper, jugador.getAttributeValue(caso.atributo()), jugador.getAttributeBaseValue(caso.atributo()), caso.estadistica().id() + " sin puntos");
		}
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void descripciones(GameTestHelper helper) {
		helper.assertValueEqual(Estadistica.CRITICO.describir(Idioma.ESPANOL, 0.2), "20 % de probabilidad de crítico (+50 % de daño)", "Crítico");
		helper.assertValueEqual(Estadistica.METABOLISMO.describir(Idioma.ESPANOL, 0.5), "−50 % de hambre", "Lo que mejora al bajar lleva un menos");
		helper.assertValueEqual(Estadistica.ESCUDO.describir(Idioma.ESPANOL, 8), "Escudo de 4 corazones que se recarga fuera de pelea", "El escudo en corazones");
		helper.assertValueEqual(Estadistica.IMAN.describir(Idioma.ESPANOL, 6), "Atraés ítems y experiencia a 6 bloques (agachado no)", "Imán");
		helper.succeed();
	}

	@GameTest
	public void criticoYEjecucion(GameTestHelper helper) {
		ServerPlayer certero = entrar(helper, "certero");
		ServerPlayer verdugo = entrar(helper, "verdugo");
		ServerLevel level = helper.getLevel();
		conConfig(helper, Map.of(Estadistica.CRITICO, SEGURO), () -> {
			Mejoras.dar(certero, Estadistica.CRITICO, 100);
			IronGolem blanco = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));
			blanco.hurtServer(level, certero.damageSources().playerAttack(certero), 8);
			assertCerca(helper, blanco.getMaxHealth() - blanco.getHealth(), 12, "Con el crítico seguro, 8 pega 12");
			blanco.discard();
		});

		Mejoras.dar(verdugo, Estadistica.EJECUCION, 100);
		IronGolem sano = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));
		sano.hurtServer(level, verdugo.damageSources().playerAttack(verdugo), 8);
		assertCerca(helper, sano.getMaxHealth() - sano.getHealth(), 8, "Con la vida llena, la ejecución no suma");
		IronGolem herido = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(4, 2, 2));
		herido.setHealth(herido.getMaxHealth() * 0.2f);
		float antes = herido.getHealth();
		herido.hurtServer(level, verdugo.damageSources().playerAttack(verdugo), 8);
		assertCerca(helper, antes - herido.getHealth(), 12, "Debajo del 30 % de vida, +50 %");

		sano.discard();
		herido.discard();
		salir(helper, certero);
		salir(helper, verdugo);
		helper.succeed();
	}

	/** Un jugador nuevo por golpe: después de cada uno, vanilla le da medio segundo sin recibir daño. */
	@GameTest
	public void resistenciasBajanElDanioElemental(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer fuego = resistente(helper, "resistente-fuego");
		fuego.hurtServer(level, fuego.damageSources().inFire(), 10);
		assertCerca(helper, fuego.getHealth(), 13, "−30 % de fuego: 10 pega 7");
		ServerPlayer magia = resistente(helper, "resistente-magia");
		magia.hurtServer(level, magia.damageSources().magic(), 10);
		assertCerca(helper, magia.getHealth(), 13, "−30 % de magia");
		ServerPlayer comun = resistente(helper, "resistente-comun");
		comun.hurtServer(level, comun.damageSources().generic(), 10);
		assertCerca(helper, comun.getHealth(), 10, "Lo que no es fuego, explosión ni magia pega entero");

		salir(helper, fuego);
		salir(helper, magia);
		salir(helper, comun);
		helper.succeed();
	}

	private static ServerPlayer resistente(GameTestHelper helper, String nombre) {
		ServerPlayer jugador = vulnerable(helper, nombre);
		Mejoras.dar(jugador, Estadistica.RESISTENCIAS, 100);
		return jugador;
	}

	@GameTest
	public void esquivaSoloAtaques(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer agil = vulnerable(helper, "agil");
		ServerPlayer caido = vulnerable(helper, "agil-caido");
		IronGolem atacante = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));
		conConfig(helper, Map.of(Estadistica.ESQUIVA, SEGURO), () -> {
			Mejoras.dar(agil, Estadistica.ESQUIVA, 100);
			Mejoras.dar(caido, Estadistica.ESQUIVA, 100);
			agil.hurtServer(level, agil.damageSources().mobAttack(atacante), 6);
			assertCerca(helper, agil.getHealth(), 20, "Con la esquiva segura, el golpe del gólem no entra");
			caido.hurtServer(level, caido.damageSources().generic(), 6);
			assertCerca(helper, caido.getHealth(), 14, "Lo que no viene de nadie no se esquiva");
		});

		atacante.discard();
		salir(helper, agil);
		salir(helper, caido);
		helper.succeed();
	}

	@GameTest
	public void represaliaDevuelveElGolpe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer espinoso = vulnerable(helper, "espinoso");
		Mejoras.dar(espinoso, Estadistica.REPRESALIA, 100);
		IronGolem atacante = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));

		espinoso.hurtServer(level, espinoso.damageSources().mobAttack(atacante), 10);
		// Lo que entró depende de la dificultad del mundo de prueba (escala el golpe de un mob): se mide
		float recibido = 20 - espinoso.getHealth();
		helper.assertTrue(recibido > 0, "El golpe entra (la dificultad no es pacífica)");
		assertCerca(helper, atacante.getMaxHealth() - atacante.getHealth(), recibido * 0.3, "Devuelve el 30 % de lo que entró");

		// Una flecha no se devuelve: el que pega no es el que la tiró
		IronGolem otro = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(4, 2, 2));
		Arrow flecha = new Arrow(level, otro, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		ServerPlayer flechado = vulnerable(helper, "espinoso-flechado");
		Mejoras.dar(flechado, Estadistica.REPRESALIA, 100);
		flechado.hurtServer(level, flechado.damageSources().arrow(flecha, otro), 4);
		assertCerca(helper, otro.getHealth(), otro.getMaxHealth(), "A distancia no hay represalia");

		atacante.discard();
		otro.discard();
		flecha.discard();
		salir(helper, espinoso);
		salir(helper, flechado);
		helper.succeed();
	}

	@GameTest
	public void escudoSeRecargaFueraDePelea(GameTestHelper helper) {
		MinecraftServer servidor = helper.getLevel().getServer();
		ServerPlayer jugador = vulnerable(helper, "escudado");
		Mejoras.dar(jugador, Estadistica.ESCUDO, 100);
		Combate.olvidarGolpes();

		Combate.recargarEscudos(servidor);
		assertCerca(helper, jugador.getAbsorptionAmount(), 1, "Medio corazón por segundo");
		for (int segundo = 0; segundo < 20; segundo++) {
			Combate.recargarEscudos(servidor);
		}
		assertCerca(helper, jugador.getAbsorptionAmount(), 8, "Hasta los 4 corazones comprados");

		jugador.hurtServer(helper.getLevel(), jugador.damageSources().generic(), 3);
		assertCerca(helper, jugador.getAbsorptionAmount(), 5, "El escudo se come el golpe");
		assertCerca(helper, jugador.getHealth(), 20, "Y la vida queda entera");
		Combate.recargarEscudos(servidor);
		assertCerca(helper, jugador.getAbsorptionAmount(), 5, "Recién golpeado no se recarga");

		Mejoras.bajar(jugador, Estadistica.ESCUDO, 50);
		assertCerca(helper, jugador.getAbsorptionAmount(), 4, "Al bajar puntos, el escudo se corta al máximo nuevo");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void destrezaTensaMasRapido(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer novato = enElTest(helper, "tensa-novato", 1);
		ServerPlayer diestro = enElTest(helper, "tensa-diestro", 3);
		Mejoras.dar(diestro, Estadistica.DESTREZA, 100);

		ItemStack arco = new ItemStack(Items.BOW);
		int duracion = arco.getUseDuration(novato);
		helper.assertValueEqual(Combate.tensar(novato, Items.BOW, arco, duracion - 10), duracion - 10, "Sin puntos, el tiempo de vanilla");
		helper.assertValueEqual(Combate.tensar(diestro, Items.BOW, arco, duracion - 10), duracion - 14, "Con +40 %, 10 ticks valen 14");

		// El mixin, con un arco de verdad: medio tensado, la flecha del diestro sale bastante más rápido
		double lenta = disparar(level, novato, duracion - 10);
		double rapida = disparar(level, diestro, duracion - 10);
		helper.assertTrue(lenta > 0 && rapida > lenta * 1.4, "La flecha del diestro sale más rápido (" + lenta + " → " + rapida + ")");

		salir(helper, novato);
		salir(helper, diestro);
		helper.succeed();
	}

	/** Suelta un arco con {@code tiempoRestante} y devuelve la velocidad de la flecha que salió. */
	private static double disparar(ServerLevel level, ServerPlayer arquero, int tiempoRestante) {
		ItemStack arco = new ItemStack(Items.BOW);
		// Primero el arco: con la mano vacía, las flechas irían a la mano y el arco las pisaría
		arquero.setItemInHand(InteractionHand.MAIN_HAND, arco);
		arquero.getInventory().add(new ItemStack(Items.ARROW, 16));
		Items.BOW.releaseUsing(arco, level, arquero, tiempoRestante);
		double velocidad = 0;
		for (Arrow flecha : level.getEntitiesOfClass(Arrow.class, arquero.getBoundingBox().inflate(4), flecha -> flecha.getOwner() == arquero)) {
			velocidad = flecha.getDeltaMovement().length();
			flecha.discard();
		}
		return velocidad;
	}

	@GameTest
	public void metabolismoBajaMenosElHambre(GameTestHelper helper) {
		ServerPlayer comun = entrar(helper, "hambriento");
		ServerPlayer lento = entrar(helper, "metabolismo");
		Mejoras.dar(lento, Estadistica.METABOLISMO, 100);
		for (ServerPlayer jugador : List.of(comun, lento)) {
			jugador.getFoodData().setFoodLevel(20);
			jugador.getFoodData().setSaturation(5);
			// Pasado 4 de cansancio, el próximo tick del hambre baja un punto de saturación
			jugador.causeFoodExhaustion(4.4f);
			jugador.getFoodData().tick(jugador);
		}
		assertCerca(helper, comun.getFoodData().getSaturationLevel(), 4, "Sin puntos, 4,4 de cansancio gastan saturación");
		assertCerca(helper, lento.getFoodData().getSaturationLevel(), 5, "Con −50 % son 2,2: todavía no");

		salir(helper, comun);
		salir(helper, lento);
		helper.succeed();
	}

	@GameTest
	public void nutricionYGloton(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer comun = entrar(helper, "comensal");
		ServerPlayer nutrido = entrar(helper, "nutrido");
		Mejoras.dar(nutrido, Estadistica.NUTRICION, 100);
		for (ServerPlayer jugador : List.of(comun, nutrido)) {
			jugador.getFoodData().setFoodLevel(0);
			jugador.getFoodData().setSaturation(0);
			new ItemStack(Items.COOKED_BEEF).finishUsingItem(level, jugador);
		}
		helper.assertValueEqual(comun.getFoodData().getFoodLevel(), 8, "Un filete da 8");
		helper.assertValueEqual(nutrido.getFoodData().getFoodLevel(), 12, "Con +50 %, 12");

		ServerPlayer gloton = entrar(helper, "gloton");
		Mejoras.dar(gloton, Estadistica.GLOTON, 100);
		int normal = comer(comun);
		helper.assertTrue(normal > 1, "Comer lleva tiempo (" + normal + " ticks)");
		helper.assertValueEqual(comer(gloton), (int) Math.ceil(normal * 0.5), "Con −50 %, la mitad");
		comun.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
		comun.startUsingItem(InteractionHand.MAIN_HAND);
		gloton.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
		gloton.startUsingItem(InteractionHand.MAIN_HAND);
		helper.assertValueEqual(gloton.getUseItemRemainingTicks(), comun.getUseItemRemainingTicks(), "Un arco no se come: tarda lo mismo");

		salir(helper, comun);
		salir(helper, nutrido);
		salir(helper, gloton);
		helper.succeed();
	}

	private static int comer(ServerPlayer jugador) {
		jugador.stopUsingItem();
		jugador.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF));
		jugador.startUsingItem(InteractionHand.MAIN_HAND);
		int ticks = jugador.getUseItemRemainingTicks();
		jugador.stopUsingItem();
		return ticks;
	}

	@GameTest
	public void tenacidadAcortaLoNegativo(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "tenaz");
		Mejoras.dar(jugador, Estadistica.TENACIDAD, 100);
		jugador.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
		jugador.addEffect(new MobEffectInstance(MobEffects.SPEED, 200));
		helper.assertValueEqual(jugador.getEffect(MobEffects.POISON).getDuration(), 100, "El veneno dura la mitad");
		helper.assertValueEqual(jugador.getEffect(MobEffects.SPEED).getDuration(), 200, "La velocidad, entera");

		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void imanAtraeLoQueEstaCerca(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer servidor = level.getServer();
		ServerPlayer jugador = enElTest(helper, "iman", 1);
		ItemEntity item = new ItemEntity(level, jugador.getX() + 4, jugador.getY(), jugador.getZ(), new ItemStack(Items.DIRT));
		item.setNoPickUpDelay();
		level.addFreshEntity(item);
		item.setDeltaMovement(0, 0, 0);

		Oficios.atraer(servidor);
		helper.assertTrue(item.getDeltaMovement().x() == 0, "Sin puntos no se mueve");
		Mejoras.dar(jugador, Estadistica.IMAN, 100);
		Oficios.atraer(servidor);
		helper.assertTrue(item.getDeltaMovement().x() < 0, "Con 6 bloques de imán va hacia el jugador (" + item.getDeltaMovement() + ")");

		item.setDeltaMovement(0, 0, 0);
		item.setPickUpDelay(40);
		Oficios.atraer(servidor);
		helper.assertTrue(item.getDeltaMovement().x() == 0, "Lo recién tirado no vuelve solo");

		item.discard();
		salir(helper, jugador);
		helper.succeed();
	}

	@GameTest
	public void cuidadoDelEquipoSalvaDurabilidad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer comun = entrar(helper, "descuidado");
		ServerPlayer cuidadoso = entrar(helper, "cuidadoso");
		conConfig(helper, Map.of(Estadistica.CUIDADO_DEL_EQUIPO, SEGURO), () -> {
			Mejoras.dar(cuidadoso, Estadistica.CUIDADO_DEL_EQUIPO, 100);
			ItemStack gastado = new ItemStack(Items.IRON_PICKAXE);
			gastado.hurtAndBreak(5, level, comun, item -> {
			});
			ItemStack cuidado = new ItemStack(Items.IRON_PICKAXE);
			cuidado.hurtAndBreak(5, level, cuidadoso, item -> {
			});
			helper.assertValueEqual(gastado.getDamageValue(), 5, "Sin puntos gasta 5");
			helper.assertValueEqual(cuidado.getDamageValue(), 0, "Con el cuidado seguro, nada");
		});

		salir(helper, comun);
		salir(helper, cuidadoso);
		helper.succeed();
	}

	@GameTest
	public void sabiduriaSoloEnLosOrbes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer sabio = entrar(helper, "sabio");
		Mejoras.dar(sabio, Estadistica.SABIDURIA, 100);
		ponerExperiencia(sabio, 0);
		sabio.takeXpDelay = 0;
		new ExperienceOrb(level, sabio.getX(), sabio.getY(), sabio.getZ(), 100).playerTouch(sabio);
		helper.assertValueEqual(Mejoras.experiencia(sabio), 125, "Un orbe de 100 da 125");

		// Bajar devuelve experiencia con giveExperiencePoints, pero no es un orbe: no suma nada
		ponerExperiencia(sabio, HerosForge.config().experienciaPorPunto());
		Mejoras.comprar(sabio, Estadistica.VIDA, 1);
		Mejoras.bajar(sabio, Estadistica.VIDA, 1);
		helper.assertValueEqual(Mejoras.experiencia(sabio), HerosForge.config().devolucionPorPunto(), "Bajar devuelve lo de siempre");

		salir(helper, sabio);
		helper.succeed();
	}

	@GameTest
	public void regateoBajaLosPrecios(GameTestHelper helper) {
		ServerPlayer jugador = entrar(helper, "regateador");
		Mejoras.dar(jugador, Estadistica.REGATEO, 100);
		MerchantOffers ofertas = new MerchantOffers();
		MerchantOffer cara = new MerchantOffer(new ItemCost(Items.EMERALD, 10), new ItemStack(Items.BREAD), 12, 1, 0.05f);
		MerchantOffer barata = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD), 12, 1, 0.05f);
		ofertas.add(cara);
		ofertas.add(barata);

		Oficios.regatear(jugador, ofertas);
		helper.assertValueEqual(cara.getCostA().getCount(), 7, "−30 % de 10 esmeraldas");
		helper.assertValueEqual(barata.getCostA().getCount(), 1, "Una esmeralda no baja de 1");

		salir(helper, jugador);
		helper.succeed();
	}

	/**
	 * Un jugador dentro de la estructura del test, en la columna {@code x}: aparece en el spawn del mundo, que puede
	 * no estar cargado, y ahí no se agregan las entidades (la flecha que dispara, el ítem que atrae).
	 */
	private static ServerPlayer enElTest(GameTestHelper helper, String nombre, int x) {
		ServerPlayer jugador = entrar(helper, nombre);
		jugador.snapTo(helper.absoluteVec(new Vec3(x + 0.5, 2, 0.5)));
		return jugador;
	}

	/**
	 * Un jugador que ya puede recibir daño: vanilla no lastima a nadie hasta que su cliente avisa que terminó de
	 * cargar el mundo ({@code hasClientLoaded}), y el de los tests no tiene cliente.
	 */
	private static ServerPlayer vulnerable(GameTestHelper helper, String nombre) {
		ServerPlayer jugador = entrar(helper, nombre);
		jugador.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
		return jugador;
	}

	/** Cambia la config global y la restaura en el finally, antes de que corra cualquier otro test. */
	private static void conConfig(GameTestHelper helper, Map<Estadistica, Configuracion.Mejora> cambios, Runnable prueba) {
		MinecraftServer servidor = helper.getLevel().getServer();
		Map<Estadistica, Configuracion.Mejora> mejoras = new EnumMap<>(Configuracion.POR_DEFECTO.mejoras());
		mejoras.putAll(cambios);
		Configuracion defecto = Configuracion.POR_DEFECTO;
		try {
			HerosForge.usar(new Configuracion(defecto.experienciaPorPunto(), defecto.devolucionAlBajar(), mejoras, defecto.idioma()), servidor);
			prueba.run();
		} finally {
			HerosForge.usar(defecto, servidor);
		}
	}
}
