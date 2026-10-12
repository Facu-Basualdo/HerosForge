package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.herosforge.Supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

@Mixin(FoodProperties.class)
abstract class ComidaMixin {
	// FoodData no sabe de quién es: lo de la nutrición se suma acá, donde se conoce al que comió
	@Inject(method = "onConsume", at = @At("TAIL"))
	private void herosforge$nutricion(Level level, LivingEntity quien, ItemStack comida, Consumable consumible, CallbackInfo info) {
		if (quien instanceof ServerPlayer jugador) {
			Supervivencia.nutrir(jugador, (FoodProperties) (Object) this);
		}
	}
}
