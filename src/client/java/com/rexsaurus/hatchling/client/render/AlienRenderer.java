package com.rexsaurus.hatchling.client.render;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.entity.AlienEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class AlienRenderer extends BipedEntityRenderer<AlienEntity, EndermanEntityModel<AlienEntity>> {
	private static final Identifier TEXTURE = Identifier.of(Hatchling.MOD_ID, "textures/entity/alien.png");

	public AlienRenderer(EntityRendererFactory.Context context) {
		super(context, new EndermanEntityModel<>(context.getPart(EntityModelLayers.ENDERMAN)), 0.5f);
	}

	@Override
	public Identifier getTexture(AlienEntity entity) {
		return TEXTURE;
	}
}
