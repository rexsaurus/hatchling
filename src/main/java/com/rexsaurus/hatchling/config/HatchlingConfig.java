package com.rexsaurus.hatchling.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rexsaurus.hatchling.Hatchling;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HatchlingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static HatchlingConfig INSTANCE = new HatchlingConfig();

	public Lifecycle lifecycle = new Lifecycle();
	public Stats stats = new Stats();
	public Targeting targeting = new Targeting();
	public Worldgen worldgen = new Worldgen();
	public Feedback feedback = new Feedback();

	/** Resolved after load; not serialized. */
	private transient Set<EntityType<?>> resolvedHostBlacklist = Collections.emptySet();
	private transient Set<EntityType<?>> resolvedHostWhitelist = Collections.emptySet();

	public static final class Lifecycle {
		public int eggHatchRandomTickChance = 6;
		public double eggProximityRadius = 8.0;
		public boolean eggRequiresNearbyAnimal = true;
		public double larvaHostSearchRadius = 24.0;
		public double larvaLatchDistance = 1.5;
		public int incubationTicks = 600;
		public double sicknessOnsetFraction = 0.5;
		public boolean alienLaysEggs = true;
		public int alienEggLayIntervalTicks = 2400;
		public double alienEggLayChance = 0.25;
		public int alienMaxEggsInRadius = 3;
		public double alienEggCheckRadius = 16.0;
		public boolean eggAlwaysDrops = true;
		public double eggThrowVelocity = 1.5;
		public boolean thrownEggHatchesOnEntityHit = true;
		public int eggThrowCooldownTicks = 10;
		public double thrownEggSpawnYOffset = 0.25;
	}

	public static final class Stats {
		public double larvaHealth = 4.0;
		public double larvaSpeed = 0.45;
		public double larvaChaseSpeedMultiplier = 1.3;
		public double alienHealth = 30.0;
		public double alienAttackDamage = 6.0;
		public double alienSpeed = 0.32;
		public double alienFollowRange = 32.0;
	}

	public static final class Targeting {
		public boolean infectPlayers = false;
		public List<String> hostBlacklist = new ArrayList<>(List.of(
				"minecraft:wolf", "minecraft:cat", "minecraft:parrot"));
		/** If non-empty, ONLY these types are valid hosts and hostBlacklist is ignored. */
		public List<String> hostWhitelist = new ArrayList<>(List.of("minecraft:cow"));
		public boolean alienTargetsAnimals = true;
	}

	public static final class Worldgen {
		public boolean generateEggs = true;
		public int eggVeinsPerChunk = 2;
		public int eggMinY = -60;
		public int eggMaxY = 20;
		public int eggClusterSize = 4;
		public List<String> biomeBlacklist = new ArrayList<>();
	}

	public static final class Feedback {
		public boolean particlesEnabled = true;
		public boolean heartbeatSoundEnabled = true;
		public boolean hostGlowsWhenInfected = false;
		/** Extra client-only Y translate while riding; default 0 (vanilla attachment is enough). */
		public double larvaRenderYOffset = 0.0;
	}

	public static HatchlingConfig get() {
		return INSTANCE;
	}

	public Set<EntityType<?>> getHostBlacklistTypes() {
		return resolvedHostBlacklist;
	}

	public Set<EntityType<?>> getHostWhitelistTypes() {
		return resolvedHostWhitelist;
	}

	public boolean hasHostWhitelist() {
		return !resolvedHostWhitelist.isEmpty();
	}

	public static void load() {
		Path path = configPath();
		HatchlingConfig loaded = new HatchlingConfig();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				HatchlingConfig parsed = GSON.fromJson(reader, HatchlingConfig.class);
				if (parsed != null) {
					loaded = parsed;
					normalizeNulls(loaded);
				}
			} catch (Exception e) {
				Hatchling.LOGGER.warn("Failed to parse hatchling.json; using defaults. File was NOT overwritten.", e);
				loaded = new HatchlingConfig();
				loaded.clamp();
				loaded.resolveHostFilters();
				INSTANCE = loaded;
				return;
			}
		} else {
			try {
				Files.createDirectories(path.getParent());
				try (Writer writer = Files.newBufferedWriter(path)) {
					GSON.toJson(loaded, writer);
				}
				Hatchling.LOGGER.info("Wrote default config to {}", path);
			} catch (IOException e) {
				Hatchling.LOGGER.warn("Could not write default hatchling.json", e);
			}
		}
		loaded.clamp();
		loaded.resolveHostFilters();
		INSTANCE = loaded;
	}

	private void resolveHostFilters() {
		this.resolvedHostBlacklist = resolveEntityTypeIds(this.targeting.hostBlacklist, "hostBlacklist");
		this.resolvedHostWhitelist = resolveEntityTypeIds(this.targeting.hostWhitelist, "hostWhitelist");
	}

	private static Set<EntityType<?>> resolveEntityTypeIds(List<String> ids, String fieldName) {
		Set<EntityType<?>> resolved = new HashSet<>();
		for (String entry : ids) {
			Identifier id = Identifier.tryParse(entry);
			if (id == null) {
				Hatchling.LOGGER.warn("Invalid {} entry (skipped): {}", fieldName, entry);
				continue;
			}
			if (!Registries.ENTITY_TYPE.containsId(id)) {
				Hatchling.LOGGER.warn("Unknown {} entity id (skipped): {}", fieldName, entry);
				continue;
			}
			resolved.add(Registries.ENTITY_TYPE.get(id));
		}
		return Collections.unmodifiableSet(resolved);
	}

	private static void normalizeNulls(HatchlingConfig cfg) {
		if (cfg.lifecycle == null) cfg.lifecycle = new Lifecycle();
		if (cfg.stats == null) cfg.stats = new Stats();
		if (cfg.targeting == null) cfg.targeting = new Targeting();
		if (cfg.worldgen == null) cfg.worldgen = new Worldgen();
		if (cfg.feedback == null) cfg.feedback = new Feedback();
		if (cfg.targeting.hostBlacklist == null) {
			cfg.targeting.hostBlacklist = new ArrayList<>();
		}
		if (cfg.targeting.hostWhitelist == null) {
			cfg.targeting.hostWhitelist = new ArrayList<>(List.of("minecraft:cow"));
		}
		if (cfg.worldgen.biomeBlacklist == null) {
			cfg.worldgen.biomeBlacklist = new ArrayList<>();
		}
	}

	public void clamp() {
		Lifecycle life = this.lifecycle;
		if (life.eggHatchRandomTickChance < 1) {
			Hatchling.LOGGER.warn("Clamped eggHatchRandomTickChance {} -> 1", life.eggHatchRandomTickChance);
			life.eggHatchRandomTickChance = 1;
		}
		if (life.eggProximityRadius <= 0) {
			Hatchling.LOGGER.warn("Clamped eggProximityRadius {} -> 1.0", life.eggProximityRadius);
			life.eggProximityRadius = 1.0;
		}
		if (life.eggProximityRadius > 64.0) {
			Hatchling.LOGGER.warn("Clamped eggProximityRadius {} -> 64.0", life.eggProximityRadius);
			life.eggProximityRadius = 64.0;
		}
		if (life.larvaHostSearchRadius <= 0) {
			Hatchling.LOGGER.warn("Clamped larvaHostSearchRadius {} -> 1.0", life.larvaHostSearchRadius);
			life.larvaHostSearchRadius = 1.0;
		}
		if (life.larvaHostSearchRadius > 64.0) {
			Hatchling.LOGGER.warn("Clamped larvaHostSearchRadius {} -> 64.0", life.larvaHostSearchRadius);
			life.larvaHostSearchRadius = 64.0;
		}
		if (life.larvaLatchDistance <= 0) {
			Hatchling.LOGGER.warn("Clamped larvaLatchDistance {} -> 0.5", life.larvaLatchDistance);
			life.larvaLatchDistance = 0.5;
		}
		if (life.incubationTicks < 1) {
			Hatchling.LOGGER.warn("Clamped incubationTicks {} -> 1", life.incubationTicks);
			life.incubationTicks = 1;
		}
		if (life.sicknessOnsetFraction < 0.0) {
			Hatchling.LOGGER.warn("Clamped sicknessOnsetFraction {} -> 0.0", life.sicknessOnsetFraction);
			life.sicknessOnsetFraction = 0.0;
		}
		if (life.sicknessOnsetFraction > 1.0) {
			Hatchling.LOGGER.warn("Clamped sicknessOnsetFraction {} -> 1.0", life.sicknessOnsetFraction);
			life.sicknessOnsetFraction = 1.0;
		}
		if (life.alienEggLayIntervalTicks < 1) {
			Hatchling.LOGGER.warn("Clamped alienEggLayIntervalTicks {} -> 1", life.alienEggLayIntervalTicks);
			life.alienEggLayIntervalTicks = 1;
		}
		if (life.alienEggLayChance < 0.0) {
			Hatchling.LOGGER.warn("Clamped alienEggLayChance {} -> 0.0", life.alienEggLayChance);
			life.alienEggLayChance = 0.0;
		}
		if (life.alienEggLayChance > 1.0) {
			Hatchling.LOGGER.warn("Clamped alienEggLayChance {} -> 1.0", life.alienEggLayChance);
			life.alienEggLayChance = 1.0;
		}
		if (life.alienMaxEggsInRadius < 0) {
			Hatchling.LOGGER.warn("Clamped alienMaxEggsInRadius {} -> 0", life.alienMaxEggsInRadius);
			life.alienMaxEggsInRadius = 0;
		}
		if (life.alienEggCheckRadius <= 0) {
			Hatchling.LOGGER.warn("Clamped alienEggCheckRadius {} -> 1.0", life.alienEggCheckRadius);
			life.alienEggCheckRadius = 1.0;
		}
		if (life.alienEggCheckRadius > 64.0) {
			Hatchling.LOGGER.warn("Clamped alienEggCheckRadius {} -> 64.0", life.alienEggCheckRadius);
			life.alienEggCheckRadius = 64.0;
		}
		if (life.eggThrowVelocity <= 0) {
			Hatchling.LOGGER.warn("Clamped eggThrowVelocity {} -> 1.5", life.eggThrowVelocity);
			life.eggThrowVelocity = 1.5;
		}
		if (life.eggThrowCooldownTicks < 0) {
			Hatchling.LOGGER.warn("Clamped eggThrowCooldownTicks {} -> 0", life.eggThrowCooldownTicks);
			life.eggThrowCooldownTicks = 0;
		}

		Stats s = this.stats;
		if (s.larvaHealth <= 0) {
			Hatchling.LOGGER.warn("Clamped larvaHealth {} -> 1.0", s.larvaHealth);
			s.larvaHealth = 1.0;
		}
		if (s.larvaSpeed <= 0) {
			Hatchling.LOGGER.warn("Clamped larvaSpeed {} -> 0.1", s.larvaSpeed);
			s.larvaSpeed = 0.1;
		}
		if (s.larvaChaseSpeedMultiplier <= 0) {
			Hatchling.LOGGER.warn("Clamped larvaChaseSpeedMultiplier {} -> 1.0", s.larvaChaseSpeedMultiplier);
			s.larvaChaseSpeedMultiplier = 1.0;
		}
		if (s.alienHealth <= 0) {
			Hatchling.LOGGER.warn("Clamped alienHealth {} -> 1.0", s.alienHealth);
			s.alienHealth = 1.0;
		}
		if (s.alienAttackDamage < 0) {
			Hatchling.LOGGER.warn("Clamped alienAttackDamage {} -> 0.0", s.alienAttackDamage);
			s.alienAttackDamage = 0.0;
		}
		if (s.alienSpeed <= 0) {
			Hatchling.LOGGER.warn("Clamped alienSpeed {} -> 0.1", s.alienSpeed);
			s.alienSpeed = 0.1;
		}
		if (s.alienFollowRange <= 0) {
			Hatchling.LOGGER.warn("Clamped alienFollowRange {} -> 1.0", s.alienFollowRange);
			s.alienFollowRange = 1.0;
		}
		if (s.alienFollowRange > 64.0) {
			Hatchling.LOGGER.warn("Clamped alienFollowRange {} -> 64.0", s.alienFollowRange);
			s.alienFollowRange = 64.0;
		}

		Worldgen w = this.worldgen;
		if (w.eggVeinsPerChunk < 0) {
			Hatchling.LOGGER.warn("Clamped eggVeinsPerChunk {} -> 0", w.eggVeinsPerChunk);
			w.eggVeinsPerChunk = 0;
		}
		if (w.eggClusterSize < 1) {
			Hatchling.LOGGER.warn("Clamped eggClusterSize {} -> 1", w.eggClusterSize);
			w.eggClusterSize = 1;
		}
		if (w.eggMinY > w.eggMaxY) {
			Hatchling.LOGGER.warn("Swapped eggMinY/eggMaxY ({}/{})", w.eggMinY, w.eggMaxY);
			int tmp = w.eggMinY;
			w.eggMinY = w.eggMaxY;
			w.eggMaxY = tmp;
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("hatchling.json");
	}

	private HatchlingConfig() {
	}
}
