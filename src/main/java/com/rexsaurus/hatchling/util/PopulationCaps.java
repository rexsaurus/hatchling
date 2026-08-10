package com.rexsaurus.hatchling.util;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.block.ParasiteEggBlock;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import com.rexsaurus.hatchling.entity.AlienEntity;
import com.rexsaurus.hatchling.entity.ParasiteEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared population / generation gates for LayEggGoal and ThrowEggGoal.
 */
public final class PopulationCaps {
	private static final Map<Long, Long> LAST_WARN_TICK_BY_CHUNK = new HashMap<>();

	private PopulationCaps() {
	}

	public static boolean canReproduce(AlienEntity alien) {
		HatchlingConfig.Limits limits = HatchlingConfig.get().limits;
		if (!limits.reproductionEnabled) {
			return false;
		}
		if (alien.getGeneration() >= limits.generationCap) {
			warnCap(alien.getWorld(), alien.getBlockPos(), "generationCap", alien.getGeneration());
			return false;
		}
		Counts counts = countNearby(alien.getWorld(), alien.getBlockPos());
		if (counts.aliens >= limits.maxAliensInRadius) {
			warnCap(alien.getWorld(), alien.getBlockPos(), "maxAliensInRadius", counts.aliens);
			return false;
		}
		if (counts.larvae >= limits.maxLarvaeInRadius) {
			warnCap(alien.getWorld(), alien.getBlockPos(), "maxLarvaeInRadius", counts.larvae);
			return false;
		}
		if (counts.eggs >= limits.maxEggBlocksInRadius) {
			warnCap(alien.getWorld(), alien.getBlockPos(), "maxEggBlocksInRadius", counts.eggs);
			return false;
		}
		return true;
	}

	public static Counts countNearby(World world, BlockPos origin) {
		double radius = HatchlingConfig.get().limits.populationCheckRadius;
		Box box = new Box(origin).expand(radius);
		int aliens = 0;
		int larvae = 0;
		for (Entity entity : world.getOtherEntities(null, box)) {
			if (entity instanceof AlienEntity) {
				aliens++;
			} else if (entity instanceof ParasiteEntity) {
				larvae++;
			}
		}
		int eggs = countEggBlocks(world, origin, Math.min(radius, HatchlingConfig.get().lifecycle.alienEggCheckRadius));
		return new Counts(aliens, larvae, eggs);
	}

	private static int countEggBlocks(World world, BlockPos origin, double radius) {
		int r = MathHelper.ceil(radius);
		int count = 0;
		for (BlockPos pos : BlockPos.iterate(origin.add(-r, -r, -r), origin.add(r, r, r))) {
			if (world.getBlockState(pos).getBlock() instanceof ParasiteEggBlock) {
				count++;
			}
		}
		return count;
	}

	private static void warnCap(World world, BlockPos pos, String cap, int value) {
		if (world.isClient) {
			return;
		}
		long chunkKey = ChunkPos.toLong(pos);
		long now = world.getTime();
		int interval = HatchlingConfig.get().limits.populationCapWarnIntervalTicks;
		Long last = LAST_WARN_TICK_BY_CHUNK.get(chunkKey);
		if (last != null && now - last < interval) {
			return;
		}
		LAST_WARN_TICK_BY_CHUNK.put(chunkKey, now);
		Hatchling.LOGGER.warn("Population cap hit at chunk {} pos={}: {}={}",
				new ChunkPos(pos), pos, cap, value);
	}

	public record Counts(int aliens, int larvae, int eggs) {
	}
}
