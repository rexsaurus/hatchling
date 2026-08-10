package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.block.HatchlingEggBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
	public static final BlockEntityType<HatchlingEggBlockEntity> HATCHLING_EGG = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(Hatchling.MOD_ID, "hatchling_egg"),
			BlockEntityType.Builder.create(HatchlingEggBlockEntity::new, ModBlocks.HATCHLING_EGG).build());

	public static void register() {
		// Static init.
	}

	private ModBlockEntities() {
	}
}
