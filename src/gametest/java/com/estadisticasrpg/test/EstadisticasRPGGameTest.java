package com.estadisticasrpg.test;

import com.estadisticasrpg.EstadisticasRPG;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

public class EstadisticasRPGGameTest {
	@GameTest
	public void modCargado(GameTestHelper helper) {
		helper.assertTrue(FabricLoader.getInstance().isModLoaded(EstadisticasRPG.MOD_ID), Component.literal("El mod tiene que estar cargado"));
		helper.succeed();
	}
}
