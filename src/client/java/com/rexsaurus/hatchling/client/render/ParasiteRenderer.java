package com.rexsaurus.hatchling.client.render;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.ParasiteEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.SilverfishEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class ParasiteRenderer extends MobEntityRenderer<ParasiteEntity, SilverfishEntityModel<ParasiteEntity>> {
	private static final Identifier TEXTURE = Identifier.of(Hatchling.MOD_ID, "textures/entity/parasite.png");

	public ParasiteRenderer(EntityRendererFactory.Context context) {
		super(context, new SilverfishEntityModel<>(context.getPart(EntityModelLayers.SILVERFISH)), 0.3f);
	}

	@Override
	public Identifier getTexture(ParasiteEntity entity) {
		return TEXTURE;
	}

	@Override
	public void render(ParasiteEntity entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		if (entity.hasVehicle()) {
			// Vanilla already places passengers via EntityAttachmentType.PASSENGER.
			// Do not stack hostHeight*0.75 — that caused the larva to float ~1 block above the back.
			double yOffset = HatchlingConfig.get().feedback.larvaRenderYOffset;
			if (yOffset != 0.0) {
				matrices.translate(0.0, yOffset, 0.0);
			}
			matrices.scale(0.8f, 0.8f, 0.8f);
		}
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
		matrices.pop();
	}
}
