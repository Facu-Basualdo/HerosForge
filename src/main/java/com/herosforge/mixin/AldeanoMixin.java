package com.herosforge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.herosforge.Oficios;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

@Mixin(Villager.class)
abstract class AldeanoMixin {
	// Después de los descuentos de vanilla (reputación y Héroe de la aldea). En 1.21.x startTrading manda las ofertas
	// después de updateSpecialPrices, así que el TAIL alcanza (en 26.x hay que entrar antes de mandarlas, en el método)
	@Inject(method = "updateSpecialPrices", at = @At("TAIL"))
	private void herosforge$regateo(Player comprador, CallbackInfo info) {
		if (comprador instanceof ServerPlayer jugador) {
			Oficios.regatear(jugador, ((Villager) (Object) this).getOffers());
		}
	}
}
