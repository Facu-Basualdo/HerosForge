package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import com.herosforge.Combate;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
abstract class DanioMixin {
	// Al principio, antes de la armadura y de los encantamientos: como si el golpe viniera más fuerte (o más débil).
	// Fabric API no tiene un evento para cambiar el daño, sólo para cancelarlo
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float herosforge$danio(float danio, @Local(argsOnly = true) DamageSource fuente) {
		return Combate.danio((LivingEntity) (Object) this, fuente, danio);
	}
}
