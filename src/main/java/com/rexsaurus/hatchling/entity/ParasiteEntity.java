package com.rexsaurus.hatchling.entity;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.goal.SeekHostGoal;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.registry.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ParasiteEntity extends PathAwareEntity {
	private static final String INFECTION_TICKS_KEY = "InfectionTicks";

	private int infectionTicks;

	public ParasiteEntity(EntityType<? extends ParasiteEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		HatchlingConfig.Stats stats = HatchlingConfig.get().stats;
		return PathAwareEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, stats.larvaHealth)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, stats.larvaSpeed);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new SeekHostGoal(this));
		this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
	}

	public static boolean isValidHost(LivingEntity entity) {
		if (entity == null || !entity.isAlive()) {
			return false;
		}
		if (entity instanceof AlienEntity) {
			return false;
		}
		if (entity.hasPassengers()) {
			return false;
		}
		return isAllowedHostType(entity);
	}

	/** Shape check for an already-mounted host (passengers expected). */
	public static boolean isValidHostShape(LivingEntity entity) {
		if (entity == null || !entity.isAlive()) {
			return false;
		}
		if (entity instanceof AlienEntity) {
			return false;
		}
		return isAllowedHostType(entity);
	}

	/**
	 * Single place for whitelist/blacklist type rules used by SeekHostGoal and latch.
	 * Non-empty whitelist wins and ignores blacklist; empty whitelist uses blacklist + animal/player rules.
	 */
	private static boolean isAllowedHostType(LivingEntity entity) {
		HatchlingConfig cfg = HatchlingConfig.get();
		if (cfg.hasHostWhitelist()) {
			return cfg.getHostWhitelistTypes().contains(entity.getType());
		}
		HatchlingConfig.Targeting targeting = cfg.targeting;
		boolean typeOk = entity instanceof AnimalEntity
				|| (targeting.infectPlayers && entity instanceof PlayerEntity);
		if (!typeOk) {
			return false;
		}
		return !cfg.getHostBlacklistTypes().contains(entity.getType());
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) {
			return;
		}

		HatchlingConfig cfg = HatchlingConfig.get();
		Entity host = this.getVehicle();
		if (host instanceof LivingEntity living && isValidHostShape(living)) {
			infectionTicks++;
			int total = cfg.lifecycle.incubationTicks;
			float progress = total <= 0 ? 1.0f : (float) infectionTicks / (float) total;

			if (cfg.feedback.particlesEnabled && infectionTicks % 20 == 0 && this.getWorld() instanceof ServerWorld serverWorld) {
				Vec3d center = living.getPos().add(0.0, living.getHeight() * 0.5, 0.0);
				serverWorld.spawnParticles(ParticleTypes.SCULK_SOUL,
						center.x, center.y, center.z,
						3, 0.15, 0.2, 0.15, 0.01);
			}

			int sicknessAt = (int) (total * cfg.lifecycle.sicknessOnsetFraction);
			if (infectionTicks == sicknessAt) {
				int remaining = Math.max(1, total - infectionTicks);
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, remaining, 1));
				living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, remaining, 0));
			}

			if (cfg.feedback.heartbeatSoundEnabled) {
				int interval = Math.max(1, Math.round(MathHelper.lerp(progress, 40.0f, 8.0f)));
				if (infectionTicks % interval == 0) {
					this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
							ModSounds.HEARTBEAT, SoundCategory.HOSTILE,
							0.8f, ModSounds.HEARTBEAT_PITCH);
				}
			}

			if (infectionTicks >= total) {
				burst(living);
			}
		} else {
			infectionTicks = 0;
		}
	}

	private void burst(LivingEntity host) {
		if (this.getWorld().isClient) {
			return;
		}

		this.stopRiding();

		if (this.getWorld() instanceof ServerWorld serverWorld) {
			Vec3d center = host.getPos().add(0.0, host.getHeight() * 0.5, 0.0);
			serverWorld.spawnParticles(ParticleTypes.CRIMSON_SPORE,
					center.x, center.y, center.z,
					60, 0.4, 0.4, 0.4, 0.1);
		}

		this.getWorld().playSound(null, host.getX(), host.getY(), host.getZ(),
				ModSounds.BURST, SoundCategory.HOSTILE, 1.0f, ModSounds.BURST_PITCH);

		AlienEntity alien = null;
		if (host instanceof MobEntity mob) {
			alien = mob.convertTo(ModEntities.ALIEN, false);
		}

		if (alien == null) {
			alien = ModEntities.ALIEN.create(this.getWorld());
			if (alien != null) {
				alien.refreshPositionAndAngles(host.getX(), host.getY(), host.getZ(),
						host.getYaw(), host.getPitch());
				this.getWorld().spawnEntity(alien);
				host.discard();
			}
		}

		if (alien != null) {
			alien.setHealth(alien.getMaxHealth());
		}

		this.discard();
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(INFECTION_TICKS_KEY, infectionTicks);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		infectionTicks = nbt.getInt(INFECTION_TICKS_KEY);
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return !this.hasVehicle() && super.isPushable();
	}

	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
		return false;
	}

	public int getInfectionTicks() {
		return infectionTicks;
	}
}
