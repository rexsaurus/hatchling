package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.block.HatchlingEggBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
	public static final Block HATCHLING_EGG = Registry.register(
			Registries.BLOCK,
			Identifier.of(Hatchling.MOD_ID, "hatchling_egg"),
			new HatchlingEggBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.GREEN)
					.strength(0.5f)
					.sounds(BlockSoundGroup.SLIME)
					.ticksRandomly()
					.nonOpaque()
					.luminance(HatchlingEggBlock::luminanceFor)));

	public static void register() {
		// Static init registers blocks.
	}

	private ModBlocks() {
	}
}
