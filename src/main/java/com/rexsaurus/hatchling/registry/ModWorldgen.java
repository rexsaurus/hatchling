package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.config.HatchlingConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Worldgen is data-driven under data/hatchling/worldgen/.
 * NOTE: configured_feature hardcodes cluster size 4 — config.worldgen.eggClusterSize
 * is documentation for a future code-based feature and is not read by JSON.
 */
public final class ModWorldgen {
	public static final RegistryKey<PlacedFeature> HATCHLING_EGG_PLACED = RegistryKey.of(
			RegistryKeys.PLACED_FEATURE,
			Identifier.of(Hatchling.MOD_ID, "hatchling_egg_placed"));

	public static void register() {
		if (!HatchlingConfig.get().worldgen.generateEggs) {
			return;
		}
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Feature.UNDERGROUND_DECORATION,
				HATCHLING_EGG_PLACED);
	}

	private ModWorldgen() {
	}
}
