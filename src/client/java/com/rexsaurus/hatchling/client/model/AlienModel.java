package com.rexsaurus.hatchling.client.model;

import com.rexsaurus.hatchling.entity.AlienEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class AlienModel<T extends AlienEntity> extends SinglePartEntityModel<T> {
	private final ModelPart body;
	private final ModelPart rightLeg;
	private final ModelPart leftLeg;
	private final ModelPart lowerTorso;
	private final ModelPart upperTorso;
	private final ModelPart wings;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public AlienModel(ModelPart root) {
		this.body = root.getChild("Entity");
		this.rightLeg = this.body.getChild("RightLeg");
		this.leftLeg = this.body.getChild("LeftLeg");
		this.lowerTorso = this.body.getChild("LowerTorso");
		this.upperTorso = this.lowerTorso.getChild("UpperTorso");
		this.wings = this.upperTorso.getChild("Wings");
		this.head = this.upperTorso.getChild("Head");
		this.rightArm = this.upperTorso.getChild("RightArm");
		this.leftArm = this.upperTorso.getChild("LeftArm");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		ModelPartData entity = modelPartData.addChild("Entity", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 24.0F, -1.75F));

		ModelPartData rightLeg = entity.addChild("RightLeg", ModelPartBuilder.create()
				.uv(24, 35).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 24.0F, 4.0F, new Dilation(0.0F))
				.uv(56, 21).cuboid(-2.0F, 26.65F, -0.25F, 4.0F, 11.25F, 4.0F, new Dilation(0.0F)),
				ModelTransform.pivot(-2.5F, -38.0F, 0.0F));

		rightLeg.addChild("cube_r1", ModelPartBuilder.create()
				.uv(0, 57).cuboid(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 25.25F, 0.85F, 0.4363F, 0.0F, 0.0F));

		ModelPartData leftLeg = entity.addChild("LeftLeg", ModelPartBuilder.create()
				.uv(40, 35).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 24.0F, 4.0F, new Dilation(0.0F))
				.uv(56, 36).cuboid(-2.0F, 26.65F, -0.25F, 4.0F, 11.25F, 4.0F, new Dilation(0.0F)),
				ModelTransform.pivot(2.5F, -38.0F, 0.0F));

		leftLeg.addChild("cube_r2", ModelPartBuilder.create()
				.uv(58, 0).cuboid(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 25.25F, 0.85F, 0.4363F, 0.0F, 0.0F));

		ModelPartData lowerTorso = entity.addChild("LowerTorso", ModelPartBuilder.create()
				.uv(32, 0).cuboid(-4.0F, -10.0F, -2.5F, 8.0F, 9.0F, 5.0F, new Dilation(0.0F))
				.uv(56, 14).cuboid(-5.0F, -1.0F, -3.0F, 10.0F, 1.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, -38.0F, 0.0F));

		ModelPartData upperTorso = lowerTorso.addChild("UpperTorso", ModelPartBuilder.create()
				.uv(24, 20).cuboid(-5.0F, -9.0F, -3.0F, 10.0F, 9.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, -10.0F, 0.0F));

		ModelPartData wings = upperTorso.addChild("Wings", ModelPartBuilder.create()
				.uv(40, 18).cuboid(-1.0F, -2.0F, 0.3F, 2.0F, 2.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, -3.5F, 3.0F));

		wings.addChild("cube_r3", ModelPartBuilder.create()
				.uv(0, 0).cuboid(-0.5F, -15.0F, 0.0F, 20.0F, 15.0F, 0.0F, new Dilation(0.0F))
				.uv(0, 0).cuboid(-20.5F, -15.0F, 0.0F, 20.0F, 15.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.of(0.5F, 0.0F, 0.0F, -0.3054F, 0.0F, 0.0F));

		upperTorso.addChild("Head", ModelPartBuilder.create()
				.uv(50, 14).cuboid(4.0F, -9.9167F, -3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F))
				.uv(0, 0).cuboid(-4.0F, -11.9167F, -4.0F, 8.0F, 12.0F, 8.0F, new Dilation(0.0F))
				.uv(56, 51).cuboid(-4.0F, -11.9167F, -4.5F, 8.0F, 12.0F, 0.0F, new Dilation(0.0F))
				.uv(16, 63).cuboid(-7.0F, -14.9167F, -3.0F, 3.0F, 6.0F, 0.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, -9.0833F, 0.0F));

		upperTorso.addChild("RightArm", ModelPartBuilder.create()
				.uv(0, 20).cuboid(-3.0F, -1.25F, -1.5F, 3.0F, 34.0F, 3.0F, new Dilation(0.0F))
				.uv(40, 14).cuboid(-3.0F, -2.25F, -1.5F, 2.0F, 1.0F, 3.0F, new Dilation(0.0F))
				.uv(22, 63).cuboid(-1.0F, 32.75F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
				.uv(32, 14).cuboid(-3.0F, 32.75F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F))
				.uv(30, 63).cuboid(-2.0F, 35.75F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.pivot(-5.0F, -7.0F, 0.0F));

		upperTorso.addChild("LeftArm", ModelPartBuilder.create()
				.uv(12, 20).cuboid(0.0F, -1.25F, -1.5F, 3.0F, 34.0F, 3.0F, new Dilation(0.0F))
				.uv(58, 9).cuboid(1.0F, -2.25F, -1.5F, 2.0F, 1.0F, 3.0F, new Dilation(0.0F))
				.uv(16, 57).cuboid(2.0F, 32.75F, -1.5F, 1.0F, 3.0F, 3.0F, new Dilation(0.0F))
				.uv(38, 63).cuboid(0.0F, 32.75F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F))
				.uv(46, 63).cuboid(1.0F, 35.75F, -1.5F, 1.0F, 1.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.pivot(5.0F, -7.0F, 0.0F));

		return TexturedModelData.of(modelData, 128, 128);
	}

	@Override
	public ModelPart getPart() {
		return this.body;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		float leftLegSwing = MathHelper.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
		float rightLegSwing = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.4F * limbDistance;

		this.leftLeg.pitch = leftLegSwing;
		this.rightLeg.pitch = rightLegSwing;
		this.leftArm.pitch = rightLegSwing * 0.85F;
		this.rightArm.pitch = leftLegSwing * 0.85F;

		this.head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
		this.head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;

		this.upperTorso.yaw = MathHelper.sin(animationProgress * 0.08F) * 0.03F;
		this.upperTorso.pitch = MathHelper.sin(animationProgress * 0.06F) * 0.02F;
		this.wings.pitch = MathHelper.sin(animationProgress * 0.15F) * 0.06F;
		this.wings.yaw = MathHelper.cos(animationProgress * 0.12F) * 0.04F;
	}
}
