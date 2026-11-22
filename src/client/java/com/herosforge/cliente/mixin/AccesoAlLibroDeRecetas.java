package com.herosforge.cliente.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

/** En 1.21.x la pantalla no dice si se está escribiendo en el buscador del libro de recetas: se mira el buscador. */
@Mixin(AbstractRecipeBookScreen.class)
public interface AccesoAlLibroDeRecetas {
	@Accessor("recipeBookComponent")
	RecipeBookComponent<?> herosforge$libro();
}
