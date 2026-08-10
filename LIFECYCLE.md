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
          (gen from BE)   |  hatchling_egg   |
                         +--------+---------+
                                  | randomTick / step / break→hatch
                                  v
 +------------------+    +------------------+    +------------------+
 | THROWN EGG       |--->| LARVA FREE       |--->| LARVA RIDING     |
 | ThrownHatchlingEgg|   | SeekHostGoal     |    | infectionTicks++ |
 +------------------+    | → cow whitelist  |    | sickness @ frac   |
         ^               +------------------+    +--------+---------+
         |                        ^                       |
         | throw (hosts+players)  | hatch                 | kill larva
         |                        |                       |  => host lives
         |               +--------v---------+             v
         |               | ALIEN            |      [HOST SAVED]
         +---------------+ ThrowEggGoal     |
                         | LayEggGoal       |
                         | PopulationCaps   |
                         +---+----+---------+
                             |    ^
                      decay  |    | BURST (convertTo / spawn)
                   blocks    |    |
                   lay/throw v    |
                         +---+----+---------+
                         | DECAY → AGE-DEATH|
                         | loot, no explode |
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
| Egg block (cluster) | `HatchlingEggBlock` (`EGGS` 1–3) + `HatchlingEggBlockEntity` | Player places item (stacking on existing cluster grows `EGGS`); alien `LayEggGoal`; worldgen ore | Hatch (randomTick, step 25%, or break when `eggAlwaysDrops=false` without silk) → **one larva per egg**, capped by `maxLarvaeInRadius` | Until hatch (random) | `lifecycle.eggHatch*`, `eggProximityRadius`, `eggRequiresNearbyAnimal`, `eggAlwaysDrops`; `feedback.eggGlowLevel`, egg particle/heartbeat keys; worldgen keys |
| Thrown egg | `ThrownHatchlingEggEntity` | Player `HatchlingEggItem.use`; alien `ThrowEggGoal` (hosts **and** players when `alienThrowsAtPlayers`) | Collision → hatch (or entity-hit skipped if `thrownEggHatchesOnEntityHit=false`); player hit + `infectPlayers=false` → hatch + brief SLOWNESS/NAUSEA, no infection | Flight until impact | `eggThrowVelocity`, `eggThrowCooldownTicks`, `thrownEggSpawnYOffset`, `thrownEggHatchesOnEntityHit`, `eggPlayerHitEffectTicks`, `alienEggThrow*`, `alienThrowsAtPlayers` |
| Larva free | `HatchlingEntity` (no vehicle) | Egg hatch; spawn egg; thrown hatch | Latch via `SeekHostGoal` when within `larvaLatchDistance` of valid host; or death | Until latch / death | `larvaHostSearchRadius`, `larvaLatchDistance`, `stats.larvaSpeed`, `larvaChaseSpeedMultiplier`, `targeting.hostWhitelist` / blacklist |
| Larva riding | `HatchlingEntity` riding host | `startRiding(host, true)` | Burst at `incubationTicks`; dismount/knockoff resets timer; larva death cures host | `incubationTicks` (default 600 = 30s) | `incubationTicks`, `sicknessOnsetFraction`, `feedback.particlesEnabled`, `heartbeatSoundEnabled`, `hatchlingRenderYOffset` |
| Sickness (host) | Status on host (not a Hatchling entity) | At `incubationTicks * sicknessOnsetFraction` | Effects expire if larva dies; otherwise last until burst | Remaining incubation | `sicknessOnsetFraction` (SLOWNESS II + NAUSEA) |
| Burst | Transient logic in `HatchlingEntity.burst` | `infectionTicks >= incubationTicks` | Alien present; larva discarded; host converted/removed | 1 tick | `feedback.burstExplosionEnabled`, `burstExplosionPower`, `burstDamagesBlocks`, `burstKnockbackRadius`, `burstKnockbackStrength`, `burstExplodeSoundVolume`, `burstExplodeSoundPitch`, `particlesEnabled` |
| Alien | `AlienEntity` | Burst convert/spawn | Decay → age-death; combat death; reproduction while caps allow and not decaying | Until lifespan / kill | `stats.alien*`, `lifecycle.alienLaysEggs`, `alienThrowsEggs`, intervals/chances/ranges, `limits.*` incl. lifespan keys |
| Decay | Same alien, `isDecaying()` | Age ≥ `lifespan * alienDecayWarningFraction` (default 0.8) | Age-death at full lifespan | Remaining ~20% of lifespan | `alienLifespanEnabled`, `alienLifespanTicks`, `alienLifespanVarianceTicks`, `alienDecayWarningFraction` |
| Age-death | Alien dies of old age | `ageTicks >= lifespanTicks` | Entity removed; **loot drops**, no burst explosion | 1 tick | Same lifespan keys; loot table `entities/alien` |
| Cap blocked | Same alien, goals no-op | Any `PopulationCaps.canReproduce` failure **or** decay | Caps ease / alien dies of age | Until counts drop or alien gone | `limits.maxAliensInRadius`, `maxLarvaeInRadius`, `maxEggBlocksInRadius`, `populationCheckRadius`, `generationCap`, `reproductionEnabled`, `populationCapWarnIntervalTicks`; also `alienMaxEggsInRadius` / `alienEggCheckRadius` for local lay density |

Host filter (all seek/throw/latch):

| Mode | When | Rule |
| --- | --- | --- |
| Whitelist | `hostWhitelist` non-empty (default: cow) | Only listed entity types |
| Blacklist fallback | whitelist empty | Animals (+ players if `infectPlayers`) minus `hostBlacklist` |

====================================================================
3. PLAYER INTERRUPTION POINTS
====================================================================

1. **Break / pick up eggs** — With `eggAlwaysDrops=true` (default), mining
   a cluster drops one item per egg in `EGGS`. Silk Touch also works.
   Removing clusters reduces future hatch pressure and frees
   `maxEggBlocksInRadius` (counts **blocks**, not eggs inside a nest).
2. **Throw eggs yourself** — Primary manual trigger: Hatchling creative
   tab → Hatchling Egg → aim at sky/ground near cows. Placing an egg on
   an existing nest grows the cluster (1→2→3) instead of a new block.
3. **Kill free larva** — Low HP (`larvaHealth` 4). Stops infection before latch.
4. **Kill riding larva** — Saves the host. Host status effects expire
   normally; infectionTicks are gone with the larva.
5. **Knock larva off** — If dismounted, infectionTicks reset to 0
   (must re-latch and restart).
6. **Kill the alien** — Stops throw/lay; drops chitin / rare egg.
   Or wait: lifespan decay stops reproduce, then age-death drops loot.
7. **Cull population** — Caps only stop *new* reproduction; existing
   aliens still fight until killed or aged out. Clear larvae/eggs/aliens
   to reopen caps.
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
| `maxLarvaeInRadius` | 8 | Max larvae in radius — also caps hatch from a cluster (a 3-egg nest wants 3 larvae; excess is skipped and logged) |
| `maxEggBlocksInRadius` | 5 | Max egg **blocks**/clusters (not individual eggs inside `EGGS`; scan uses min of population radius and `alienEggCheckRadius`) |
| `reproductionEnabled` | true | Master switch |

Why: generation alone does not stop *lateral* spam (many gen-0 aliens
in one chunk). Soft local density caps keep TPS and horror pacing
intact. Cap hits log a throttled WARN (`populationCapWarnIntervalTicks`).
A full 3-egg cluster is three potential larvae at once — so
`maxLarvaeInRadius` matters more than counting nests alone.
`lifecycle.alienMaxEggsInRadius` is an *additional* local density check
inside `LayEggGoal` (not the same as `limits.maxEggBlocksInRadius`).

### Lifespan + caps (dual damping)

Two independent brakes keep waves from snowballing:

1. **Population / generation caps** — stop *new* lay/throw when density or
   lineage depth is too high (lateral + recursive spam).
2. **Alien lifespan** — each alien rolls a personal TTL
   (`alienLifespanTicks` + up to `alienLifespanVarianceTicks`, default
   ~1 Minecraft day). Past `alienDecayWarningFraction` it decays (no
   reproduce) then age-dies with loot. Caps throttle births; lifespan
   retires adults so a region does not stay permanently saturated.

Disable with `alienLifespanEnabled: false` if you want immortal aliens
(caps alone still apply).

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
     "hatchlingRenderYOffset": 0.0,
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
    "hatchlingRenderYOffset": 0.0,
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
    "hatchlingRenderYOffset": 0.0,
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
    "hatchlingRenderYOffset": 0.0,
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
| Alien lifespan (base) | 24000 | 1200 (1 Minecraft day) |
| Decay onset | ~80% of lifespan | `alienDecayWarningFraction` |
