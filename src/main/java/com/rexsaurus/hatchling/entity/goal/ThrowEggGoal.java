package com.rexsaurus.hatchling.entity.goal;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.AlienEntity;
import com.rexsaurus.hatchling.entity.HatchlingEntity;
import com.rexsaurus.hatchling.entity.ThrownHatchlingEggEntity;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.util.PopulationCaps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

public class ThrowEggGoal extends Goal {
	private final AlienEntity alien;
	private int cooldown;
	private int windup;
	private LivingEntity target;

	public ThrowEggGoal(AlienEntity alien) {
		this.alien = alien;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
	}

	@Override
	public boolean canStart() {
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		if (!life.alienThrowsEggs) {
			return false;
		}
		if (alien.isDecaying()) {
			return false;
		}
		if (!PopulationCaps.canReproduce(alien)) {
			return false;
		}
		if (++cooldown < life.alienEggThrowIntervalTicks) {
			return false;
		}
		if (alien.getRandom().nextDouble() >= life.alienEggThrowChance) {
			Hatchling.LOGGER.debug("ThrowEggGoal: chance roll failed; resetting cooldown");
			cooldown = 0;
			return false;
		}
		target = findTarget();
		if (target == null) {
			Hatchling.LOGGER.debug("ThrowEggGoal: no valid target in range");
			cooldown = 0;
			return false;
		}
		windup = 0;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		return target != null
				&& target.isAlive()
				&& !alien.isDecaying()
				&& isValidThrowTarget(target)
				&& windup < life.alienEggThrowWindupTicks;
	}

	@Override
	public void stop() {
		target = null;
		windup = 0;
	}

	@Override
	public void tick() {
		if (target == null) {
			return;
		}
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		alien.getLookControl().lookAt(target, 30.0f, 30.0f);
		windup++;

		if (alien.getWorld() instanceof ServerWorld serverWorld
				&& life.alienEggThrowWindupTicks > 0
				&& HatchlingConfig.get().feedback.particlesEnabled) {
			Vec3d mouth = alien.getPos().add(0.0, alien.getStandingEyeHeight(), 0.0);
			serverWorld.spawnParticles(ParticleTypes.SCULK_SOUL,
					mouth.x, mouth.y, mouth.z, 2, 0.1, 0.1, 0.1, 0.01);
			serverWorld.spawnParticles(ParticleTypes.CRIMSON_SPORE,
					mouth.x, mouth.y, mouth.z, 1, 0.08, 0.08, 0.08, 0.0);
		}

		if (windup >= life.alienEggThrowWindupTicks) {
			throwEgg();
			cooldown = 0;
			target = null;
		}
	}

	private void throwEgg() {
		World world = alien.getWorld();
		if (world.isClient || target == null) {
			return;
		}
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		ThrownHatchlingEggEntity thrown = new ThrownHatchlingEggEntity(ModEntities.THROWN_HATCHLING_EGG, alien, world);
		thrown.setGeneration(alien.getGeneration() + 1);

		double dx = target.getX() - alien.getX();
		double dz = target.getZ() - alien.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double dy = (target.getBodyY(0.333) - thrown.getY()) + horizontal * life.alienEggThrowArcFactor;

		thrown.setVelocity(dx, dy, dz, (float) life.alienEggThrowVelocity, life.alienEggThrowInaccuracy);
		world.spawnEntity(thrown);
		world.playSound(null, alien.getX(), alien.getY(), alien.getZ(),
				SoundEvents.ENTITY_EGG_THROW, SoundCategory.HOSTILE, 0.8f, 0.7f);
	}

	/**
	 * Priority 1: player (if alienThrowsAtPlayers).
	 * Priority 2: valid uninfected animal via shared {@link HatchlingEntity#isValidHost}.
	 * Never target passengers (already infected) or aliens (isValidHost excludes them).
	 * Interval between throws: {@code alienEggThrowIntervalTicks} (cooldown in canStart).
	 */
	private LivingEntity findTarget() {
		double range = HatchlingConfig.get().lifecycle.alienEggThrowRange;
		Box box = alien.getBoundingBox().expand(range);
		World world = alien.getWorld();

		if (HatchlingConfig.get().targeting.alienThrowsAtPlayers) {
			LivingEntity bestPlayer = null;
			double bestDist = Double.MAX_VALUE;
			for (PlayerEntity player : world.getEntitiesByClass(PlayerEntity.class, box,
					p -> p.isAlive() && !p.isSpectator() && !p.hasPassengers() && alien.canSee(p))) {
				double d = alien.squaredDistanceTo(player);
				if (d < bestDist) {
					bestDist = d;
					bestPlayer = player;
				}
			}
			if (bestPlayer != null) {
				return bestPlayer;
			}
		}

		// isValidHost enforces whitelist + no passengers + not alien.
		List<LivingEntity> hosts = world.getEntitiesByClass(
				LivingEntity.class, box,
				e -> HatchlingEntity.isValidHost(e) && alien.canSee(e));
		LivingEntity best = null;
		double bestDist = Double.MAX_VALUE;
		for (LivingEntity candidate : hosts) {
			double d = alien.squaredDistanceTo(candidate);
			if (d < bestDist) {
				bestDist = d;
				best = candidate;
			}
		}
		return best;
	}

	private static boolean isValidThrowTarget(LivingEntity entity) {
		if (entity instanceof PlayerEntity player) {
			return HatchlingConfig.get().targeting.alienThrowsAtPlayers
					&& player.isAlive()
					&& !player.isSpectator()
					&& !player.hasPassengers();
		}
		return HatchlingEntity.isValidHost(entity);
	}
}
