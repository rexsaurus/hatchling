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
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class AlienEntity extends HostileEntity {
	private static final String GENERATION_KEY = "Generation";
	private int generation;

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
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		generation = nbt.getInt(GENERATION_KEY);
	}

	public int getGeneration() {
		return generation;
	}

	public void setGeneration(int generation) {
		this.generation = Math.max(0, generation);
	}
}
