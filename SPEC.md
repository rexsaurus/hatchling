# HATCHLING — Cursor Implementation Spec
# Repo: git@github.com:rexsaurus/hatchling.git
# Author: Rex St John

====================================================================
REVISION HISTORY
====================================================================

- **2026-08-10 — M6 REVISION**
  - FIX A: larva passenger render no longer stacks `hostHeight * 0.75`
    (vanilla attachment already places passengers). Tunable via
    `feedback.hatchlingRenderYOffset` (default `0.0`; was
    `larvaRenderYOffset` pre-M9). Keep 0.8 scale.
  - Host targeting: `targeting.hostWhitelist` default `["minecraft:cow"]`.
    Non-empty whitelist ignores blacklist.
  - Throwable egg: `HatchlingEggItem`, `ThrownHatchlingEggEntity`,
    `eggAlwaysDrops`, throw velocity/cooldown/Y offset config keys.
  - Egg block entity stores generation for laid eggs.

- **2026-08-10 — M7 REVISION**
  - Burst VFX: optional explosion with power `0.0` + `ExplosionSourceType.NONE`
    (no block damage by default), knockback radius/strength, extra particles,
    explode SFX volume/pitch.
  - `ThrowEggGoal` on aliens (priority above `LayEggGoal`).
  - Population / generation caps via `limits.*` + `PopulationCaps`.
  - Peaceful survival: `isDisallowedInPeaceful()` returns `false` on larva
    and alien so Creative/Peaceful acceptance tests work.

- **2026-08-10 — M8** (egg clusters — notes remain in §5):
  - `EGGS` 1–3 nest models, stack-on-place, proximity particles/heartbeat,
    one larva per egg at hatch (capped by `maxLarvaeInRadius`).

- **2026-08-10 — M9 REVISION**
  - Larva entity is `HatchlingEntity` (not Parasite). Registry IDs:
    `hatchling:hatchling`, `hatchling:hatchling_egg`,
    `hatchling:thrown_hatchling_egg` (+ `hatchling:alien`).
  - Custom Blockbench models: `HatchlingModel` / `AlienModel`. Toggle via
    `feedback.useCustomModels` (default `true`). **Restart required** —
    not applied by `/hatchling reload`.
  - Riding Y fine-tune: `feedback.hatchlingRenderYOffset` (replaces
    `larvaRenderYOffset`).
  - `targeting.alienThrowsAtPlayers` (default true): ThrowEggGoal can aim
    at players. `lifecycle.eggPlayerHitEffectTicks` (default 60): on player
    hit with `infectPlayers` still **false** → hatch + brief SLOWNESS/NAUSEA;
    **no** player ride/infection.
  - Alien lifespan: `limits.alienLifespanEnabled`, `alienLifespanTicks`
    (24000), `alienLifespanVarianceTicks`, `alienDecayWarningFraction`
    (0.8). Decay blocks lay/throw. Age-death drops loot, no explode.

Superseded statements from the original M1–M5 draft are marked
**[SUPERSEDED]** inline below. Prefer the M6–M9 text and CURRENT config
defaults when they conflict.

====================================================================
0. RULES FOR CURSOR — READ FIRST
====================================================================

1. TARGET VERSIONS ARE FIXED.
   - Minecraft **1.21.1**
   - Yarn **1.21.1+build.3**
   - Fabric Loader **0.16.14**
   - Fabric API **0.102.1+1.21.1**
   - Loom **1.7.4**
   - Java **21**
   - Gradle **8.10**
   Do NOT silently upgrade or downgrade. If a required API does not
   exist in 1.21.1, STOP and say so — do not invent a method name.

2. NEVER FABRICATE MOJANG/FABRIC API SIGNATURES. Method names change
   every minor version. Before using any vanilla method, verify it
   exists in the decompiled sources available in the Gradle-generated
   sources jar. If you cannot verify, leave a // TODO(verify) comment
   and flag it in your summary rather than guessing.

3. COMPILE AFTER EVERY MILESTONE. Run `./gradlew build`. Do not move
   to the next milestone with a red build. Report the actual compiler
   error text, never a paraphrase.

4. CLIENT/SERVER SPLIT IS NON-NEGOTIABLE. Renderers, models, and any
   net.minecraft.client.* import live ONLY in the `client` package and
   are registered ONLY from HatchlingClient (ClientModInitializer).
   Common code must never import a client class. A violation crashes
   dedicated servers.

5. NO HARDCODED TUNING NUMBERS. Every duration, radius, chance, and
   stat below must be read from HatchlingConfig. If you find yourself
   typing a magic number in a goal or entity, it belongs in config.
   Exception documented in §8: worldgen JSON cannot read config, so
   `eggClusterSize` is hardcoded to 4 in the configured feature JSON.

6. ALL SERVER-SIDE LOGIC MUST GUARD `if (world.isClient) return;`
   before mutating state or spawning entities.

7. PERSISTENCE IS REQUIRED. Any field that represents lifecycle
   progress must round-trip through writeCustomDataToNbt /
   readCustomDataFromNbt (entities) or block-entity NBT. Test by
   saving and reloading the world. Persist at least:
   `InfectionTicks`, `Generation` on larva/alien/thrown egg/egg BE.

8. PHASE 1 USES VANILLA MODELS. Do not block progress on artwork.
   Ship working mechanics against reskinned vanilla models first.

9. COMMIT PER MILESTONE. Conventional commits:
   feat(entity): add parasite larva ride-and-incubate behavior
   Small commits. No 2,000-line dumps.

10. .gitignore MUST cover build/, .gradle/, run/, .idea/, *.iml
    before any commit. Verify with `git status` that fewer than ~60
    files stage on the first commit.

====================================================================
1. WHAT WE ARE BUILDING
====================================================================

A horror-flavored parasite lifecycle mod for Minecraft 1.21.1 (Fabric).

Loop:
  Eggs (block **or** thrown projectile)
    → larva seeks a valid host (default: cow via hostWhitelist)
    → latches and incubates
    → host bursts into alien (VFX + knockback)
    → alien lays and/or throws eggs (gated by population/generation caps)
    → loop

Design pillars:
  - The threat is SLOW then SUDDEN. Long quiet incubation, violent burst.
  - The player can INTERVENE. The larva is a separate entity with its
    own hitbox and low HP — killing it mid-incubation saves the host.
  - It SPREADS. Ignoring it must be punished — but caps prevent
    unbounded server melt.

Mod id:        hatchling
Root package:  com.rexsaurus.hatchling
Display name:  Hatchling
License:       MIT

====================================================================
2. PACKAGE STRUCTURE (CURRENT)
====================================================================

com.rexsaurus.hatchling
  Hatchling.java                 ModInitializer entry point
  config/
    HatchlingConfig.java         POJO + GSON load/save + clamp
  registry/
    ModBlocks.java
    ModBlockEntities.java
    ModItems.java
    ModEntities.java
    ModSounds.java
    ModWorldgen.java
  block/
    HatchlingEggBlock.java
    HatchlingEggBlockEntity.java  Generation for laid eggs
  item/
    HatchlingEggItem.java         BlockItem + air-throw
  entity/
    HatchlingEntity.java
    AlienEntity.java
    ThrownHatchlingEggEntity.java
    goal/
      SeekHostGoal.java
      LayEggGoal.java
      ThrowEggGoal.java
  util/
    PopulationCaps.java         Shared reproduction gates
com.rexsaurus.hatchling.client
  HatchlingClient.java           ClientModInitializer entry point
  model/
    HatchlingModel.java          Blockbench larva (useCustomModels)
    AlienModel.java              Blockbench alien (useCustomModels)
  render/
    HatchlingRenderer.java       Custom model path
    AlienRenderer.java
    HatchlingLegacyRenderer.java Silverfish stand-in
    AlienLegacyRenderer.java     Enderman stand-in

fabric.mod.json declares BOTH entrypoints:
  "main":   ["com.rexsaurus.hatchling.Hatchling"]
  "client": ["com.rexsaurus.hatchling.client.HatchlingClient"]

====================================================================
3. CONFIG FILE — CURRENT DEFAULTS
====================================================================

Path: `config/hatchling.json` (created with defaults on first run)
Loader: GSON (bundled with Minecraft, no new dependency).
Load in `Hatchling.onInitialize()` BEFORE any registration that reads it.
Static access: `HatchlingConfig.get()`
Reload: `/hatchling reload` (op level 2)

Implementation notes:
  - Fields are public. On load: if file missing, write defaults.
  - Missing fields in an existing file keep Java defaults (forward-compat).
  - Parse failure → WARN, fall back to defaults, **do not** overwrite file.
  - `clamp()` after load enforces sane bounds.
  - Host filter IDs resolve once into transient EntityType sets.

Default file contents (matches CURRENT `HatchlingConfig` field defaults):

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
    "alienEggThrowInaccuracy": 6.0,
    "eggPlayerHitEffectTicks": 60
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
    "alienTargetsAnimals": true,
    "alienThrowsAtPlayers": true
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
    "useCustomModels": true,
    "burstExplosionEnabled": true,
    "burstExplosionPower": 0.0,
    "burstDamagesBlocks": false,
    "burstKnockbackRadius": 3.0,
    "burstKnockbackStrength": 0.6,
    "burstExplodeSoundVolume": 0.8,
    "burstExplodeSoundPitch": 1.4,
    "eggGlowLevel": 6,
    "eggIdleParticleChance": 3,
    "eggProximityParticleChance": 1,
    "eggProximityHeartbeatIntervalTicks": 40,
    "eggProximityHeartbeatVolume": 0.35,
    "eggProximityHeartbeatPitch": 0.4
  },
  "limits": {
    "maxAliensInRadius": 6,
    "maxLarvaeInRadius": 8,
    "populationCheckRadius": 48.0,
    "maxEggBlocksInRadius": 5,
    "generationCap": 4,
    "reproductionEnabled": true,
    "populationCapWarnIntervalTicks": 1200,
    "alienLifespanEnabled": true,
    "alienLifespanTicks": 24000,
    "alienLifespanVarianceTicks": 4000,
    "alienDecayWarningFraction": 0.8
  }
}
```

Config semantics:
  - `eggHatchRandomTickChance` = N means 1-in-N per random tick.
  - `sicknessOnsetFraction` 0.5 → effects at 50% of `incubationTicks`.
  - **hostWhitelist (M6):** if non-empty, ONLY those entity types are
    valid hosts; `hostBlacklist` is ignored. If empty, fall back to
    AnimalEntity (+ player if `infectPlayers`) minus blacklist.
  - Invalid entity IDs log WARN and are skipped; never crash.
  - `infectPlayers=false` remains the default. Do not enable without
    an explicit product decision. **[SUPERSEDED wording]** Older text
    said “player infection is Milestone 7”; that milestone is now
    burst/caps/throw — player infection is still opt-in, not shipped.
  - **M9 player egg hit:** with `infectPlayers=false`, a thrown egg that
    hits a player still hatches a larva nearby and applies SLOWNESS +
    NAUSEA for `eggPlayerHitEffectTicks` — it does **not** latch onto
    or infect the player. `alienThrowsAtPlayers` only affects throw aiming.
  - `useCustomModels`: read once at client init — change requires restart.
  - Burst defaults: explosion **enabled** but power **0.0** with
    `NONE` source type → cosmetic boom + knockback, no terrain dig.
  - Caps: aliens refuse to lay/throw when any limit fails (see LIFECYCLE.md).
  - Lifespan: decaying aliens also refuse lay/throw; age-death uses normal
    damage/loot (no burst explosion).

====================================================================
4. LIFECYCLE STATE MACHINE
====================================================================

```
  [EGG BLOCK]  or  [THROWN EGG]
         |                |
         | hatch          | impact hatch
         v                v
       [LARVA — unattached]  SeekHostGoal → cow (whitelist)
         |
         | latch (startRiding)
         v
       [LARVA — riding host]  <-- kill larva to cure host
         | infectionTicks → incubationTicks
         | sickness @ sicknessOnsetFraction
         v
       [BURST]  VFX + knockback; convertTo ALIEN (or manual spawn)
         |
         v
       [ALIEN]
         | ThrowEggGoal (gen+1 projectile)
         | LayEggGoal   (gen+1 block BE)
         | both gated by PopulationCaps
         v
       back to eggs
```

Host validity (`HatchlingEntity.isValidHost`):
  - alive, not AlienEntity, no existing passengers
  - if whitelist non-empty: type ∈ whitelist
  - else: AnimalEntity (or Player if infectPlayers) and not in blacklist

Generation model:
  - Player / worldgen eggs & spawned larvae → generation **0**
  - Alien inherits larva generation on burst
  - Alien whose generation is `>= generationCap` cannot reproduce
  - Eggs created by aliens store `generation = alien.generation + 1`
  - Hatched larva reads that generation from block entity / thrown egg

Peaceful: larva and alien override `isDisallowedInPeaceful()` → **false**
so acceptance tests on Peaceful Superflat remain valid. Aliens still
target players via `ActiveTargetGoal` when a player is present.

====================================================================
5. CORE CODE — IMPLEMENT AS SPECIFIED (CURRENT)
====================================================================

--- HatchlingEggBlock + HatchlingEggBlockEntity ---

Block settings: strength 0.5, SLIME sounds, ticksRandomly, nonOpaque,
`luminance` from `feedback.eggGlowLevel` (default 6) scaled by `EGGS`
(+2 per extra egg, capped at 15). Collision/outline: low nest VoxelShape
(~5px tall) so players can walk over a nest.

**EGGS** (`IntProperty` 1..3, default 1): cluster size. Blockstates map
`eggs=1|2|3` → `models/block/hatchling_egg_{1,2,3}.json` (hand-authored
element clusters — not a cube). Placing another hatchling egg item onto
an existing cluster increments `EGGS` up to 3 (turtle-egg style).
Breaking removes the whole cluster and drops `EGGS` items when
`eggAlwaysDrops` or Silk Touch.

randomTick: proximity gate + 1-in-N chance → hatch; also proximity
heartbeat when a valid host is nearby.
scheduledTick: ~`eggProximityHeartbeatIntervalTicks` heartbeat while
host in `eggProximityRadius`.
randomDisplayTick (client): idle CRIMSON_SPORE; faster when host nearby
(`feedback.particlesEnabled`).
onSteppedOn: 25% chance hatch (server).
Drops: if `eggAlwaysDrops` (default true), drop `EGGS` items on survival
break without silk; else silk-only / hatch-on-break as before.
**[SUPERSEDED]** Original “Silk Touch only; otherwise hatch” as the
only drop mode — still available when `eggAlwaysDrops=false`.

Hatch reads generation from `HatchlingEggBlockEntity` (default 0) and
spawns **one larva per egg** in the cluster, capped by
`limits.maxLarvaeInRadius` (WARN if capped).

Textures: original 16×16 `block/hatchling_egg.png` and
`item/hatchling_egg.png` (Hatchling palette only — not Mojang assets).

--- HatchlingEggItem extends BlockItem ---

- useOnBlock: place block (inherited).
- use: throw `ThrownHatchlingEggEntity` with `eggThrowVelocity`,
  cooldown `eggThrowCooldownTicks`, hatch SFX pitch via vanilla egg throw.

--- ThrownHatchlingEggEntity extends ThrownItemEntity ---

On collision (server): spawn larva at hit + `thrownEggSpawnYOffset`,
play hatch, CRIMSON_SPORE ×12, discard. Entity hits honor
`thrownEggHatchesOnEntityHit`. Player hit + `infectPlayers=false`: hatch
plus `eggPlayerHitEffectTicks` of SLOWNESS/NAUSEA (no player infection).
Do not force-mount; SeekHostGoal latches.
Client tick: short CRIMSON_SPORE trail while in flight
(`feedback.particlesEnabled`). Client renderer: FlyingItemEntityRenderer.
Registry id: `hatchling:thrown_hatchling_egg`.

--- HatchlingEntity extends PathAwareEntity ---

Larva entity (not Parasite). Registry id: `hatchling:hatchling`.
Fields: infectionTicks, generation. Dimensions 0.5×0.4.
Goals: SeekHostGoal, WanderAroundFar, LookAround.
Incubation, particles, heartbeat, sickness as originally specified.
burst():
  stopRiding → optional createExplosion(power, NONE|MOB) → knockback
  → CRIMSON_SPORE×60 + EXPLOSION + LARGE_SMOKE → BURST + explode SFX
  → drop host loot table → convertTo(ALIEN) or manual spawn
  → alien.setGeneration(larva.generation) → larva.discard()
isDisallowedInPeaceful → false. Fall damage immune. Not pushable while riding.
NBT: InfectionTicks, Generation.

--- SeekHostGoal ---

Unchanged control flow; filters through `isValidHost` (whitelist aware).

--- AlienEntity extends HostileEntity ---

Goals (priority order):
  1 MeleeAttackGoal
  2 ThrowEggGoal
  3 LayEggGoal
  4 WanderAroundFarGoal(alienWanderSpeed)
  5 LookAtEntityGoal(Player)
  6 LookAroundGoal
Targets: Revenge, Player, Animals if alienTargetsAnimals.
No sunlight burn. isDisallowedInPeaceful → false.
Loot: chitin + rare hatchling_egg. NBT: Generation, lifespan age.
**Lifespan (M9):** on spawn, roll `alienLifespanTicks + random[0, variance]`.
At `alienDecayWarningFraction` of lifespan → decay (slowness, smoke/soul
particles, attack dampened); LayEggGoal/ThrowEggGoal no-op while decaying.
At full age → dieOfAge (loot table, no explosion).

--- LayEggGoal ---

Requires `alienLaysEggs` + not decaying + `PopulationCaps.canReproduce`
+ interval/chance + local egg count < alienMaxEggsInRadius + valid placement.
Places egg, sets BE generation to alien.generation + 1.

--- ThrowEggGoal ---

Requires `alienThrowsEggs` + not decaying + PopulationCaps + interval/chance
+ visible target in range. Prefer player when `alienThrowsAtPlayers`, else
valid host. Windup particles, then throw with arc/inaccuracy.
Thrown egg generation = alien.generation + 1.

--- PopulationCaps ---

Shared gate for LayEggGoal and ThrowEggGoal:
  reproductionEnabled
  alien.generation < generationCap
  nearby aliens < maxAliensInRadius
  nearby larvae < maxLarvaeInRadius
  nearby egg blocks < maxEggBlocksInRadius
Radius: populationCheckRadius (egg block scan also capped by alienEggCheckRadius).
Throttled WARN logs per chunk via populationCapWarnIntervalTicks.

--- Registration ---

FabricDefaultAttributeRegistry for HATCHLING and ALIEN (hard crash if omitted).
Spawn eggs + creative tab `hatchling:main`.
Block entity type for hatchling egg.

====================================================================
6. ARTWORK
====================================================================

**M9 default:** `feedback.useCustomModels=true` → `HatchlingModel` /
`AlienModel` (from `art/hatchling_larva.bbmodel` /
`art/hatchling_alien.bbmodel`). Set `false` and **restart** for Phase 1
legacy silverfish/enderman renderers.

Riding offset **[SUPERSEDED by M6 FIX A]**:
  Original draft: translate (0, hostHeight*0.75, -0.15) while riding.
  CURRENT: do **not** stack hostHeight translate — vanilla passenger
  attachment already places the larva. Keep scale 0.8. Optional fine
  tune only via `feedback.hatchlingRenderYOffset` (default 0.0;
  formerly `larvaRenderYOffset`).

TEXTURE PATHS:
  assets/hatchling/textures/entity/hatchling.png       64x64 (custom model)
  assets/hatchling/textures/entity/alien.png          128x128 (Blockbench export)
  assets/hatchling/textures/block/hatchling_egg.png    16x16
  assets/hatchling/textures/item/chitin.png           16x16
  assets/hatchling/textures/item/hatchling_spawn_egg.png

PALETTE:
  bile green      #7ea832
  deep rot        #3f5418
  membrane pink   #c2708a
  wet highlight   #d9e8a8
  void black      #14180d
  egg glow        #a8ff5c

Source Blockbench: `art/hatchling_larva.bbmodel`, `art/hatchling_alien.bbmodel`.
See CREDITS.md before public release.

====================================================================
7. SOUND
====================================================================

ModSounds: EGG_HATCH, HEARTBEAT, BURST, ALIEN_AMBIENT, ALIEN_HURT, ALIEN_DEATH.

PHASE 1 (vanilla remaps):
  EGG_HATCH   → ENTITY_SLIME_SQUISH, pitch 0.6
  HEARTBEAT   → BLOCK_NOTE_BLOCK_BASEDRUM, pitch 0.5
  BURST       → ENTITY_RAVAGER_ROAR, pitch 0.5
  ALIEN_*     → ENTITY_HOGLIN_* at pitch 0.7

Burst also plays ENTITY_GENERIC_EXPLODE with
`burstExplodeSoundVolume` / `burstExplodeSoundPitch`.

PHASE 2: real .ogg under assets/hatchling/sounds/ + sounds.json.
Accelerating heartbeat is the highest-value custom audio asset.

====================================================================
8. WORLDGEN
====================================================================

Data-driven JSON under data/hatchling/worldgen/:
  configured_feature/hatchling_egg_cluster.json
    type: minecraft:ore, **size hardcoded to 4**
    targets: stone_ore_replaceables + deepslate_ore_replaceables
  placed_feature/hatchling_egg_placed.json
    count 2, in_square, height uniform -60..20, biome

BiomeModifications.addFeature(... UNDERGROUND_DECORATION ...)
guarded by `cfg.worldgen.generateEggs`.

**LIMITATION:** `config.worldgen.eggClusterSize` is documentation /
future code-feature only. Changing it in hatchling.json does **not**
change the JSON ore size until a code-based feature reads it.

====================================================================
9. MILESTONES
====================================================================

Original plan (historical):
  M1 scaffold → M2 config → M3 entities/render → M4 seek/latch →
  M5 incubate/burst → M6 egg block + LayEggGoal → M7 worldgen →
  M8 polish.

Current shipped surface (fold-in complete):
  M1–M5 as above.
  **M6 REVISION (2026-08-10):** FIX A render, cow whitelist, throwable egg,
  eggAlwaysDrops, generation BE.
  **M7 REVISION (2026-08-10):** burst VFX/knockback, ThrowEggGoal,
  PopulationCaps + limits, Peaceful survival.
  **M8:** egg clusters (`EGGS` 1–3) + proximity feedback — §5 notes remain.
  **M9 REVISION (2026-08-10):** HatchlingEntity IDs, custom models,
  hatchlingRenderYOffset, player egg-throw targeting (no infectPlayers),
  alien lifespan/decay.
  Worldgen remains part of the baseline build.

ACCEPTANCE — core lifecycle (Creative Superflat, Peaceful OK):
  1. Spawn cow; throw or place+hatch hatchling egg near it.
  2. Larva paths, latches, sits ON the back (not floating).
  3. Pig nearby is ignored (whitelist).
  4. At 50% incubation: slowness + nausea.
  5. Save/reload mid-incubation; InfectionTicks resume.
  6. At incubationTicks: burst VFX, cow → alien, alien hostile.
  7. Kill larva mid-incubation once: host survives.
  8. Alien eventually lays/throws eggs until caps/generation stop it.

See also LIFECYCLE.md and RUNNING.md §6.

====================================================================
10. KNOWN HARD PARTS
====================================================================

- Force-mounting / passenger attachment vs render offset (M6 FIX A).
- convertTo() null → manual spawn path; generation must still copy.
- Burst explosion power 0.0 with NONE still plays FX — do not confuse
  with “explosion disabled” (`burstExplosionEnabled=false`).
- Population caps + generationCap can look like “aliens never lay”
  during soak tests — check logs for cap WARN.
- Worldgen ore in air pockets if replaceables wrong.
- Gradle / JDK mismatch — paste real errors; do not guess.
- If startRiding proves unstable, leash fallback exists as design
  escape hatch — do NOT switch without asking.

====================================================================
11. WHAT NOT TO DO
====================================================================

- Do not add dependencies beyond Fabric API without asking.
- Do not create an "infected cow" entity type. Larva-as-passenger is deliberate.
- Do not add a GUI config screen (no Cloth Config) in v1.
- Do not enable infectPlayers unless explicitly requested.
- Do not reintroduce hostHeight*0.75 riding translate (FIX A).
- Do not make aliens/larvae despawn in Peaceful (acceptance depends on it).
- Do not commit run/ or build/ or .gradle/.
- Do not bump MC/Yarn/Loader/API/Loom/Gradle/Java versions silently.
