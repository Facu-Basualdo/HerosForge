package com.estadisticasrpg.cliente.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** {@code leftPos} es protegido, y el libro de recetas lo cambia: el botón del inventario lo sigue con esto. */
@Mixin(AbstractContainerScreen.class)
public interface AccesoAPantallaDeContenedor {
	@Accessor("leftPos")
	int estadisticasrpg$leftPos();
}
