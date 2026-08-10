package com.rexsaurus.hatchling.entity.goal;

import com.rexsaurus.hatchling.block.ParasiteEggBlock;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.AlienEntity;
import com.rexsaurus.hatchling.registry.ModBlocks;
import com.rexsaurus.hatchling.registry.ModSounds;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class LayEggGoal extends Goal {
	private final AlienEntity alien;
	private int cooldown;
	private BlockPos placePos;

	public LayEggGoal(AlienEntity alien) {
		this.alien = alien;
	}

	@Override
	public boolean canStart() {
		HatchlingConfig.Lifecycle life = HatchlingConfig.get().lifecycle;
		if (!life.alienLaysEggs) {
			return false;
		}
		if (++cooldown < life.alienEggLayIntervalTicks) {
			return false;
		}
		if (alien.getRandom().nextDouble() >= life.alienEggLayChance) {
			cooldown = 0;
			return false;
		}
		if (countEggsNearby() >= life.alienMaxEggsInRadius) {
			cooldown = 0;
			return false;
		}
		placePos = findPlacement();
		if (placePos == null) {
			cooldown = 0;
			return false;
		}
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return false;
	}

	@Override
	public void start() {
		World world = alien.getWorld();
		if (world.isClient || placePos == null) {
			return;
		}
		world.setBlockState(placePos, ModBlocks.PARASITE_EGG.getDefaultState());
		world.playSound(null, placePos, ModSounds.EGG_HATCH, SoundCategory.BLOCKS,
				0.6f, ModSounds.EGG_HATCH_PITCH);
		cooldown = 0;
		placePos = null;
	}

	private int countEggsNearby() {
		World world = alien.getWorld();
		double radius = Math.min(16.0, HatchlingConfig.get().lifecycle.alienEggCheckRadius);
		BlockPos origin = alien.getBlockPos();
		int r = MathHelper.ceil(radius);
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
			if (!isChunkLoaded(world, pos)) {
				continue;
			}
			if (world.getBlockState(pos).getBlock() instanceof ParasiteEggBlock) {
				count++;
			}
		}
		return count;
	}

	private BlockPos findPlacement() {
		World world = alien.getWorld();
		BlockPos origin = alien.getBlockPos();
		for (int attempt = 0; attempt < 12; attempt++) {
			int dx = alien.getRandom().nextInt(7) - 3;
			int dz = alien.getRandom().nextInt(7) - 3;
			int dy = alien.getRandom().nextInt(3) - 1;
			BlockPos candidate = origin.add(dx, dy, dz);
			if (!isChunkLoaded(world, candidate)) {
				continue;
			}
			BlockPos below = candidate.down();
			if (world.getBlockState(candidate).isAir()
					&& world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean isChunkLoaded(World world, BlockPos pos) {
		return world.isChunkLoaded(
				ChunkSectionPos.getSectionCoord(pos.getX()),
				ChunkSectionPos.getSectionCoord(pos.getZ()));
	}
}
