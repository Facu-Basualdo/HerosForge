package com.estadisticasrpg;

import java.util.Arrays;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ComandoEstadisticas {
	private static final DynamicCommandExceptionType ESTADISTICA_DESCONOCIDA = new DynamicCommandExceptionType(
			id -> Component.literal("No existe la estadística \"" + id + "\". Opciones: " + ids()));

	private ComandoEstadisticas() {
	}

	public static void registrar(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> raiz = dispatcher.register(Commands.literal("estadisticas")
				// Fase 2: sin argumentos abre el menú
				.executes(ctx -> ver(ctx, ctx.getSource().getPlayerOrException()))
				.then(Commands.literal("ver")
						.executes(ctx -> ver(ctx, ctx.getSource().getPlayerOrException()))
						.then(Commands.argument("jugador", EntityArgument.player())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(ctx -> ver(ctx, EntityArgument.getPlayer(ctx, "jugador")))))
				.then(Commands.literal("subir")
						.then(estadistica()
								.executes(ctx -> subir(ctx, 1))
								.then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
										.executes(ctx -> subir(ctx, IntegerArgumentType.getInteger(ctx, "cantidad"))))))
				.then(Commands.literal("dar")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("jugador", EntityArgument.player())
								.then(estadistica()
										.then(Commands.argument("cantidad", IntegerArgumentType.integer(1))
												.executes(ComandoEstadisticas::dar)))))
				.then(Commands.literal("reiniciar")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("jugador", EntityArgument.player())
								.executes(ComandoEstadisticas::reiniciar))));

		// Un redirect no hereda el executes del nodo destino: sin el propio, "/stats" solo no haría nada
		dispatcher.register(Commands.literal("stats")
				.executes(ctx -> ver(ctx, ctx.getSource().getPlayerOrException()))
				.redirect(raiz));
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> estadistica() {
		return Commands.argument("estadistica", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Estadistica.values()).map(Estadistica::id), builder));
	}

	private static Estadistica leerEstadistica(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String id = StringArgumentType.getString(ctx, "estadistica");
		return Estadistica.porId(id).orElseThrow(() -> ESTADISTICA_DESCONOCIDA.create(id));
	}

	private static int ver(CommandContext<CommandSourceStack> ctx, ServerPlayer jugador) {
		Configuracion config = EstadisticasRPG.config();
		PuntosDelJugador puntos = Mejoras.puntos(jugador);
		StringBuilder texto = new StringBuilder("Estadísticas de ").append(jugador.getName().getString())
				.append(" · cada punto cuesta ").append(Mejoras.niveles(config.costoPorPunto()))
				.append(" · tenés ").append(Mejoras.niveles(jugador.experienceLevel));
		for (Estadistica estadistica : Estadistica.values()) {
			Configuracion.Mejora mejora = config.de(estadistica);
			int actuales = puntos.de(estadistica);
			texto.append("\n").append(estadistica.nombre()).append(" (").append(estadistica.id()).append("): ")
					.append(actuales).append("/").append(mejora.tope());
			if (actuales > 0) {
				texto.append(" · ").append(estadistica.describir(mejora.efecto(actuales)));
			}
			texto.append(actuales >= mejora.tope() ? " · máximo" : " · próximo " + estadistica.describir(mejora.porPunto()));
		}
		ctx.getSource().sendSuccess(() -> Component.literal(texto.toString()), false);
		return 1;
	}

	private static int subir(CommandContext<CommandSourceStack> ctx, int cantidad) throws CommandSyntaxException {
		ServerPlayer jugador = ctx.getSource().getPlayerOrException();
		Mejoras.Compra compra = Mejoras.comprar(jugador, leerEstadistica(ctx), cantidad);
		if (compra.comprados() == 0) {
			ctx.getSource().sendFailure(Mejoras.mensaje(compra));
		} else {
			ctx.getSource().sendSuccess(() -> Mejoras.mensaje(compra), false);
		}
		return compra.comprados();
	}

	private static int dar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
		Estadistica estadistica = leerEstadistica(ctx);
		int dados = Mejoras.dar(jugador, estadistica, IntegerArgumentType.getInteger(ctx, "cantidad"));
		if (dados == 0) {
			ctx.getSource().sendFailure(Component.literal(estadistica.nombre() + " de " + jugador.getName().getString() + " ya está en el máximo"));
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("Le diste " + dados + (dados == 1 ? " punto" : " puntos") + " de "
					+ estadistica.nombre().toLowerCase() + " a " + jugador.getName().getString()), true);
		}
		return dados;
	}

	private static int reiniciar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer jugador = EntityArgument.getPlayer(ctx, "jugador");
		Mejoras.reiniciar(jugador);
		ctx.getSource().sendSuccess(() -> Component.literal("Las estadísticas de " + jugador.getName().getString() + " volvieron a 0"), true);
		return 1;
	}

	private static String ids() {
		return String.join(", ", Arrays.stream(Estadistica.values()).map(Estadistica::id).toList());
	}
}
