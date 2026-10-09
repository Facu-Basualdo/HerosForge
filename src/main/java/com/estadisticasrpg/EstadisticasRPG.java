package com.estadisticasrpg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

public class EstadisticasRPG implements ModInitializer {
	public static final String MOD_ID = "estadisticasrpg";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Fase 1 (docs/plan.md): registrar el attachment PUNTOS, JOIN, COPY_FROM y el comando.
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
