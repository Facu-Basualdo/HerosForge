package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.herosforge.Supervivencia;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

@Mixin(LivingEntity.class)
abstract class UsoMixin {
	@Shadow
	protected int useItemRemaining;

	@Shadow
	protected ItemStack useItem;

	// En el TAIL: los return de antes son los casos en que no empieza a usar nada. El cliente vanilla sigue
	// contando su tiempo, pero cuando el servidor termina le manda el evento 9 (ServerPlayer.completeUsingItem)
	@Inject(method = "startUsingItem", at = @At("TAIL"))
	private void herosforge$gloton(InteractionHand mano, CallbackInfo info) {
		if ((Object) this instanceof ServerPlayer jugador) {
			useItemRemaining = Supervivencia.tiempoDeComer(jugador, useItem, useItemRemaining);
		}
	}

	// addEffect(MobEffectInstance) también termina acá: es final y llama a éste con la entidad en null
	@ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
	private MobEffectInstance herosforge$tenacidad(MobEffectInstance efecto) {
		return (Object) this instanceof ServerPlayer jugador ? Supervivencia.efecto(jugador, efecto) : efecto;
	}
}
