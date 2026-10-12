package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import com.herosforge.Oficios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

@Mixin(ExperienceOrb.class)
abstract class OrbeMixin {
	// Lo que queda del orbe después de reparar con Reparación, justo antes de giveExperiencePoints. No se toca
	// giveExperiencePoints: Mejoras.bajar también lo usa para devolver experiencia, y ahí no tiene que sumar nada
	@ModifyVariable(method = "playerTouch", at = @At("STORE"), ordinal = 0)
	private int herosforge$sabiduria(int experiencia, @Local(argsOnly = true) Player quien) {
		return quien instanceof ServerPlayer jugador ? Oficios.sabiduria(jugador, experiencia) : experiencia;
	}
}
