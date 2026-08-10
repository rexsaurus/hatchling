# Hatchling — Lifecycle Reference

Horror parasite loop for Minecraft 1.21.1 (Fabric). Tuning lives in
`config/hatchling.json`. This document is the player/designer view of
states, interruptions, caps, and three ready-to-paste presets.

Related: [SPEC.md](SPEC.md) · [RUNNING.md](RUNNING.md) · [README.md](README.md)

====================================================================
1. ASCII STATE DIAGRAM
====================================================================

```
                         +------------------+
          place / worldgen|  EGG BLOCK       |
          (gen from BE)   |  parasite_egg    |
                         +--------+---------+
                                  | randomTick / step / break→hatch
                                  v
 +------------------+    +------------------+    +------------------+
 | THROWN EGG       |--->| LARVA FREE       |--->| LARVA RIDING     |
 | ThrownParasiteEgg|    | SeekHostGoal     |    | infectionTicks++ |
 +------------------+    | → cow whitelist  |    | sickness @ frac   |
         ^               +------------------+    +--------+---------+
         |                        ^                       |
         | throw (ThrowEggGoal)   | hatch                 | kill larva
         |                        |                       |  => host lives
         |               +--------v---------+             v
         |               | ALIEN            |      [HOST SAVED]
         +---------------+ ThrowEggGoal     |
                         | LayEggGoal       |
                         | PopulationCaps   |
                         +--------+---------+
                                  ^
                                  | BURST (convertTo / spawn)
                                  |
                         +--------+---------+
                         | BURST TRANSIENT  |
                         | VFX + knockback  |
                         | host → alien     |
                         +------------------+
```

Generation edges:
- Player item / worldgen egg / creative larva → gen **0**
- Burst: alien.generation = larva.generation
- Alien lay/throw: next egg/larva gen = alien.generation + **1**
- If alien.generation >= `limits.generationCap` → no more lay/throw

====================================================================
2. STATE TABLE
====================================================================

| State | Entity / block | Entry | Exit | Duration | Config keys |
| --- | --- | --- | --- | --- | --- |
| Egg block | `ParasiteEggBlock` + `ParasiteEggBlockEntity` | Player places item; alien `LayEggGoal`; worldgen ore | Hatch (randomTick, step 25%, or break when `eggAlwaysDrops=false` without silk) | Until hatch (random) | `lifecycle.eggHatchRandomTickChance`, `eggProximityRadius`, `eggRequiresNearbyAnimal`, `eggAlwaysDrops`; worldgen keys for natural spawn |
| Thrown egg | `ThrownParasiteEggEntity` | Player `ParasiteEggItem.use`; alien `ThrowEggGoal` | Collision → hatch (or entity-hit skipped if `thrownEggHatchesOnEntityHit=false`) | Flight until impact | `eggThrowVelocity`, `eggThrowCooldownTicks`, `thrownEggSpawnYOffset`, `thrownEggHatchesOnEntityHit`, `alienEggThrow*` |
| Larva free | `ParasiteEntity` (no vehicle) | Egg hatch; spawn egg; thrown hatch | Latch via `SeekHostGoal` when within `larvaLatchDistance` of valid host; or death | Until latch / death | `larvaHostSearchRadius`, `larvaLatchDistance`, `stats.larvaSpeed`, `larvaChaseSpeedMultiplier`, `targeting.hostWhitelist` / blacklist |
| Larva riding | `ParasiteEntity` riding host | `startRiding(host, true)` | Burst at `incubationTicks`; dismount/knockoff resets timer; larva death cures host | `incubationTicks` (default 600 = 30s) | `incubationTicks`, `sicknessOnsetFraction`, `feedback.particlesEnabled`, `heartbeatSoundEnabled`, `larvaRenderYOffset` |
| Sickness (host) | Status on host (not a Hatchling entity) | At `incubationTicks * sicknessOnsetFraction` | Effects expire if larva dies; otherwise last until burst | Remaining incubation | `sicknessOnsetFraction` (SLOWNESS II + NAUSEA) |
| Burst | Transient logic in `ParasiteEntity.burst` | `infectionTicks >= incubationTicks` | Alien present; larva discarded; host converted/removed | 1 tick | `feedback.burstExplosionEnabled`, `burstExplosionPower`, `burstDamagesBlocks`, `burstKnockbackRadius`, `burstKnockbackStrength`, `burstExplodeSoundVolume`, `burstExplodeSoundPitch`, `particlesEnabled` |
| Alien | `AlienEntity` | Burst convert/spawn | Death; reproduction goals fire while caps allow | Indefinite | `stats.alien*`, `lifecycle.alienLaysEggs`, `alienThrowsEggs`, intervals/chances/ranges, `limits.*` |
| Cap blocked | Same alien, goals no-op | Any `PopulationCaps.canReproduce` failure | Caps ease (entities die / eggs broken / gen below cap) | Until counts drop | `limits.maxAliensInRadius`, `maxLarvaeInRadius`, `maxEggBlocksInRadius`, `populationCheckRadius`, `generationCap`, `reproductionEnabled`, `populationCapWarnIntervalTicks`; also `alienMaxEggsInRadius` / `alienEggCheckRadius` for local lay density |

Host filter (all seek/throw/latch):

| Mode | When | Rule |
| --- | --- | --- |
| Whitelist | `hostWhitelist` non-empty (default: cow) | Only listed entity types |
| Blacklist fallback | whitelist empty | Animals (+ players if `infectPlayers`) minus `hostBlacklist` |

====================================================================
3. PLAYER INTERRUPTION POINTS
====================================================================

1. **Break / pick up eggs** — With `eggAlwaysDrops=true` (default), mining
   an egg drops the item. Silk Touch also works. Removing eggs reduces
   future hatch pressure and frees `maxEggBlocksInRadius`.
2. **Throw eggs yourself** — Primary manual trigger: Hatchling creative
   tab → Parasite Egg → aim at sky/ground near cows.
3. **Kill free larva** — Low HP (`larvaHealth` 4). Stops infection before latch.
4. **Kill riding larva** — Saves the host. Host status effects expire
   normally; infectionTicks are gone with the larva.
5. **Knock larva off** — If dismounted, infectionTicks reset to 0
   (must re-latch and restart).
6. **Kill the alien** — Stops throw/lay; drops chitin / rare egg.
7. **Cull population** — Caps only stop *new* reproduction; existing
   aliens still fight. Clear larvae/eggs/aliens to reopen caps.
8. **Config** — Edit `config/hatchling.json` then `/hatchling reload`
   (op 2) to change timings without restart.
9. **Peaceful is OK** — Larvae/aliens do **not** despawn in Peaceful
   (`isDisallowedInPeaceful` false). Difficulty still affects vanilla
   combat feel, but the lifecycle can be tested on Peaceful Superflat.

====================================================================
4. GENERATION + POPULATION CAPS (WHY)
====================================================================

### Generation

Each lineage step increments when an **alien** creates the next egg:

```
gen 0 larva  →  gen 0 alien  →  gen 1 egg/larva  →  gen 1 alien  →  …
```

When `alien.getGeneration() >= generationCap` (default **4**), that alien
will not lay or throw. Earlier generations can still reproduce until
their own gen hits the cap.

Why: without a generation ceiling, one cave egg can recursively fill
a region forever. Cap = finite family tree depth.

### Population caps

Before lay/throw, `PopulationCaps.canReproduce` checks a sphere of
`populationCheckRadius` (default 48):

| Cap | Default | Meaning |
| --- | --- | --- |
| `maxAliensInRadius` | 6 | Max aliens counted in radius |
| `maxLarvaeInRadius` | 8 | Max larvae in radius |
| `maxEggBlocksInRadius` | 5 | Max egg blocks (scan uses min of population radius and `alienEggCheckRadius`) |
| `reproductionEnabled` | true | Master switch |

Why: generation alone does not stop *lateral* spam (many gen-0 aliens
in one chunk). Soft local density caps keep TPS and horror pacing
intact. Cap hits log a throttled WARN (`populationCapWarnIntervalTicks`).

`lifecycle.alienMaxEggsInRadius` is an *additional* local density check
inside `LayEggGoal` (not the same as `limits.maxEggBlocksInRadius`).

====================================================================
5. TUNING PRESETS — EXACT JSON
====================================================================

Edit `run/config/hatchling.json` (or your instance `config/hatchling.json`),
then run `/hatchling reload`.

Below: full replacement documents. Diffs are against the **Default
(shipped)** file in §5.2.

--------------------------------------------------------------------
5.1 Sparse horror
--------------------------------------------------------------------

Longer quiet, rarer reproduction, tighter caps, softer burst knockback.

```diff
--- hatchling.default.json
+++ hatchling.sparse.json
@@
   "lifecycle": {
-    "eggHatchRandomTickChance": 6,
-    "eggProximityRadius": 8.0,
+    "eggHatchRandomTickChance": 14,
+    "eggProximityRadius": 6.0,
     "eggRequiresNearbyAnimal": true,
-    "larvaHostSearchRadius": 24.0,
+    "larvaHostSearchRadius": 18.0,
     "larvaLatchDistance": 1.5,
-    "incubationTicks": 600,
+    "incubationTicks": 1200,
     "sicknessOnsetFraction": 0.5,
     "alienLaysEggs": true,
-    "alienEggLayIntervalTicks": 2400,
-    "alienEggLayChance": 0.25,
-    "alienMaxEggsInRadius": 3,
+    "alienEggLayIntervalTicks": 4800,
+    "alienEggLayChance": 0.12,
+    "alienMaxEggsInRadius": 1,
     "alienEggCheckRadius": 16.0,
     "eggAlwaysDrops": true,
     "eggThrowVelocity": 1.5,
     "thrownEggHatchesOnEntityHit": true,
     "eggThrowCooldownTicks": 10,
     "thrownEggSpawnYOffset": 0.25,
     "alienThrowsEggs": true,
-    "alienEggThrowIntervalTicks": 600,
-    "alienEggThrowChance": 0.4,
-    "alienEggThrowRange": 16.0,
+    "alienEggThrowIntervalTicks": 1600,
+    "alienEggThrowChance": 0.18,
+    "alienEggThrowRange": 12.0,
     "alienEggThrowVelocity": 0.9,
     "alienEggThrowWindupTicks": 20,
     "alienEggThrowArcFactor": 0.2,
     "alienEggThrowInaccuracy": 6.0
   },
   "stats": {
     "larvaHealth": 4.0,
-    "larvaSpeed": 0.45,
+    "larvaSpeed": 0.38,
     "larvaChaseSpeedMultiplier": 1.3,
-    "alienHealth": 30.0,
-    "alienAttackDamage": 6.0,
-    "alienSpeed": 0.32,
+    "alienHealth": 26.0,
+    "alienAttackDamage": 5.0,
+    "alienSpeed": 0.28,
     "alienFollowRange": 32.0,
     "alienWanderSpeed": 0.8
   },
   "targeting": {
     "infectPlayers": false,
     "hostBlacklist": ["minecraft:wolf", "minecraft:cat", "minecraft:parrot"],
     "hostWhitelist": ["minecraft:cow"],
     "alienTargetsAnimals": true
   },
   "worldgen": {
     "generateEggs": true,
-    "eggVeinsPerChunk": 2,
+    "eggVeinsPerChunk": 1,
     "eggMinY": -60,
     "eggMaxY": 20,
     "eggClusterSize": 4,
     "biomeBlacklist": []
   },
   "feedback": {
     "particlesEnabled": true,
     "heartbeatSoundEnabled": true,
     "hostGlowsWhenInfected": false,
     "larvaRenderYOffset": 0.0,
     "burstExplosionEnabled": true,
     "burstExplosionPower": 0.0,
     "burstDamagesBlocks": false,
-    "burstKnockbackRadius": 3.0,
-    "burstKnockbackStrength": 0.6,
+    "burstKnockbackRadius": 2.0,
+    "burstKnockbackStrength": 0.35,
     "burstExplodeSoundVolume": 0.8,
     "burstExplodeSoundPitch": 1.4
   },
   "limits": {
-    "maxAliensInRadius": 6,
-    "maxLarvaeInRadius": 8,
-    "populationCheckRadius": 48.0,
-    "maxEggBlocksInRadius": 5,
-    "generationCap": 4,
+    "maxAliensInRadius": 3,
+    "maxLarvaeInRadius": 4,
+    "populationCheckRadius": 64.0,
+    "maxEggBlocksInRadius": 2,
+    "generationCap": 2,
     "reproductionEnabled": true,
     "populationCapWarnIntervalTicks": 1200
   }
```

Full sparse file:

```json
{
  "lifecycle": {
    "eggHatchRandomTickChance": 14,
    "eggProximityRadius": 6.0,
    "eggRequiresNearbyAnimal": true,
    "larvaHostSearchRadius": 18.0,
    "larvaLatchDistance": 1.5,
    "incubationTicks": 1200,
    "sicknessOnsetFraction": 0.5,
    "alienLaysEggs": true,
    "alienEggLayIntervalTicks": 4800,
    "alienEggLayChance": 0.12,
    "alienMaxEggsInRadius": 1,
    "alienEggCheckRadius": 16.0,
    "eggAlwaysDrops": true,
    "eggThrowVelocity": 1.5,
    "thrownEggHatchesOnEntityHit": true,
    "eggThrowCooldownTicks": 10,
    "thrownEggSpawnYOffset": 0.25,
    "alienThrowsEggs": true,
    "alienEggThrowIntervalTicks": 1600,
    "alienEggThrowChance": 0.18,
    "alienEggThrowRange": 12.0,
    "alienEggThrowVelocity": 0.9,
    "alienEggThrowWindupTicks": 20,
    "alienEggThrowArcFactor": 0.2,
    "alienEggThrowInaccuracy": 6.0
  },
  "stats": {
    "larvaHealth": 4.0,
    "larvaSpeed": 0.38,
    "larvaChaseSpeedMultiplier": 1.3,
    "alienHealth": 26.0,
    "alienAttackDamage": 5.0,
    "alienSpeed": 0.28,
    "alienFollowRange": 32.0,
    "alienWanderSpeed": 0.8
  },
  "targeting": {
    "infectPlayers": false,
    "hostBlacklist": ["minecraft:wolf", "minecraft:cat", "minecraft:parrot"],
    "hostWhitelist": ["minecraft:cow"],
    "alienTargetsAnimals": true
  },
  "worldgen": {
    "generateEggs": true,
    "eggVeinsPerChunk": 1,
    "eggMinY": -60,
    "eggMaxY": 20,
    "eggClusterSize": 4,
    "biomeBlacklist": []
  },
  "feedback": {
    "particlesEnabled": true,
    "heartbeatSoundEnabled": true,
    "hostGlowsWhenInfected": false,
    "larvaRenderYOffset": 0.0,
    "burstExplosionEnabled": true,
    "burstExplosionPower": 0.0,
    "burstDamagesBlocks": false,
    "burstKnockbackRadius": 2.0,
    "burstKnockbackStrength": 0.35,
    "burstExplodeSoundVolume": 0.8,
    "burstExplodeSoundPitch": 1.4
  },
  "limits": {
    "maxAliensInRadius": 3,
    "maxLarvaeInRadius": 4,
    "populationCheckRadius": 64.0,
    "maxEggBlocksInRadius": 2,
    "generationCap": 2,
    "reproductionEnabled": true,
    "populationCapWarnIntervalTicks": 1200
  }
}
```

Note: `eggVeinsPerChunk` / `eggClusterSize` in config do not rewrite the
datapack ore `size: 4` — see SPEC.md §8. Sparse worldgen density for
veins still needs a JSON/datapack change or a future code feature.

--------------------------------------------------------------------
5.2 Default (shipped)
--------------------------------------------------------------------

```json
{
  "lifecycle": {
    "eggHatchRandomTickChance": 6,
    "eggProximityRadius": 8.0,
    "eggRequiresNearbyAnimal": true,
    "larvaHostSearchRadius": 24.0,
    "larvaLatchDistance": 1.5,
    "incubationTicks": 600,
    "sicknessOnsetFraction": 0.5,
    "alienLaysEggs": true,
    "alienEggLayIntervalTicks": 2400,
    "alienEggLayChance": 0.25,
    "alienMaxEggsInRadius": 3,
    "alienEggCheckRadius": 16.0,
    "eggAlwaysDrops": true,
    "eggThrowVelocity": 1.5,
    "thrownEggHatchesOnEntityHit": true,
    "eggThrowCooldownTicks": 10,
    "thrownEggSpawnYOffset": 0.25,
    "alienThrowsEggs": true,
    "alienEggThrowIntervalTicks": 600,
    "alienEggThrowChance": 0.4,
    "alienEggThrowRange": 16.0,
    "alienEggThrowVelocity": 0.9,
    "alienEggThrowWindupTicks": 20,
    "alienEggThrowArcFactor": 0.2,
    "alienEggThrowInaccuracy": 6.0
  },
  "stats": {
    "larvaHealth": 4.0,
    "larvaSpeed": 0.45,
    "larvaChaseSpeedMultiplier": 1.3,
    "alienHealth": 30.0,
    "alienAttackDamage": 6.0,
    "alienSpeed": 0.32,
    "alienFollowRange": 32.0,
    "alienWanderSpeed": 0.8
  },
  "targeting": {
    "infectPlayers": false,
    "hostBlacklist": ["minecraft:wolf", "minecraft:cat", "minecraft:parrot"],
    "hostWhitelist": ["minecraft:cow"],
    "alienTargetsAnimals": true
  },
  "worldgen": {
    "generateEggs": true,
    "eggVeinsPerChunk": 2,
    "eggMinY": -60,
    "eggMaxY": 20,
    "eggClusterSize": 4,
    "biomeBlacklist": []
  },
  "feedback": {
    "particlesEnabled": true,
    "heartbeatSoundEnabled": true,
    "hostGlowsWhenInfected": false,
    "larvaRenderYOffset": 0.0,
    "burstExplosionEnabled": true,
    "burstExplosionPower": 0.0,
    "burstDamagesBlocks": false,
    "burstKnockbackRadius": 3.0,
    "burstKnockbackStrength": 0.6,
    "burstExplodeSoundVolume": 0.8,
    "burstExplodeSoundPitch": 1.4
  },
  "limits": {
    "maxAliensInRadius": 6,
    "maxLarvaeInRadius": 8,
    "populationCheckRadius": 48.0,
    "maxEggBlocksInRadius": 5,
    "generationCap": 4,
    "reproductionEnabled": true,
    "populationCapWarnIntervalTicks": 1200
  }
}
```

--------------------------------------------------------------------
5.3 Infestation
--------------------------------------------------------------------

Faster hatch/incubate, aggressive throw/lay, higher caps, stronger burst.

```diff
--- hatchling.default.json
+++ hatchling.infestation.json
@@
   "lifecycle": {
-    "eggHatchRandomTickChance": 6,
-    "eggProximityRadius": 8.0,
+    "eggHatchRandomTickChance": 3,
+    "eggProximityRadius": 12.0,
     "eggRequiresNearbyAnimal": true,
-    "larvaHostSearchRadius": 24.0,
+    "larvaHostSearchRadius": 32.0,
     "larvaLatchDistance": 1.5,
-    "incubationTicks": 600,
-    "sicknessOnsetFraction": 0.5,
+    "incubationTicks": 240,
+    "sicknessOnsetFraction": 0.35,
     "alienLaysEggs": true,
-    "alienEggLayIntervalTicks": 2400,
-    "alienEggLayChance": 0.25,
-    "alienMaxEggsInRadius": 3,
-    "alienEggCheckRadius": 16.0,
+    "alienEggLayIntervalTicks": 400,
+    "alienEggLayChance": 0.65,
+    "alienMaxEggsInRadius": 8,
+    "alienEggCheckRadius": 24.0,
     "eggAlwaysDrops": true,
-    "eggThrowVelocity": 1.5,
+    "eggThrowVelocity": 1.7,
     "thrownEggHatchesOnEntityHit": true,
-    "eggThrowCooldownTicks": 10,
+    "eggThrowCooldownTicks": 5,
     "thrownEggSpawnYOffset": 0.25,
     "alienThrowsEggs": true,
-    "alienEggThrowIntervalTicks": 600,
-    "alienEggThrowChance": 0.4,
-    "alienEggThrowRange": 16.0,
-    "alienEggThrowVelocity": 0.9,
-    "alienEggThrowWindupTicks": 20,
+    "alienEggThrowIntervalTicks": 120,
+    "alienEggThrowChance": 0.75,
+    "alienEggThrowRange": 24.0,
+    "alienEggThrowVelocity": 1.15,
+    "alienEggThrowWindupTicks": 8,
     "alienEggThrowArcFactor": 0.2,
-    "alienEggThrowInaccuracy": 6.0
+    "alienEggThrowInaccuracy": 3.0
   },
   "stats": {
-    "larvaHealth": 4.0,
-    "larvaSpeed": 0.45,
-    "larvaChaseSpeedMultiplier": 1.3,
-    "alienHealth": 30.0,
-    "alienAttackDamage": 6.0,
-    "alienSpeed": 0.32,
-    "alienFollowRange": 32.0,
-    "alienWanderSpeed": 0.8
+    "larvaHealth": 6.0,
+    "larvaSpeed": 0.55,
+    "larvaChaseSpeedMultiplier": 1.55,
+    "alienHealth": 40.0,
+    "alienAttackDamage": 8.0,
+    "alienSpeed": 0.38,
+    "alienFollowRange": 40.0,
+    "alienWanderSpeed": 1.0
   },
@@
   "worldgen": {
     "generateEggs": true,
-    "eggVeinsPerChunk": 2,
+    "eggVeinsPerChunk": 4,
     "eggMinY": -60,
     "eggMaxY": 20,
     "eggClusterSize": 4,
     "biomeBlacklist": []
   },
   "feedback": {
@@
     "burstExplosionEnabled": true,
     "burstExplosionPower": 0.0,
     "burstDamagesBlocks": false,
-    "burstKnockbackRadius": 3.0,
-    "burstKnockbackStrength": 0.6,
-    "burstExplodeSoundVolume": 0.8,
-    "burstExplodeSoundPitch": 1.4
+    "burstKnockbackRadius": 5.0,
+    "burstKnockbackStrength": 1.1,
+    "burstExplodeSoundVolume": 1.0,
+    "burstExplodeSoundPitch": 1.2
   },
   "limits": {
-    "maxAliensInRadius": 6,
-    "maxLarvaeInRadius": 8,
-    "populationCheckRadius": 48.0,
-    "maxEggBlocksInRadius": 5,
-    "generationCap": 4,
+    "maxAliensInRadius": 16,
+    "maxLarvaeInRadius": 24,
+    "populationCheckRadius": 48.0,
+    "maxEggBlocksInRadius": 16,
+    "generationCap": 8,
     "reproductionEnabled": true,
     "populationCapWarnIntervalTicks": 1200
   }
```

Full infestation file:

```json
{
  "lifecycle": {
    "eggHatchRandomTickChance": 3,
    "eggProximityRadius": 12.0,
    "eggRequiresNearbyAnimal": true,
    "larvaHostSearchRadius": 32.0,
    "larvaLatchDistance": 1.5,
    "incubationTicks": 240,
    "sicknessOnsetFraction": 0.35,
    "alienLaysEggs": true,
    "alienEggLayIntervalTicks": 400,
    "alienEggLayChance": 0.65,
    "alienMaxEggsInRadius": 8,
    "alienEggCheckRadius": 24.0,
    "eggAlwaysDrops": true,
    "eggThrowVelocity": 1.7,
    "thrownEggHatchesOnEntityHit": true,
    "eggThrowCooldownTicks": 5,
    "thrownEggSpawnYOffset": 0.25,
    "alienThrowsEggs": true,
    "alienEggThrowIntervalTicks": 120,
    "alienEggThrowChance": 0.75,
    "alienEggThrowRange": 24.0,
    "alienEggThrowVelocity": 1.15,
    "alienEggThrowWindupTicks": 8,
    "alienEggThrowArcFactor": 0.2,
    "alienEggThrowInaccuracy": 3.0
  },
  "stats": {
    "larvaHealth": 6.0,
    "larvaSpeed": 0.55,
    "larvaChaseSpeedMultiplier": 1.55,
    "alienHealth": 40.0,
    "alienAttackDamage": 8.0,
    "alienSpeed": 0.38,
    "alienFollowRange": 40.0,
    "alienWanderSpeed": 1.0
  },
  "targeting": {
    "infectPlayers": false,
    "hostBlacklist": ["minecraft:wolf", "minecraft:cat", "minecraft:parrot"],
    "hostWhitelist": ["minecraft:cow"],
    "alienTargetsAnimals": true
  },
  "worldgen": {
    "generateEggs": true,
    "eggVeinsPerChunk": 4,
    "eggMinY": -60,
    "eggMaxY": 20,
    "eggClusterSize": 4,
    "biomeBlacklist": []
  },
  "feedback": {
    "particlesEnabled": true,
    "heartbeatSoundEnabled": true,
    "hostGlowsWhenInfected": false,
    "larvaRenderYOffset": 0.0,
    "burstExplosionEnabled": true,
    "burstExplosionPower": 0.0,
    "burstDamagesBlocks": false,
    "burstKnockbackRadius": 5.0,
    "burstKnockbackStrength": 1.1,
    "burstExplodeSoundVolume": 1.0,
    "burstExplodeSoundPitch": 1.2
  },
  "limits": {
    "maxAliensInRadius": 16,
    "maxLarvaeInRadius": 24,
    "populationCheckRadius": 48.0,
    "maxEggBlocksInRadius": 16,
    "generationCap": 8,
    "reproductionEnabled": true,
    "populationCapWarnIntervalTicks": 1200
  }
}
```

====================================================================
6. QUICK TIME REFERENCE (DEFAULT)
====================================================================

| Event | Ticks | Seconds (20 TPS) |
| --- | --- | --- |
| Incubation | 600 | 30 |
| Sickness onset | 300 | 15 |
| Alien throw interval | 600 | 30 |
| Alien lay interval | 2400 | 120 |
| Throw windup | 20 | 1 |
