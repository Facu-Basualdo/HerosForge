package com.herosforge.cliente.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

/** Null hasta que se abre el libro por primera vez. */
@Mixin(RecipeBookComponent.class)
public interface AccesoAlBuscadorDeRecetas {
	@Accessor("searchBox")
	@Nullable EditBox herosforge$buscador();
}
