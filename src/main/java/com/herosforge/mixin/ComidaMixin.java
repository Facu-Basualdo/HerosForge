package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.herosforge.Supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * En 1.21.1 lo que se come pasa por Player.eat (después de sumarlo a FoodData); en 26.x, por FoodProperties.onConsume.
 */
@Mixin(Player.class)
abstract class ComidaMixin {
	// FoodData no sabe de quién es: lo de la nutrición se suma acá, donde se conoce al que comió
	@Inject(method = "eat", at = @At("TAIL"))
	private void herosforge$nutricion(Level level, ItemStack comida, FoodProperties propiedades, CallbackInfoReturnable<ItemStack> info) {
		if ((Object) this instanceof ServerPlayer jugador) {
			Supervivencia.nutrir(jugador, propiedades);
		}
	}
}
