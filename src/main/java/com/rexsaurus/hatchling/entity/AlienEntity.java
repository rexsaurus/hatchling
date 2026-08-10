package com.rexsaurus.hatchling.entity;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.goal.LayEggGoal;
import com.rexsaurus.hatchling.entity.goal.ThrowEggGoal;
import com.rexsaurus.hatchling.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class AlienEntity extends HostileEntity {
	private static final String GENERATION_KEY = "Generation";
	private static final String AGE_TICKS_KEY = "AgeTicks";
	private static final String LIFESPAN_TICKS_KEY = "LifespanTicks";

	private int generation;
	private int ageTicks;
	/** Rolled once on first spawn; stable across reloads. 0 means not yet rolled. */
	private int lifespanTicks;
	private boolean decayApplied;

	public AlienEntity(EntityType<? extends AlienEntity> entityType, World world) {
		super(entityType, world);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		HatchlingConfig.Stats stats = HatchlingConfig.get().stats;
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, stats.alienHealth)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, stats.alienSpeed)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, stats.alienAttackDamage)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, stats.alienFollowRange);
	}

	@Override
	protected void initGoals() {
		HatchlingConfig.Stats stats = HatchlingConfig.get().stats;
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1, false));
		this.goalSelector.add(2, new ThrowEggGoal(this));
		this.goalSelector.add(3, new LayEggGoal(this));
		this.goalSelector.add(4, new WanderAroundFarGoal(this, stats.alienWanderSpeed));
		this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		this.goalSelector.add(6, new LookAroundGoal(this));

		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
		if (HatchlingConfig.get().targeting.alienTargetsAnimals) {
			this.targetSelector.add(3, new ActiveTargetGoal<>(this, AnimalEntity.class, false));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.getWorld().isClient) {
			return;
		}
		ensureLifespanRolled();
		HatchlingConfig.Limits limits = HatchlingConfig.get().limits;
		if (!limits.alienLifespanEnabled || lifespanTicks <= 0) {
			return;
		}
		ageTicks++;
		if (isDecaying()) {
			applyDecayEffects();
		}
		if (ageTicks >= lifespanTicks) {
			dieOfAge();
		}
	}

	private void ensureLifespanRolled() {
		if (lifespanTicks > 0) {
			return;
		}
		HatchlingConfig.Limits limits = HatchlingConfig.get().limits;
		int variance = Math.max(0, limits.alienLifespanVarianceTicks);
		int roll = variance <= 0 ? 0 : this.random.nextInt(variance + 1);
		lifespanTicks = Math.max(1, limits.alienLifespanTicks + roll);
	}

	/** True once age reaches alienDecayWarningFraction of lifespan. */
	public boolean isDecaying() {
		HatchlingConfig.Limits limits = HatchlingConfig.get().limits;
		if (!limits.alienLifespanEnabled || lifespanTicks <= 0) {
			return false;
		}
		return ageTicks >= (int) (lifespanTicks * limits.alienDecayWarningFraction);
	}

	private void applyDecayEffects() {
		if (!decayApplied) {
			decayApplied = true;
			EntityAttributeInstance attack = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
			if (attack != null) {
				attack.setBaseValue(HatchlingConfig.get().stats.alienAttackDamage * 0.75);
			}
		}
		if (this.age % 40 == 0) {
			this.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0, false, false, true));
		}
		if (HatchlingConfig.get().feedback.particlesEnabled
				&& this.getWorld() instanceof ServerWorld serverWorld
				&& this.age % 10 == 0) {
			serverWorld.spawnParticles(ParticleTypes.SMOKE,
					this.getX(), this.getY() + this.getHeight() * 0.6, this.getZ(),
					3, 0.2, 0.3, 0.2, 0.01);
			serverWorld.spawnParticles(ParticleTypes.SCULK_SOUL,
					this.getX(), this.getY() + this.getHeight() * 0.7, this.getZ(),
					1, 0.15, 0.2, 0.15, 0.01);
		}
	}

	private void dieOfAge() {
		World world = this.getWorld();
		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.SMOKE,
					this.getX(), this.getY() + 1.0, this.getZ(),
					30, 0.4, 0.6, 0.4, 0.02);
			serverWorld.spawnParticles(ParticleTypes.SCULK_SOUL,
					this.getX(), this.getY() + 1.0, this.getZ(),
					12, 0.3, 0.5, 0.3, 0.02);
		}
		world.playSound(null, this.getX(), this.getY(), this.getZ(),
				ModSounds.ALIEN_DEATH, net.minecraft.sound.SoundCategory.HOSTILE,
				1.0f, ModSounds.ALIEN_PITCH);
		// Normal death path so loot tables run; no explosion (cow-burst exclusive).
		this.damage(this.getDamageSources().generic(), Float.MAX_VALUE);
		if (!this.isRemoved()) {
			this.discard();
		}
	}

	@Override
	public boolean hurtByWater() {
		return false;
	}

	/**
	 * Aliens must remain in Peaceful for the documented acceptance lifecycle.
	 * They still target players via ActiveTargetGoal when a player is present.
	 */
	@Override
	protected boolean isDisallowedInPeaceful() {
		return false;
	}

	@Override
	protected boolean isAffectedByDaylight() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.ALIEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ALIEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ALIEN_DEATH;
	}

	@Override
	public float getSoundPitch() {
		return ModSounds.ALIEN_PITCH;
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt(GENERATION_KEY, generation);
		nbt.putInt(AGE_TICKS_KEY, ageTicks);
		nbt.putInt(LIFESPAN_TICKS_KEY, lifespanTicks);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		generation = nbt.getInt(GENERATION_KEY);
		ageTicks = nbt.getInt(AGE_TICKS_KEY);
		lifespanTicks = nbt.getInt(LIFESPAN_TICKS_KEY);
		decayApplied = false;
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = Math.max(0, generation);
	}
}
