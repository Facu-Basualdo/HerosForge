package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import com.herosforge.Combate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Los dos calculan la carga al soltar como getUseDuration − tiempoRestante: se achica el tiempo restante
@Mixin({BowItem.class, CrossbowItem.class})
abstract class TensarMixin {
	@ModifyVariable(method = "releaseUsing", at = @At("HEAD"), argsOnly = true)
	private int herosforge$destreza(int tiempoRestante, @Local(argsOnly = true) ItemStack arma, @Local(argsOnly = true) LivingEntity quien) {
		return Combate.tensar(quien, (Item) (Object) this, arma, tiempoRestante);
	}
}
