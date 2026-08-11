package com.rexsaurus.hatchling.client.model;

import com.rexsaurus.hatchling.entity.HatchlingEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;

public class HatchlingModel<T extends HatchlingEntity> extends SinglePartEntityModel<T> {
	private final ModelPart root;
	private final ModelPart tail;
	private final ModelPart head;
	private final ModelPart antenna;
	private final ModelPart leftAntenna;
	private final ModelPart rightAntenna;
	private final ModelPart leftLegs;
	private final ModelPart leftLeg;
	private final ModelPart leftLeg2;
	private final ModelPart leftLeg3;
	private final ModelPart rightLegs;
	private final ModelPart rightLeg;
	private final ModelPart rightLeg2;
	private final ModelPart rightLeg3;

	public HatchlingModel(ModelPart root) {
		this.root = root.getChild("root");
		this.tail = this.root.getChild("tail");
		this.head = this.root.getChild("head");
		this.antenna = this.head.getChild("antenna");
		this.leftAntenna = this.antenna.getChild("left_antenna");
		this.rightAntenna = this.antenna.getChild("right_antenna");
		this.leftLegs = this.root.getChild("left_legs");
		this.leftLeg = this.leftLegs.getChild("left_leg");
		this.leftLeg2 = this.leftLegs.getChild("left_leg2");
		this.leftLeg3 = this.leftLegs.getChild("left_leg3");
		this.rightLegs = this.root.getChild("right_legs");
		this.rightLeg = this.rightLegs.getChild("right_leg");
		this.rightLeg2 = this.rightLegs.getChild("right_leg2");
		this.rightLeg3 = this.rightLegs.getChild("right_leg3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		ModelPartData root = modelPartData.addChild("root", ModelPartBuilder.create()
				.uv(18, 0).cuboid(-2.5F, -3.0F, 1.0F, 5.0F, 4.0F, 3.0F, new Dilation(0.0F))
				.uv(0, 0).cuboid(-2.5F, -3.0F, 4.0F, 5.0F, 4.0F, 4.0F, new Dilation(0.0F))
				.uv(18, 7).cuboid(-2.5F, -2.0F, 8.0F, 5.0F, 3.0F, 3.0F, new Dilation(0.0F))
				.uv(12, 29).cuboid(2.5F, 0.0F, 2.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(18, 18).cuboid(2.5F, 0.0F, 4.0F, 2.0F, 1.0F, 4.0F, new Dilation(0.0F))
				.uv(20, 29).cuboid(2.5F, 0.0F, 8.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(30, 13).cuboid(-4.5F, 0.0F, 8.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(0, 20).cuboid(-4.5F, 0.0F, 4.0F, 2.0F, 1.0F, 4.0F, new Dilation(0.0F))
				.uv(30, 16).cuboid(-4.5F, 0.0F, 2.0F, 2.0F, 1.0F, 2.0F, new Dilation(0.0F))
				.uv(12, 20).cuboid(-3.5F, 0.0F, 10.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(30, 19).cuboid(2.5F, 0.0F, 10.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(30, 21).cuboid(2.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F))
				.uv(12, 32).cuboid(-3.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, new Dilation(0.0F)),
				// Ground-plane convention: root at y=24 so local y=0 is at the feet.
				// M9 wrongly used 4.0, which floated the whole mesh ~1.25 blocks.
				ModelTransform.pivot(0.0F, 24.0F, 0.0F));

		ModelPartData tail = root.addChild("tail", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.575F, 10.65F));

		tail.addChild("cube_r1", ModelPartBuilder.create()
				.uv(0, 25).cuboid(-1.5F, -0.5F, 0.0F, 4.0F, 2.0F, 1.0F, new Dilation(0.0F)),
				ModelTransform.of(-0.5F, 0.5F, 0.0F, 0.7854F, 0.0F, 0.0F));

		ModelPartData head = root.addChild("head", ModelPartBuilder.create()
				.uv(18, 13).cuboid(-2.0F, -1.5F, -2.0F, 4.0F, 3.0F, 2.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, -0.5F, 1.0F));

		ModelPartData antenna = head.addChild("antenna", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -1.5F, -2.0F));

		antenna.addChild("left_antenna", ModelPartBuilder.create()
				.uv(0, 14).cuboid(0.0F, 0.0F, -6.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		antenna.addChild("right_antenna", ModelPartBuilder.create()
				.uv(0, 8).cuboid(-3.0F, 0.0F, -6.0F, 3.0F, 0.0F, 6.0F, new Dilation(0.0F)),
				ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData leftLegs = root.addChild("left_legs", ModelPartBuilder.create(), ModelTransform.pivot(2.5F, 1.0F, 6.0F));

		ModelPartData leftLeg = leftLegs.addChild("left_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, -3.5F));

		leftLeg.addChild("cube_r2", ModelPartBuilder.create()
				.uv(6, 28).cuboid(0.0F, 0.0F, -4.0F, 0.0F, 2.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, 2.5F, 0.0F, 0.0F, -0.7854F));

		ModelPartData leftLeg2 = leftLegs.addChild("left_leg2", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		leftLeg2.addChild("cube_r3", ModelPartBuilder.create()
				.uv(12, 23).cuboid(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, -0.7854F));

		ModelPartData leftLeg3 = leftLegs.addChild("left_leg3", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 3.5F));

		leftLeg3.addChild("cube_r4", ModelPartBuilder.create()
				.uv(0, 28).cuboid(0.0F, 0.0F, 2.0F, 0.0F, 2.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, -3.5F, 0.0F, 0.0F, -0.7854F));

		ModelPartData rightLegs = root.addChild("right_legs", ModelPartBuilder.create(), ModelTransform.pivot(-2.5F, 1.0F, 6.0F));

		ModelPartData rightLeg = rightLegs.addChild("right_leg", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, -3.5F));

		rightLeg.addChild("cube_r5", ModelPartBuilder.create()
				.uv(28, 23).cuboid(0.0F, 0.0F, -7.0F, 0.0F, 2.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, 5.5F, 0.0F, 0.0F, 0.7854F));

		ModelPartData rightLeg2 = rightLegs.addChild("right_leg2", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		rightLeg2.addChild("cube_r6", ModelPartBuilder.create()
				.uv(20, 23).cuboid(0.0F, 0.0F, -4.0F, 0.0F, 2.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.7854F));

		ModelPartData rightLeg3 = rightLegs.addChild("right_leg3", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 3.5F));

		rightLeg3.addChild("cube_r7", ModelPartBuilder.create()
				.uv(28, 28).cuboid(0.0F, 0.0F, -1.0F, 0.0F, 2.0F, 3.0F, new Dilation(0.0F)),
				ModelTransform.of(0.0F, 0.0F, -0.5F, 0.0F, 0.0F, 0.7854F));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public ModelPart getPart() {
		return this.root;
	}

	@Override
	public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
		boolean mounted = entity.hasVehicle();
		float walkAmp = mounted ? 0.02F : 1.0F;

		float leftPhase = MathHelper.cos(limbAngle * 0.6662F) * limbDistance * 0.8F * walkAmp;
		float rightPhase = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * limbDistance * 0.8F * walkAmp;

		this.leftLeg.pitch = leftPhase;
		this.leftLeg2.pitch = -leftPhase * 0.85F;
		this.leftLeg3.pitch = leftPhase * 0.7F;
		this.rightLeg.pitch = rightPhase;
		this.rightLeg2.pitch = -rightPhase * 0.85F;
		this.rightLeg3.pitch = rightPhase * 0.7F;

		this.leftLeg.roll = 0.0F;
		this.leftLeg2.roll = 0.0F;
		this.leftLeg3.roll = 0.0F;
		this.rightLeg.roll = 0.0F;
		this.rightLeg2.roll = 0.0F;
		this.rightLeg3.roll = 0.0F;

		if (mounted) {
			float tuck = 0.6F;
			this.leftLeg.roll = tuck;
			this.leftLeg2.roll = tuck;
			this.leftLeg3.roll = tuck;
			this.rightLeg.roll = -tuck;
			this.rightLeg2.roll = -tuck;
			this.rightLeg3.roll = -tuck;
		}

		this.root.yaw = MathHelper.sin(animationProgress * 0.12F) * 0.04F;
		this.root.pitch = MathHelper.sin(animationProgress * 0.09F) * 0.03F;
		this.tail.pitch = MathHelper.sin(animationProgress * 0.18F) * 0.2F;
		this.tail.yaw = MathHelper.cos(animationProgress * 0.14F) * 0.08F;
		this.antenna.pitch = MathHelper.sin(animationProgress * 0.22F) * 0.12F;
		this.leftAntenna.yaw = MathHelper.sin(animationProgress * 0.25F) * 0.1F;
		this.rightAntenna.yaw = -MathHelper.sin(animationProgress * 0.25F + 0.4F) * 0.1F;
		this.head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.35F;
		this.head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE * 0.35F;
	}
}
