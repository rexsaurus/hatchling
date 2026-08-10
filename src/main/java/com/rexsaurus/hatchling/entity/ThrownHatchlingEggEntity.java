package com.rexsaurus.hatchling.entity;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.registry.ModItems;
import com.rexsaurus.hatchling.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ThrownHatchlingEggEntity extends ThrownItemEntity {
	private static final String GENERATION_KEY = "Generation";
	private int generation;

	public ThrownHatchlingEggEntity(EntityType<? extends ThrownHatchlingEggEntity> entityType, World world) {
		super(entityType, world);
	}

	public ThrownHatchlingEggEntity(EntityType<? extends ThrownHatchlingEggEntity> entityType, LivingEntity owner, World world) {
		super(entityType, owner, world);
	}

	public ThrownHatchlingEggEntity(EntityType<? extends ThrownHatchlingEggEntity> entityType, double x, double y, double z, World world) {
		super(entityType, x, y, z, world);
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.HATCHLING_EGG;
	}

	@Override
	public void tick() {
		super.tick();
		World world = this.getWorld();
		// Client-only trail; no net.minecraft.client imports.
		if (world.isClient && HatchlingConfig.get().feedback.particlesEnabled) {
			world.addParticle(
					ParticleTypes.CRIMSON_SPORE,
					this.getX(),
					this.getY(),
					this.getZ(),
					0.0,
					0.0,
					0.0);
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		if (this.getWorld().isClient) {
			return;
		}

		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		boolean hatch = true;
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			hatch = life.thrownEggHatchesOnEntityHit;
		}

		PlayerEntity hitPlayer = null;
		if (hitResult instanceof EntityHitResult entityHit
				&& entityHit.getEntity() instanceof PlayerEntity player) {
			hitPlayer = player;
		}

		if (hatch) {
			hatchLarva(hitResult.getPos());
		}
		if (hitPlayer != null && !HatchlingConfig.get().targeting.infectPlayers) {
			applyPlayerHitEffects(hitPlayer);
		}
		this.discard();
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
	}

	/**
	 * infectPlayers stays false by default: hatch adjacent, seek cows normally,
	 * brief SLOWNESS/NAUSEA for weight — no player ride/infection.
	 */
	private void applyPlayerHitEffects(PlayerEntity player) {
		int ticks = HatchlingConfig.get().lifecycle.eggPlayerHitEffectTicks;
		if (ticks > 0) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 0));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, ticks, 0));
		}
		this.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_SLIME_ATTACK, SoundCategory.PLAYERS,
				0.9f, 0.55f);
	}

	private void hatchLarva(Vec3d hitPos) {
		World world = this.getWorld();
		double yOffset = HatchlingConfig.get().lifecycle.thrownEggSpawnYOffset;
		HatchlingEntity larva = ModEntities.HATCHLING.create(world);
		if (larva != null) {
			larva.setGeneration(this.generation);
			larva.refreshPositionAndAngles(
					hitPos.x,
					hitPos.y + yOffset,
					hitPos.z,
					world.getRandom().nextFloat() * 360.0f,
					0.0f);
			world.spawnEntity(larva);
		}

		world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
				ModSounds.EGG_HATCH, SoundCategory.NEUTRAL,
				1.0f, ModSounds.EGG_HATCH_PITCH);

		if (world instanceof ServerWorld serverWorld) {
			serverWorld.spawnParticles(ParticleTypes.CRIMSON_SPORE,
					hitPos.x, hitPos.y + yOffset, hitPos.z,
					12, 0.2, 0.2, 0.2, 0.05);
		}
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

	public void setGeneration(int generation) {
		this.generation = Math.max(0, generation);
	}

	public int getGeneration() {
		return generation;
	}
}
