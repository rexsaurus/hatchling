package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.block.ParasiteEggBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {
	public static final BlockEntityType<ParasiteEggBlockEntity> PARASITE_EGG = Registry.register(
			Registries.BLOCK_ENTITY_TYPE,
			Identifier.of(Hatchling.MOD_ID, "parasite_egg"),
			BlockEntityType.Builder.create(ParasiteEggBlockEntity::new, ModBlocks.PARASITE_EGG).build());

	public static void register() {
		// Static init.
	}

	private ModBlockEntities() {
	}
}
