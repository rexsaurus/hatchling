package com.rexsaurus.hatchling.entity.goal;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.HatchlingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;

import java.util.EnumSet;

public class SeekHostGoal extends Goal {
	private final HatchlingEntity hatchling;
	private LivingEntity target;
	private final TargetPredicate hostPredicate;

	public SeekHostGoal(HatchlingEntity hatchling) {
		this.hatchling = hatchling;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		this.hostPredicate = TargetPredicate.createAttackable()
				.setPredicate(HatchlingEntity::isValidHost);
	}

	@Override
	public boolean canStart() {
		if (hatchling.hasVehicle()) {
			return false;
		}
		target = findHost();
		return target != null;
	}

	@Override
	public boolean shouldContinue() {
		return !hatchling.hasVehicle()
				&& target != null
				&& target.isAlive()
				&& HatchlingEntity.isValidHost(target);
	}

	@Override
	public void stop() {
		target = null;
		hatchling.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}
		hatchling.getLookControl().lookAt(target, 30.0f, 30.0f);
		double chase = HatchlingConfig.get().stats.larvaChaseSpeedMultiplier;
		hatchling.getNavigation().startMovingTo(target, chase);

		double latch = HatchlingConfig.get().lifecycle.larvaLatchDistance;
		if (hatchling.squaredDistanceTo(target) < latch * latch) {
			hatchling.startRiding(target, true);
		}
	}

	private LivingEntity findHost() {
		double radius = HatchlingConfig.get().lifecycle.larvaHostSearchRadius;
		Box box = hatchling.getBoundingBox().expand(radius);
		return hatchling.getWorld().getClosestEntity(
				LivingEntity.class,
				hostPredicate.setBaseMaxDistance(radius),
				hatchling,
				hatchling.getX(), hatchling.getY(), hatchling.getZ(),
				box);
	}
}
