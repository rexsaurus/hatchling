package com.rexsaurus.hatchling.entity;

import com.rexsaurus.hatchling.Hatchling;
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
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class HatchlingEntity extends PathAwareEntity {
	private static final String INFECTION_TICKS_KEY = "InfectionTicks";
	private static final String GENERATION_KEY = "Generation";

	private int infectionTicks;
	private int generation;

	public HatchlingEntity(EntityType<? extends HatchlingEntity> entityType, World world) {
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

	public static boolean isValidHostShape(LivingEntity entity) {
		if (entity == null || !entity.isAlive()) {
			return false;
		}
		if (entity instanceof AlienEntity) {
			return false;
		}
		return isAllowedHostType(entity);
	}

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
		// M10 §1a — free-standing position probe (physics vs visual).
		if (!this.hasVehicle() && this.age % 20 == 0) {
			Hatchling.LOGGER.info("free hatchling y={} onGround={} blockBelowY={}",
					this.getY(), this.isOnGround(), this.getBlockPos().getY());
		}

		Entity host = this.getVehicle();
		if (host instanceof LivingEntity living && isValidHostShape(living)) {
			infectionTicks++;
			if (infectionTicks == 1) {
				Hatchling.LOGGER.info("rider y={} vehicle y={} delta={}",
						this.getY(), host.getY(), this.getY() - host.getY());
			}
			int total = cfg.lifecycle.incubationTicks;
			float progress = total <= 0 ? 1.0f : (float) infectionTicks / (float) total;

			if (infectionTicks % 20 == 0) {
				Hatchling.LOGGER.debug("infectionTicks={} / {}", infectionTicks, total);
			}

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

		double hostX = host.getX();
		double hostY = host.getY();
		double hostZ = host.getZ();
		float hostYaw = host.getYaw();
		float hostPitch = host.getPitch();
		Vec3d center = new Vec3d(hostX, hostY + host.getHeight() * 0.5, hostZ);

		Hatchling.LOGGER.info("BURST host={} alive={} pos={}", host.getType(), host.isAlive(), host.getPos());

		HatchlingConfig.Feedback feedback = HatchlingConfig.get().feedback;
		World world = this.getWorld();

		if (feedback.burstExplosionEnabled) {
			ExplosionSourceType sourceType = feedback.burstDamagesBlocks
					? ExplosionSourceType.MOB
					: ExplosionSourceType.NONE;
			world.createExplosion(this, hostX, hostY, hostZ,
					feedback.burstExplosionPower, sourceType);
		}

		applyBurstKnockback(world, center, host);

		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.CRIMSON_SPORE,
					center.x, center.y, center.z, 60, 0.4, 0.4, 0.4, 0.1);
			serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
					center.x, center.y, center.z, 1, 0.0, 0.0, 0.0, 0.0);
			serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
					center.x, center.y, center.z, 20, 0.35, 0.35, 0.35, 0.02);
		}

		world.playSound(null, hostX, hostY, hostZ,
				ModSounds.BURST, SoundCategory.HOSTILE, 1.0f, ModSounds.BURST_PITCH);
		world.playSound(null, hostX, hostY, hostZ,
				SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE,
				feedback.burstExplodeSoundVolume, feedback.burstExplodeSoundPitch);

		dropHostLoot(host);

		AlienEntity alien = null;
		if (host instanceof MobEntity mob && !mob.isRemoved()) {
			alien = mob.convertTo(ModEntities.ALIEN, false);
			Hatchling.LOGGER.info("BURST convertTo returned {}", alien);
		}

		if (alien == null) {
			alien = ModEntities.ALIEN.create(world);
			if (alien != null) {
				alien.refreshPositionAndAngles(hostX, hostY, hostZ, hostYaw, hostPitch);
				alien.setGeneration(this.generation);
				world.spawnEntity(alien);
				if (!host.isRemoved()) {
					host.discard();
				}
			}
			Hatchling.LOGGER.info("BURST manual spawn alien={}", alien);
		} else {
			alien.setGeneration(this.generation);
		}

		if (alien != null) {
			alien.setHealth(alien.getMaxHealth());
		}

		this.discard();
	}

	private void applyBurstKnockback(World world, Vec3d center, LivingEntity excludeHost) {
		HatchlingConfig.Feedback feedback = HatchlingConfig.get().feedback;
		double radius = feedback.burstKnockbackRadius;
		double strength = feedback.burstKnockbackStrength;
		if (radius <= 0 || strength <= 0) {
			return;
		}
		Box box = new Box(center, center).expand(radius);
		for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, e -> e != excludeHost && e.isAlive())) {
			if (entity instanceof AlienEntity) {
				continue;
			}
			Vec3d away = entity.getPos().subtract(center);
			double len = away.length();
			if (len < 1.0E-4) {
				away = new Vec3d(world.getRandom().nextGaussian(), 0.2, world.getRandom().nextGaussian());
				len = away.length();
			}
			Vec3d push = away.normalize().multiply(strength * (1.0 - Math.min(1.0, len / radius)));
			entity.addVelocity(push.x, Math.max(0.15, push.y + 0.2), push.z);
			entity.velocityModified = true;
		}
	}

	private void dropHostLoot(LivingEntity host) {
		if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
			return;
		}
		try {
			RegistryKey<LootTable> key = host.getLootTable();
			LootTable table = serverWorld.getServer().getReloadableRegistries().getLootTable(key);
			DamageSource source = this.getDamageSources().generic();
			LootContextParameterSet params = new LootContextParameterSet.Builder(serverWorld)
					.add(LootContextParameters.THIS_ENTITY, host)
					.add(LootContextParameters.ORIGIN, host.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, source)
					.build(LootContextTypes.ENTITY);
			table.generateLoot(params, host.getLootTableSeed(), host::dropStack);
		} catch (Exception e) {
			Hatchling.LOGGER.warn("Could not drop host loot table for {}; skipping rather than hardcoding items",
					host.getType().getTranslationKey(), e);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(INFECTION_TICKS_KEY, infectionTicks);
		nbt.putInt(GENERATION_KEY, generation);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		infectionTicks = nbt.getInt(INFECTION_TICKS_KEY);
		generation = nbt.getInt(GENERATION_KEY);
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

	/** Larvae must survive Peaceful for the acceptance lifecycle. */
	@Override
	protected boolean isDisallowedInPeaceful() {
		return false;
	}

	public int getInfectionTicks() {
		return infectionTicks;
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = Math.max(0, generation);
	}
}
