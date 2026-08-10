package com.rexsaurus.hatchling.entity;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.goal.LayEggGoal;
import com.rexsaurus.hatchling.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
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
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public class AlienEntity extends HostileEntity {
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
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1, false));
		this.goalSelector.add(2, new LayEggGoal(this));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));

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

	@Override
	protected boolean isDisallowedInPeaceful() {
		return true;
	}

	/** Aliens do not burn in sunlight — not undead. */
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
}
