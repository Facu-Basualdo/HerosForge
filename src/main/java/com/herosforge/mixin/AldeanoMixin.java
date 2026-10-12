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
	// Después de los descuentos de vanilla (reputación y Héroe de la aldea) y antes de mandarle las ofertas al
	// jugador: en el TAIL, el cliente vería los precios sin el regateo
	@Inject(method = "updateSpecialPrices", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;getTradingPlayer()Lnet/minecraft/world/entity/player/Player;"))
	private void herosforge$regateo(Player comprador, CallbackInfo info) {
		if (comprador instanceof ServerPlayer jugador) {
			Oficios.regatear(jugador, ((Villager) (Object) this).getOffers());
		}
	}
}
