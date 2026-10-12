package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.herosforge.Oficios;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
abstract class DurabilidadMixin {
	// Después de Irrompibilidad: lo que el encantamiento ya salvó no se vuelve a sortear. Todos los hurtAndBreak
	// pasan por acá, con el jugador en null si el que usa el ítem no es un jugador
	@Inject(method = "processDurabilityChange", at = @At("RETURN"), cancellable = true)
	private void herosforge$cuidado(int gasto, ServerLevel level, ServerPlayer jugador, CallbackInfoReturnable<Integer> info) {
		if (jugador != null && info.getReturnValueI() > 0) {
			info.setReturnValue(Oficios.durabilidad(jugador, info.getReturnValueI()));
		}
	}
}
