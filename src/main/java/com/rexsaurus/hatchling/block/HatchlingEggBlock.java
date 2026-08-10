package com.rexsaurus.hatchling.block;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.HatchlingEntity;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.registry.ModSounds;
import com.rexsaurus.hatchling.util.PopulationCaps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class HatchlingEggBlock extends Block implements BlockEntityProvider {
	public static final MapCodec<HatchlingEggBlock> CODEC = createCodec(HatchlingEggBlock::new);
	/** Cluster size 1–3. Verified: IntProperty.of(name, min, max) in Yarn 1.21.1. */
	public static final IntProperty EGGS = IntProperty.of("eggs", 1, 3);

	/** Low nest shape (~5px tall) so players can walk over it. */
	private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

	public HatchlingEggBlock(Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(EGGS, 1));
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(EGGS);
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new HatchlingEggBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		return !context.shouldCancelInteraction()
				&& context.getStack().isOf(this.asItem())
				&& state.get(EGGS) < 3
				|| super.canReplace(state, context);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState existing = ctx.getWorld().getBlockState(ctx.getBlockPos());
		if (existing.isOf(this)) {
			return existing.with(EGGS, Math.min(3, existing.get(EGGS) + 1));
		}
		return this.getDefaultState().with(EGGS, 1);
	}

	/** Light level: base eggGlowLevel, plus +2 per extra egg, capped at 15. */
	public static int luminanceFor(BlockState state) {
		int eggs = state.contains(EGGS) ? state.get(EGGS) : 1;
		int base = HatchlingConfig.get().feedback.eggGlowLevel;
		return Math.min(15, Math.max(0, base + (eggs - 1) * 2));
	}

	@Override
	protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
		super.onBlockAdded(state, world, pos, oldState, notify);
		if (!world.isClient && !oldState.isOf(this)) {
			scheduleProximityPulse(world, pos);
		}
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		HatchlingConfig.Feedback feedback = HatchlingConfig.get().feedback;
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		if (feedback.heartbeatSoundEnabled
				&& hasNearbyValidHost(world, pos, life.eggProximityRadius)) {
			world.playSound(null, pos, ModSounds.HEARTBEAT, SoundCategory.BLOCKS,
					feedback.eggProximityHeartbeatVolume,
					feedback.eggProximityHeartbeatPitch);
		}
		scheduleProximityPulse(world, pos);
	}

	private void scheduleProximityPulse(World world, BlockPos pos) {
		int interval = Math.max(1, HatchlingConfig.get().feedback.eggProximityHeartbeatIntervalTicks);
		world.scheduleBlockTick(pos, this, interval);
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		HatchlingConfig.Feedback feedback = HatchlingConfig.get().feedback;

		boolean hostNearby = hasNearbyValidHost(world, pos, life.eggProximityRadius);

		// Server proximity sound also fires from randomTick (spec); scheduledTick
		// keeps ~40-tick cadence while a host remains nearby.
		if (hostNearby && feedback.heartbeatSoundEnabled && random.nextInt(3) == 0) {
			world.playSound(null, pos, ModSounds.HEARTBEAT, SoundCategory.BLOCKS,
					feedback.eggProximityHeartbeatVolume,
					feedback.eggProximityHeartbeatPitch);
		}

		if (life.eggRequiresNearbyAnimal && !hostNearby) {
			return;
		}
		if (random.nextInt(life.eggHatchRandomTickChance) != 0) {
			return;
		}
		hatch(world, pos, state, true);
	}

	/**
	 * Client-only call site (vanilla). No net.minecraft.client imports — particles only.
	 */
	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		HatchlingConfig.Feedback feedback = HatchlingConfig.get().feedback;
		if (!feedback.particlesEnabled) {
			return;
		}
		boolean hostNearby = hasNearbyValidHost(world, pos, HatchlingConfig.get().lifecycle.eggProximityRadius);
		int chance = hostNearby ? feedback.eggProximityParticleChance : feedback.eggIdleParticleChance;
		if (random.nextInt(Math.max(1, chance)) != 0) {
			return;
		}
		int eggs = state.get(EGGS);
		double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
		double y = pos.getY() + 0.2 + eggs * 0.05;
		double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
		world.addParticle(ParticleTypes.CRIMSON_SPORE, x, y, z, 0.0, 0.02, 0.0);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (!world.isClient && entity instanceof LivingEntity && world.getRandom().nextFloat() < 0.25f) {
			hatch(world, pos, state, true);
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && !player.isCreative()) {
			HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
			int eggs = state.get(EGGS);
			boolean silk = hasSilkTouch(world, player.getMainHandStack());
			if (life.eggAlwaysDrops || silk) {
				dropStack(world, pos, new ItemStack(this, eggs));
			} else {
				hatch(world, pos, state, false);
			}
		}
		return super.onBreak(world, pos, state, player);
	}

	@Override
	public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
		return new ItemStack(this);
	}

	private static boolean hasSilkTouch(World world, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		RegistryEntry<Enchantment> silk = world.getRegistryManager()
				.get(RegistryKeys.ENCHANTMENT)
				.getEntry(Enchantments.SILK_TOUCH)
				.orElse(null);
		if (silk == null) {
			return false;
		}
		return EnchantmentHelper.getLevel(silk, stack) > 0;
	}

	private static boolean hasNearbyValidHost(World world, BlockPos pos, double radius) {
		Box area = new Box(pos).expand(radius);
		return !world.getEntitiesByClass(LivingEntity.class, area, HatchlingEntity::isValidHost).isEmpty();
	}

	private void hatch(World world, BlockPos pos, BlockState state, boolean breakBlock) {
		if (world.isClient) {
			return;
		}
		int generation = 0;
		BlockEntity be = world.getBlockEntity(pos);
		if (be instanceof HatchlingEggBlockEntity eggBe) {
			generation = eggBe.getGeneration();
		}
		int eggs = state.contains(EGGS) ? state.get(EGGS) : 1;
		if (breakBlock) {
			world.breakBlock(pos, false);
		}

		PopulationCaps.Counts counts = PopulationCaps.countNearby(world, pos);
		int room = Math.max(0, HatchlingConfig.get().limits.maxLarvaeInRadius - counts.larvae());
		int toSpawn = Math.min(eggs, room);
		if (toSpawn < eggs) {
			Hatchling.LOGGER.warn(
					"Egg cluster hatch at {} capped: wanted {} larvae, spawning {} (maxLarvaeInRadius)",
					pos, eggs, toSpawn);
		}

		for (int i = 0; i < toSpawn; i++) {
			HatchlingEntity larva = ModEntities.HATCHLING.create(world);
			if (larva == null) {
				continue;
			}
			larva.setGeneration(generation);
			double ox = (i % 2) * 0.25;
			double oz = (i / 2) * 0.25;
			larva.refreshPositionAndAngles(
					pos.getX() + 0.4 + ox,
					pos.getY() + 0.1,
					pos.getZ() + 0.4 + oz,
					world.getRandom().nextFloat() * 360.0f,
					0.0f);
			world.spawnEntity(larva);
		}

		world.playSound(null, pos, ModSounds.EGG_HATCH, SoundCategory.BLOCKS,
				1.0f, ModSounds.EGG_HATCH_PITCH);
	}
}
