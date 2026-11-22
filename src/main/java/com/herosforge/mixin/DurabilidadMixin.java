package com.herosforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.herosforge.Oficios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemStack.class)
abstract class DurabilidadMixin {
	// Después de Irrompibilidad: lo que el encantamiento ya salvó no se vuelve a sortear. Todos los hurtAndBreak
	// pasan por acá, con el jugador en null si el que usa el ítem no es un jugador. En 1.21.1 el cálculo lo hace
	// EnchantmentHelper adentro de hurtAndBreak (en 26.x es ItemStack.processDurabilityChange)
	@ModifyExpressionValue(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
	private int herosforge$cuidado(int gasto, @Local(argsOnly = true) ServerPlayer jugador) {
		return jugador != null && gasto > 0 ? Oficios.durabilidad(jugador, gasto) : gasto;
	}
}
