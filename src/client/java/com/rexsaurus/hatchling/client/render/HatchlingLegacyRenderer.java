package com.rexsaurus.hatchling.client.render;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.HatchlingEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SilverfishEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Phase 1 stand-in — silverfish geometry. Selected when useCustomModels=false. */
public class HatchlingLegacyRenderer extends MobEntityRenderer<HatchlingEntity, SilverfishEntityModel<HatchlingEntity>> {
	private static final Identifier TEXTURE = Identifier.of(Hatchling.MOD_ID, "textures/entity/hatchling.png");

	public HatchlingLegacyRenderer(EntityRendererFactory.Context context) {
		super(context, new SilverfishEntityModel<>(context.getPart(EntityModelLayers.SILVERFISH)), 0.3f);
	}

	@Override
	public Identifier getTexture(HatchlingEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(HatchlingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		if (entity.hasVehicle()) {
			double yOffset = HatchlingConfig.get().feedback.hatchlingRenderYOffset;
			if (yOffset != 0.0) {
				matrices.translate(0.0, yOffset, 0.0);
			}
			matrices.scale(0.8f, 0.8f, 0.8f);
		}
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		matrices.pop();
	}
}
