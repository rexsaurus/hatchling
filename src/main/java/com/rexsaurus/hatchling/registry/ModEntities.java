package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import com.rexsaurus.hatchling.entity.AlienEntity;
import com.rexsaurus.hatchling.entity.ParasiteEntity;
import com.rexsaurus.hatchling.entity.ThrownParasiteEggEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
	public static final EntityType<ParasiteEntity> PARASITE = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(Hatchling.MOD_ID, "parasite"),
			EntityType.Builder.create(ParasiteEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.5f, 0.4f)
					.build());

	public static final EntityType<AlienEntity> ALIEN = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(Hatchling.MOD_ID, "alien"),
			EntityType.Builder.create(AlienEntity::new, SpawnGroup.MONSTER)
					.dimensions(0.7f, 2.1f)
					.build());

	/** Same tracking profile as vanilla egg (Yarn: maxTrackingRange(4), trackingTickInterval(10)). */
	public static final EntityType<ThrownParasiteEggEntity> THROWN_PARASITE_EGG = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(Hatchling.MOD_ID, "thrown_parasite_egg"),
			EntityType.Builder.<ThrownParasiteEggEntity>create(ThrownParasiteEggEntity::new, SpawnGroup.MISC)
					.dimensions(0.25f, 0.25f)
					.maxTrackingRange(4)
					.trackingTickInterval(10)
					.build());

	public static void register() {
		// Static init registers entity types.
	}

	private ModEntities() {
	}
}
