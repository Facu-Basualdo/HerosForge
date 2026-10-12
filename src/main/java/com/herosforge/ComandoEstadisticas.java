package com.herosforge;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ComandoEstadisticas {
	private ComandoEstadisticas() {
	}

	/** Los nombres de los subcomandos en un idioma: el árbol se arma una vez con cada uno. */
	private record Nombres(String ver, String subir, String bajar, String dar, String reiniciar, String recargar) {
	}

	private static final List<Nombres> NOMBRES = List.of(
			new Nombres("ver", "subir", "bajar", "dar", "reiniciar", "recargar"),
			new Nombres("view", "buy", "refund", "give", "reset", "reload"));

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> arbol = Commands.literal("estadisticas").executes(ComandoEstadisticas::abrir);
		for (Nombres nombres : NOMBRES) {
			arbol.then(Commands.literal(nombres.ver())
							.executes(ctx -> ver(ctx, ctx.getSource().getPlayerOrException()))
							.then(Commands.argument("jugador", EntityArgument.player())
									.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
									.executes(ctx -> ver(ctx, EntityArgument.getPlayer(ctx, "jugador")))))
					.then(Commands.literal(nombres.subir())
							.then(estadistica()
									.executes(ctx -> subir(ctx, 1))
									.then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
											.executes(ctx -> subir(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"))))))
					.then(Commands.literal(nombres.bajar())
							.then(estadistica()
									.executes(ctx -> bajar(ctx, 1))
									.then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
											.executes(ctx -> bajar(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"))))))
					.then(Commands.literal(nombres.dar())
							.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
							.then(Commands.argument("jugador", EntityArgument.player())
									.then(estadistica()
											.then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
													.executes(ComandoEstadisticas::dar)))))
					.then(Commands.literal(nombres.reiniciar())
							.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
							.then(Commands.argument("jugador", EntityArgument.player())
									.executes(ComandoEstadisticas::reiniciar)))
					.then(Commands.literal(nombres.recargar())
							.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
							.executes(ComandoEstadisticas::recargar));
		}
		LiteralCommandNode<CommandSourceStack> raiz = dispatcher.register(arbol);

		// Un redirect no hereda el executes del nodo destino: sin el propio, "/stats" solo no haría nada
		dispatcher.register(Commands.literal("stats")
				.executes(ComandoEstadisticas::abrir)
				.redirect(raiz));
	}

	private static int abrir(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		InterfazDelCliente.abrir(ctx.getSource().getPlayerOrException());
		return 1;
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> estadistica() {
		return Commands.argument("estadistica", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Estadistica.values()).map(Estadistica::id), builder));
	}

	private static Estadistica leerEstadistica(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String id = StringArgumentType.getString(ctx, "estadistica");
		Optional<Estadistica> estadistica = Estadistica.porId(id);
		if (estadistica.isEmpty()) {
			// En el idioma de quien lo usó: por eso no es una constante, como suelen ser los errores de comando
			throw new SimpleCommandExceptionType(Idioma.tr(ctx.getSource(), "herosforge.comando.desconocida", id, ids())).create();
		}
		return estadistica.get();
	}

	private static int ver(CommandContext<CommandSourceStack> ctx, ServerPlayer jugador) {
		Configuracion config = HerosForge.config();
		PuntosDelJugador puntos = Mejoras.puntos(jugador);
		String idioma = Idioma.de(ctx.getSource().getPlayer());
		StringBuilder texto = new StringBuilder(Idioma.tr(idioma, "herosforge.ver.encabezado", deQuien(idioma, jugador),
				Mejoras.experiencia(idioma, config.experienciaPorPunto()), config.devolucionPorPunto(),
				Mejoras.experiencia(idioma, Mejoras.experiencia(jugador))).getString());
		for (Estadistica estadistica : Estadistica.values()) {
			if (estadistica.rama().estadisticas().getFirst() == estadistica) {
				texto.append("\n— ").append(estadistica.rama().nombre(idioma)).append(" —");
			}
			Configuracion.Mejora mejora = config.de(estadistica);
			int actuales = puntos.de(estadistica);
			texto.append("\n").append(estadistica.nombre(idioma)).append(" (").append(estadistica.id()).append("): ")
					.append(actuales).append("/").append(mejora.tope());
			if (actuales > 0) {
				texto.append(" · ").append(estadistica.describir(idioma, mejora.efecto(actuales)));
			}
			texto.append(actuales >= mejora.tope()
					? Idioma.tr(idioma, "herosforge.ver.maximo").getString()
					: Idioma.tr(idioma, "herosforge.ver.proximo", estadistica.describir(idioma, mejora.porPunto())).getString());
		}
		ctx.getSource().sendSuccess(() -> Component.literal(texto.toString()), false);
		return 1;
	}

	private static int subir(CommandContext<CommandSourceStack> ctx, int cantidad) throws CommandSyntaxException {
		ServerPlayer jugador = ctx.getSource().getPlayerOrException();
		Mejoras.Compra compra = Mejoras.comprar(jugador, leerEstadistica(ctx), cantidad);
		if (compra.comprados() == 0) {
			ctx.getSource().sendFailure(Mejoras.mensaje(Idioma.de(jugador), compra));
		} else {
			ctx.getSource().sendSuccess(() -> Mejoras.mensaje(Idioma.de(jugador), compra), false);
		}
		return compra.comprados();
	}

	private static int bajar(CommandContext<CommandSourceStack> ctx, int cantidad) throws CommandSyntaxException {
		Mejoras.Devolucion devolucion = Mejoras.bajar(ctx.getSource().getPlayerOrException(), leerEstadistica(ctx), cantidad);
		if (devolucion.bajados() == 0) {
			ctx.getSource().sendFailure(Mejoras.mensaje(Idioma.de(ctx.getSource().getPlayer()), devolucion));
		} else {
			ctx.getSource().sendSuccess(() -> Mejoras.mensaje(Idioma.de(ctx.getSource().getPlayer()), devolucion), false);
		}
		return devolucion.bajados();
	}

	private static int dar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
		Estadistica estadistica = leerEstadistica(ctx);
		int dados = Mejoras.dar(jugador, estadistica, IntegerArgumentType.getInteger(ctx, "cantidad"));
		String idioma = Idioma.de(ctx.getSource().getPlayer());
		if (dados == 0) {
			ctx.getSource().sendFailure(Idioma.tr(idioma, "herosforge.dar.tope", estadistica.nombre(idioma), deQuien(idioma, jugador)));
		} else {
			ctx.getSource().sendSuccess(() -> Idioma.tr(idioma, Mejoras.puntosDelGrupo() ? "herosforge.dar.hecho_grupo" : "herosforge.dar.hecho",
					Mejoras.puntos(idioma, dados), estadistica.nombre(idioma).toLowerCase(Locale.ROOT), jugador.getName().getString()), true);
		}
		return dados;
	}

	private static int reiniciar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
		Mejoras.reiniciar(jugador);
		ctx.getSource().sendSuccess(() -> Idioma.tr(ctx.getSource(), "herosforge.reiniciar.hecho", deQuien(Idioma.de(ctx.getSource().getPlayer()), jugador)), true);
		return 1;
	}

	private static int recargar(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack fuente = ctx.getSource();
		ArchivoDeConfiguracion.Lectura lectura = HerosForge.recargar(fuente.getServer());
		lectura.avisos().forEach(aviso -> fuente.sendSuccess(() -> Component.literal(aviso).withStyle(ChatFormatting.YELLOW), false));
		if (!lectura.ok()) {
			fuente.sendFailure(Idioma.tr(fuente, "herosforge.config.anterior"));
			return 0;
		}
		fuente.sendSuccess(() -> lectura.avisos().isEmpty()
				? Idioma.tr(fuente, "herosforge.config.recargada")
				: Idioma.tr(fuente, "herosforge.config.recargada_avisos", lectura.avisos().size()), true);
		return 1;
	}

	/** Con los puntos compartidos, cualquier jugador ve y cambia los del grupo: "del grupo", "de Ana". */
	private static String deQuien(String idioma, ServerPlayer jugador) {
		return Mejoras.puntosDelGrupo()
				? Idioma.tr(idioma, "herosforge.de_quien.grupo").getString()
				: Idioma.tr(idioma, "herosforge.de_quien.jugador", jugador.getName().getString()).getString();
	}

	private static String ids() {
		return String.join(", ", Arrays.stream(Estadistica.values()).map(Estadistica::id).toList());
	}
}
