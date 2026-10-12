package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.herosforge.Supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
abstract class HambreMixin {
	// Todo el cansancio pasa por acá (correr, saltar, nadar, pegar, minar): vanilla no tiene un atributo para eso
	@ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
	private float herosforge$cansancio(float cansancio) {
		return (Object) this instanceof ServerPlayer jugador ? Supervivencia.cansancio(jugador, cansancio) : cansancio;
	}
}
