package com.rexsaurus.hatchling.client;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.client.model.AlienModel;
import com.rexsaurus.hatchling.client.model.HatchlingModel;
import com.rexsaurus.hatchling.client.render.AlienLegacyRenderer;
import com.rexsaurus.hatchling.client.render.AlienRenderer;
import com.rexsaurus.hatchling.client.render.HatchlingLegacyRenderer;
import com.rexsaurus.hatchling.client.render.HatchlingRenderer;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class HatchlingClient implements ClientModInitializer {
	public static final EntityModelLayer HATCHLING_LAYER =
			new EntityModelLayer(Identifier.of(Hatchling.MOD_ID, "hatchling"), "main");
	public static final EntityModelLayer ALIEN_LAYER =
			new EntityModelLayer(Identifier.of(Hatchling.MOD_ID, "alien"), "main");

	@Override
	public void onInitializeClient() {
		EntityModelLayerRegistry.registerModelLayer(HATCHLING_LAYER, HatchlingModel::getTexturedModelData);
		EntityModelLayerRegistry.registerModelLayer(ALIEN_LAYER, AlienModel::getTexturedModelData);

		// useCustomModels is read at registration only — restart required (not /hatchling reload).
		boolean custom = HatchlingConfig.get().feedback.useCustomModels;
		if (custom) {
			EntityRendererRegistry.register(ModEntities.HATCHLING, HatchlingRenderer::new);
			EntityRendererRegistry.register(ModEntities.ALIEN, AlienRenderer::new);
		} else {
			EntityRendererRegistry.register(ModEntities.HATCHLING, HatchlingLegacyRenderer::new);
			EntityRendererRegistry.register(ModEntities.ALIEN, AlienLegacyRenderer::new);
		}
		EntityRendererRegistry.register(ModEntities.THROWN_HATCHLING_EGG, FlyingItemEntityRenderer::new);
		Hatchling.LOGGER.info("Hatchling client initialized (useCustomModels={}).", custom);
	}
}
