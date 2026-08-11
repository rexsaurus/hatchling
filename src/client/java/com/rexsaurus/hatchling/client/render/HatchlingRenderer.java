package com.rexsaurus.hatchling.client.render;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.client.HatchlingClient;
import com.rexsaurus.hatchling.client.model.HatchlingModel;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.HatchlingEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Custom Blockbench hatchling (woodlouse) renderer.
 * Phase 1 silverfish kept in {@link HatchlingLegacyRenderer}.
 */
public class HatchlingRenderer extends MobEntityRenderer<HatchlingEntity, HatchlingModel<HatchlingEntity>> {
	private static final Identifier TEXTURE = Identifier.of(Hatchling.MOD_ID, "textures/entity/hatchling.png");

	public HatchlingRenderer(EntityRendererFactory.Context context) {
		super(context, new HatchlingModel<>(context.getPart(HatchlingClient.HATCHLING_LAYER)), 0.3f);
	}

	@Override
	public Identifier getTexture(HatchlingEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(HatchlingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		// Fine-tune only — structural ground contact is the model root pivot (y=24).
		double yOffset = HatchlingConfig.get().feedback.hatchlingRenderYOffset;
		if (yOffset != 0.0) {
			matrices.translate(0.0, yOffset, 0.0);
		}
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		matrices.pop();
	}
}
