package com.estadisticasrpg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.estadisticasrpg.Mejoras;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

// El tridente sobrescribe onHitEntity sin llamar al de AbstractArrow: hace falta inyectar en los dos
@Mixin({AbstractArrow.class, ThrownTrident.class})
abstract class ProyectilMixin {
	// Las flechas y el tridente lanzado no usan ATTACK_DAMAGE: sin esto, los puntos de daño no sirven a distancia.
	// Es el daño final (velocidad, Poder y crítico ya sumados), así el arco y la ballesta escalan igual
	@ModifyArg(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
	private float estadisticasrpg$danioADistancia(float danio) {
		return (float) (danio * Mejoras.multiplicadorADistancia(((AbstractArrow) (Object) this).getOwner()));
	}
}
