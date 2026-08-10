package com.rexsaurus.hatchling.block;

import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.ParasiteEntity;
import com.rexsaurus.hatchling.registry.ModEntities;
import com.rexsaurus.hatchling.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class ParasiteEggBlock extends Block {
	public static final MapCodec<ParasiteEggBlock> CODEC = createCodec(ParasiteEggBlock::new);
	private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

	public ParasiteEggBlock(Settings settings) {
		super(settings);
	}

	@Override
	protected MapCodec<? extends Block> getCodec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		if (life.eggRequiresNearbyAnimal) {
			Box area = new Box(pos).expand(life.eggProximityRadius);
			if (world.getEntitiesByClass(AnimalEntity.class, area, e -> true).isEmpty()) {
				return;
			}
		}
		if (random.nextInt(life.eggHatchRandomTickChance) != 0) {
			return;
		}
		hatch(world, pos, true);
	}

	@Override
	public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
		if (!world.isClient && entity instanceof LivingEntity && world.getRandom().nextFloat() < 0.25f) {
			hatch(world, pos, true);
		}
		super.onSteppedOn(world, pos, state, entity);
	}

	@Override
	public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		if (!world.isClient && !player.isCreative() && !hasSilkTouch(world, player.getMainHandStack())) {
			hatch(world, pos, false);
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

	private void hatch(World world, BlockPos pos, boolean breakBlock) {
		if (world.isClient) {
			return;
		}
		if (breakBlock) {
			world.breakBlock(pos, false);
		}
		ParasiteEntity larva = ModEntities.PARASITE.create(world);
		if (larva != null) {
			larva.refreshPositionAndAngles(
					pos.getX() + 0.5,
					pos.getY() + 0.1,
					pos.getZ() + 0.5,
					world.getRandom().nextFloat() * 360.0f,
					0.0f);
			world.spawnEntity(larva);
		}
		world.playSound(null, pos, ModSounds.EGG_HATCH, SoundCategory.BLOCKS,
				1.0f, ModSounds.EGG_HATCH_PITCH);
	}
}
