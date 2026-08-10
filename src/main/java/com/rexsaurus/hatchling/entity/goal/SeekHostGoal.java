package com.rexsaurus.hatchling.entity.goal;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.ParasiteEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;

import java.util.EnumSet;

public class SeekHostGoal extends Goal {
	private final ParasiteEntity parasite;
	private LivingEntity target;
	private final TargetPredicate hostPredicate;

	public SeekHostGoal(ParasiteEntity parasite) {
		this.parasite = parasite;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
		this.hostPredicate = TargetPredicate.createAttackable()
				.setPredicate(ParasiteEntity::isValidHost);
	}

	@Override
	public boolean canStart() {
		if (parasite.hasVehicle()) {
			return false;
		}
		target = findHost();
		return target != null;
	}

	@Override
	public boolean shouldContinue() {
		return !parasite.hasVehicle()
				&& target != null
				&& target.isAlive()
				&& ParasiteEntity.isValidHost(target);
	}

	@Override
	public void stop() {
		target = null;
		parasite.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}
		parasite.getLookControl().lookAt(target, 30.0f, 30.0f);
		double chase = HatchlingConfig.get().stats.larvaChaseSpeedMultiplier;
		parasite.getNavigation().startMovingTo(target, chase);

		double latch = HatchlingConfig.get().lifecycle.larvaLatchDistance;
		if (parasite.squaredDistanceTo(target) < latch * latch) {
			parasite.startRiding(target, true);
		}
	}

	private LivingEntity findHost() {
		double radius = HatchlingConfig.get().lifecycle.larvaHostSearchRadius;
		Box box = parasite.getBoundingBox().expand(radius);
		return parasite.getWorld().getClosestEntity(
				LivingEntity.class,
				hostPredicate.setBaseMaxDistance(radius),
				parasite,
				parasite.getX(), parasite.getY(), parasite.getZ(),
				box);
	}
}
