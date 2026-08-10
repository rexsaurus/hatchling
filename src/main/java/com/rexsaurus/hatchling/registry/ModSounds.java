package com.rexsaurus.hatchling.registry;

import com.rexsaurus.hatchling.Hatchling;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final SoundEvent EGG_HATCH = register("egg_hatch");
	public static final SoundEvent HEARTBEAT = register("heartbeat");
	public static final SoundEvent BURST = register("burst");
	public static final SoundEvent ALIEN_AMBIENT = register("alien_ambient");
	public static final SoundEvent ALIEN_HURT = register("alien_hurt");
	public static final SoundEvent ALIEN_DEATH = register("alien_death");

	/** Phase 1 pitch multipliers when playing each event. */
	public static final float EGG_HATCH_PITCH = 0.6f;
	public static final float HEARTBEAT_PITCH = 0.5f;
	public static final float BURST_PITCH = 0.5f;
	public static final float ALIEN_PITCH = 0.7f;

	private static SoundEvent register(String name) {
		Identifier id = Identifier.of(Hatchling.MOD_ID, name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void register() {
		// Static init registers sound events. Phase 1 sounds.json maps to vanilla samples.
	}

	private ModSounds() {
	}
}
