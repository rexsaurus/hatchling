package com.rexsaurus.hatchling.client.render;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.client.HatchlingClient;
import com.rexsaurus.hatchling.client.model.AlienModel;
import com.rexsaurus.hatchling.entity.AlienEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/**
 * Custom Blockbench alien renderer.
 * Phase 1 enderman kept in {@link AlienLegacyRenderer}.
 */
public class AlienRenderer extends MobEntityRenderer<AlienEntity, AlienModel<AlienEntity>> {
	private static final Identifier TEXTURE = Identifier.of(Hatchling.MOD_ID, "textures/entity/alien.png");

	public AlienRenderer(EntityRendererFactory.Context context) {
		super(context, new AlienModel<>(context.getPart(HatchlingClient.ALIEN_LAYER)), 0.5f);
	}

	@Override
	public Identifier getTexture(AlienEntity entity) {
		return TEXTURE;
	}
}
