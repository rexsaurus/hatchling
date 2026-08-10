package com.rexsaurus.hatchling.client;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.client.render.AlienRenderer;
import com.rexsaurus.hatchling.client.render.ParasiteRenderer;
import com.rexsaurus.hatchling.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class HatchlingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.PARASITE, ParasiteRenderer::new);
		EntityRendererRegistry.register(ModEntities.ALIEN, AlienRenderer::new);
		Hatchling.LOGGER.info("Hatchling client initialized.");
	}
}
