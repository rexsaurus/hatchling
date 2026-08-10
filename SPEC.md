# HATCHLING — Cursor Implementation Spec
# Repo: git@github.com:rexsaurus/hatchling.git
# Author: Rex St John

====================================================================
0. RULES FOR CURSOR — READ FIRST
====================================================================

1. TARGET VERSIONS ARE FIXED. Minecraft 1.21.1, Fabric Loader 0.16.x,
   Fabric API 0.102.x+1.21.1, Yarn mappings, Java 21, Gradle 8.x.
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

6. ALL SERVER-SIDE LOGIC MUST GUARD `if (world.isClient) return;`
   before mutating state or spawning entities.

7. PERSISTENCE IS REQUIRED. Any field that represents lifecycle
   progress must round-trip through writeCustomDataToNbt /
   readCustomDataFromNbt. Test by saving and reloading the world.

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

A horror-flavored parasite lifecycle mod. Eggs generate in deep caves.
When a warm-blooded animal wanders near, the egg hatches into a fast
larva. The larva hunts the nearest animal, latches onto it, and
incubates. The host visibly sickens. When the timer expires the host
bursts and is REPLACED by a hostile alien. Adult aliens occasionally
lay new eggs, closing the loop so an unchecked infestation spreads.

Design pillars:
  - The threat is SLOW then SUDDEN. Long quiet incubation, violent burst.
  - The player can INTERVENE. The larva is a separate entity with its
    own hitbox and low HP — killing it mid-incubation saves the host.
  - It SPREADS. Ignoring it must be punished.

Mod id:        hatchling
Root package:  com.rexsaurus.hatchling
Display name:  Hatchling
License:       MIT

====================================================================
2. PACKAGE STRUCTURE
====================================================================

com.rexsaurus.hatchling
  Hatchling.java                 ModInitializer entry point
  config/
    HatchlingConfig.java         POJO + GSON load/save
  registry/
    ModBlocks.java
    ModItems.java
    ModEntities.java
    ModSounds.java
    ModWorldgen.java
  block/
    ParasiteEggBlock.java
  entity/
    ParasiteEntity.java
    AlienEntity.java
    goal/
      SeekHostGoal.java
      LayEggGoal.java
com.rexsaurus.hatchling.client
  HatchlingClient.java           ClientModInitializer entry point
  render/
    ParasiteRenderer.java
    AlienRenderer.java

fabric.mod.json declares BOTH entrypoints:
  "main":   ["com.rexsaurus.hatchling.Hatchling"]
  "client": ["com.rexsaurus.hatchling.client.HatchlingClient"]

====================================================================
3. CONFIG FILE — BUILD THIS FIRST
====================================================================

Path: config/hatchling.json (created with defaults on first run)
Loader: GSON (bundled with Minecraft, no new dependency).
Load in Hatchling.onInitialize() BEFORE any registration that reads it.
Static access: HatchlingConfig.get()

Implementation notes:
  - Fields are public and primitive/boxed. No getters needed.
  - On load: if file missing, write defaults. If a field is missing
    from an existing file, GSON leaves the default — this is desired
    forward-compatibility. If parsing throws, log a WARN, fall back to
    defaults, and DO NOT overwrite the user's file.
  - Add a `clamp()` method called after load that forces sane bounds
    (no zero/negative durations, radius <= 64) and logs any correction.
  - Provide `/hatchling reload` command (server op level 2) that
    re-reads the file at runtime. Registered via
    CommandRegistrationCallback.

Default file contents:

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
    "alienEggCheckRadius": 16.0
  },
  "stats": {
    "larvaHealth": 4.0,
    "larvaSpeed": 0.45,
    "larvaChaseSpeedMultiplier": 1.3,
    "alienHealth": 30.0,
    "alienAttackDamage": 6.0,
    "alienSpeed": 0.32,
    "alienFollowRange": 32.0
  },
  "targeting": {
    "infectPlayers": false,
    "hostBlacklist": ["minecraft:wolf", "minecraft:cat", "minecraft:parrot"],
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
    "hostGlowsWhenInfected": false
  }
}

Config semantics Cursor must honor:
  - eggHatchRandomTickChance = N means 1-in-N per random tick.
  - sicknessOnsetFraction 0.5 => status effects applied at 50% of
    incubationTicks.
  - hostBlacklist entries are entity type IDs parsed via
    Registries.ENTITY_TYPE.get(Identifier.of(s)). Invalid IDs log WARN
    and are skipped, never crash.
  - infectPlayers=false is the DEFAULT. Player infection is Milestone 7
    and stays off until explicitly enabled.

====================================================================
4. LIFECYCLE STATE MACHINE
====================================================================

  [EGG BLOCK]
     |  randomTick, 1-in-eggHatchRandomTickChance,
     |  gated on animal within eggProximityRadius
     v
  [LARVA — unattached]
     |  SeekHostGoal: find nearest valid AnimalEntity within
     |  larvaHostSearchRadius, path to it, startRiding when within
     |  larvaLatchDistance
     v
  [LARVA — riding host]  <-- interruptible: kill larva to cure host
     |  tick counter to incubationTicks
     |  at sicknessOnsetFraction: SLOWNESS II + NAUSEA on host
     |  particles every 20 ticks, heartbeat SFX accelerating
     v
  [BURST]
     |  larva.stopRiding(); particle + sound burst;
     |  host.convertTo(ALIEN); larva.discard()
     v
  [ALIEN — hostile]
     |  LayEggGoal every alienEggLayIntervalTicks
     v
  back to [EGG BLOCK]

Validity rule for a host candidate (single method, used by both
SeekHostGoal.canStart and the latch check):

  ParasiteEntity.isValidHost(LivingEntity e):
    - instanceof AnimalEntity (or PlayerEntity if infectPlayers)
    - e.isAlive() && !e.isBaby() is NOT required — babies allowed
    - !e.hasPassengers()   (one parasite per host)
    - type ID not in hostBlacklist
    - !(e instanceof AlienEntity)

====================================================================
5. CORE CODE — IMPLEMENT AS SPECIFIED
====================================================================

--- ParasiteEggBlock ---

extends Block. Settings: .strength(0.5f), .sounds(BlockSoundGroup.SLIME),
.ticksRandomly(), .nonOpaque(), .luminance(s -> 3).

Override randomTick(state, world, pos, random):
  cfg = HatchlingConfig.get()
  if (cfg.lifecycle.eggRequiresNearbyAnimal):
    Box area = new Box(pos).expand(cfg.lifecycle.eggProximityRadius)
    if world.getEntitiesByClass(AnimalEntity.class, area, e -> true).isEmpty() return
  if (random.nextInt(cfg.lifecycle.eggHatchRandomTickChance) != 0) return
  world.breakBlock(pos, false)
  spawn ParasiteEntity at pos center
  play ModSounds.EGG_HATCH at pos

Also override onSteppedOn: 25% chance to hatch immediately (player
stepping on it is the horror beat). Guard for server side.

Drops itself with Silk Touch only; otherwise drops nothing and hatches.

--- ParasiteEntity extends PathAwareEntity ---

Fields: private int infectionTicks;
Dimensions: 0.5f wide, 0.4f tall.
Attributes from config: MAX_HEALTH, MOVEMENT_SPEED.
initGoals: priority 1 SeekHostGoal, priority 2 WanderAroundFarGoal(1.0),
           priority 3 LookAroundGoal.

tick():
  super.tick(); if (getWorld().isClient) return;
  Entity host = getVehicle();
  if (host instanceof LivingEntity living && isValidHostShape(living)) {
      infectionTicks++;
      int total = cfg.lifecycle.incubationTicks;
      if (cfg.feedback.particlesEnabled && infectionTicks % 20 == 0)
          spawn SCULK_SOUL particles at host body center, count 3
      if (infectionTicks == (int)(total * cfg.lifecycle.sicknessOnsetFraction)) {
          apply SLOWNESS amplifier 1 and NAUSEA amplifier 0 for the
          remaining duration
      }
      if (cfg.feedback.heartbeatSoundEnabled) {
          // interval shrinks from 40 ticks to 8 ticks as progress -> 1
          int interval = MathHelper.lerp(progress, 40, 8) rounded
          if (infectionTicks % interval == 0) play ModSounds.HEARTBEAT
      }
      if (infectionTicks >= total) burst(living);
  } else {
      infectionTicks = 0;   // reset if knocked off
  }

burst(LivingEntity host):
  server-only. stopRiding().
  CRIMSON_SPORE particles x60 at host center, spread 0.4, speed 0.1
  play ModSounds.BURST
  if (host instanceof MobEntity mob) {
      AlienEntity alien = mob.convertTo(ModEntities.ALIEN, false);
      if (alien != null) alien.setHealth(alien.getMaxHealth());
  }
  discard();

  NOTE: convertTo returns null if the entity is removed or conversion
  fails. Handle the null branch by spawning the alien manually at the
  host position and discarding the host. Do not NPE.

NBT: persist "InfectionTicks".

Also override: canBeLeashed -> false, isPushable -> false while riding,
and make the larva immune to fall damage.

--- SeekHostGoal extends Goal ---

Controls: MOVE, LOOK.
canStart: !parasite.hasVehicle() && a valid host exists within
          larvaHostSearchRadius (use world.getClosestEntity with a
          TargetPredicate, filtered by isValidHost).
shouldContinue: target alive, still valid, parasite not riding.
tick: look at target, navigation.startMovingTo(target,
      larvaChaseSpeedMultiplier); when squaredDistanceTo < 
      larvaLatchDistance^2 -> parasite.startRiding(target, true)
stop: clear target, stop navigation.

Force-mount caveat: startRiding(target, true) bypasses the normal
"can this be ridden" check. Expect render clipping — handled in the
renderer, not here.

--- AlienEntity extends HostileEntity ---

Attributes from config. Dimensions 0.7f x 2.1f.
initGoals:
  goalSelector 1: MeleeAttackGoal(this, 1.1, false)
  goalSelector 2: LayEggGoal(this)
  goalSelector 3: WanderAroundFarGoal(this, 1.0)
  goalSelector 4: LookAtEntityGoal(PlayerEntity, 8.0f)
  targetSelector 1: RevengeGoal(this)
  targetSelector 2: ActiveTargetGoal<>(this, PlayerEntity.class, true)
  targetSelector 3: ActiveTargetGoal<>(this, AnimalEntity.class, false)
                    — only if cfg.targeting.alienTargetsAnimals

Sunlight: does NOT burn. It is not undead.
Loot: 1-2 of a new item `hatchling:chitin` plus rare `parasite_egg`.

--- LayEggGoal extends Goal ---

canStart: cfg.lifecycle.alienLaysEggs
          && ++cooldown >= alienEggLayIntervalTicks
          && random < alienEggLayChance
          && count of ParasiteEggBlock within alienEggCheckRadius
             < alienMaxEggsInRadius
          && a valid placement pos exists (solid block below, air at
             pos, within 3 blocks of the alien)
start: place ModBlocks.PARASITE_EGG, play sound, reset cooldown.
This goal completes in a single tick — shouldContinue returns false.

Counting eggs: iterate BlockPos.iterate over the radius box. Cap the
radius at 16 and skip the check entirely if the chunk isn't loaded.

--- ModEntities registration ---

Registry.register(Registries.ENTITY_TYPE, Identifier.of(MOD_ID, "parasite"),
  EntityType.Builder.create(ParasiteEntity::new, SpawnGroup.MONSTER)
    .dimensions(0.5f, 0.4f).build());

CRITICAL: in Hatchling.onInitialize(), call
  FabricDefaultAttributeRegistry.register(ModEntities.PARASITE,
      ParasiteEntity.createAttributes());
for BOTH entities. Omitting this is a hard crash on spawn.

Also register spawn eggs (ModItems) for both entities so they can be
tested without worldgen. Add them to a creative tab.

====================================================================
6. ARTWORK
====================================================================

PHASE 1 — NO ORIGINAL ART REQUIRED. Ship mechanics first.
  ParasiteRenderer extends MobEntityRenderer with
    SilverfishEntityModel(ctx.getPart(EntityModelLayers.SILVERFISH))
  AlienRenderer extends BipedEntityRenderer-style with
    EndermanEntityModel or ZombieEntityModel
  Point getTexture() at OUR namespace paths from day one so swapping
  in real art later requires zero code changes.

Riding offset: in ParasiteRenderer.render(), when
entity.hasVehicle(), translate +0.0 / +hostHeight*0.75 / -0.15 and
scale to 0.8 so the larva sits ON the host's back rather than inside
its ribcage. This is the single most important visual fix.

TEXTURE PATHS (create these files even if Phase 1 copies vanilla):
  assets/hatchling/textures/entity/parasite.png       64x32
  assets/hatchling/textures/entity/alien.png          64x64
  assets/hatchling/textures/block/parasite_egg.png    16x16
  assets/hatchling/textures/item/chitin.png           16x16
  assets/hatchling/textures/item/parasite_spawn_egg.png (vanilla template)

PALETTE — use these, nothing else. The mod reads as "ours" because
nothing in vanilla is this color combination.
  bile green      #7ea832
  deep rot        #3f5418
  membrane pink   #c2708a
  wet highlight   #d9e8a8
  void black      #14180d
  egg glow        #a8ff5c   (emissive accent only)

Art direction: wet, segmented, chitinous. Asymmetry sells "wrong."
The egg block should read as translucent with a dark curled shape
suspended inside — bulge it slightly with a 14x14x14 cube model rather
than a full block, and set nonOpaque so it renders with soft edges.

PHASE 2 — Blockbench (blockbench.net), Minecraft Entity template.
Priority order for original models: alien first (it is the set piece
players actually look at), egg block second, larva last — a recolored
silverfish reads fine forever.
Remember entity textures are UV-unwrapped box nets, not a drawing
surface. Build the model, then paint the exported net.

MODELS / BLOCKSTATES to author:
  assets/hatchling/blockstates/parasite_egg.json  -> single variant
  assets/hatchling/models/block/parasite_egg.json -> cube_all variant
       with 14x14x14 inner cube
  assets/hatchling/models/item/parasite_egg.json  -> parent block model
  assets/hatchling/models/item/chitin.json        -> item/generated
  assets/hatchling/lang/en_us.json                -> ALL display names,
       including entity.hatchling.parasite, entity.hatchling.alien,
       block.hatchling.parasite_egg, item.hatchling.chitin,
       itemGroup.hatchling.main

====================================================================
7. SOUND
====================================================================

ModSounds registers: EGG_HATCH, HEARTBEAT, BURST, ALIEN_AMBIENT,
ALIEN_HURT, ALIEN_DEATH.

PHASE 1: map each to a vanilla SoundEvent so nothing is silent —
  EGG_HATCH   -> ENTITY_SLIME_SQUISH, pitch 0.6
  HEARTBEAT   -> BLOCK_NOTE_BLOCK_BASEDRUM, pitch 0.5
  BURST       -> ENTITY_RAVAGER_ROAR, pitch 0.5
  ALIEN_*     -> ENTITY_HOGLIN_* at pitch 0.7

PHASE 2: real .ogg files at assets/hatchling/sounds/ + sounds.json.
The accelerating heartbeat is the highest-value audio asset in the
mod. Build it early even if everything else stays vanilla.

====================================================================
8. WORLDGEN
====================================================================

Data-driven JSON, not code. Under data/hatchling/worldgen/:
  configured_feature/parasite_egg_cluster.json
    type: minecraft:random_patch (or ore with deepslate replaceables)
    cluster size from config is NOT readable in JSON — hardcode 4 here
    and treat config.worldgen.eggClusterSize as documentation for a
    future code-based feature. NOTE THIS LIMITATION explicitly.
  placed_feature/parasite_egg_placed.json
    count: 2, in_square, height range uniform -60 to 20,
    biome filter
  Add to biomes via BiomeModifications.addFeature(
    BiomeSelectors.foundInOverworld(), UNDERGROUND_DECORATION, key)
  Guard the whole registration behind cfg.worldgen.generateEggs.

====================================================================
9. MILESTONES — DO THEM IN THIS ORDER
====================================================================

M1  Scaffold builds. fabric.mod.json correct, both entrypoints fire,
    logger prints on init. `./gradlew runClient` opens a world.
M2  HatchlingConfig loads/saves/clamps. /hatchling reload works.
M3  Both entities registered with attributes + spawn eggs. Phase 1
    vanilla renderers. Spawn them from creative and see them.
M4  SeekHostGoal: larva chases and latches onto a cow. Riding offset
    correct in the renderer.
M5  Incubation timer, sickness effects, particles, heartbeat, burst
    into alien. Save/reload mid-incubation preserves progress.
M6  Egg block: place, random-tick hatch, proximity gate, silk touch.
    LayEggGoal closes the loop.
M7  Worldgen. Find an egg in a fresh world without cheats.
M8  Polish: sounds, loot tables, advancements, custom art.

ACCEPTANCE TEST for M5 (run this exact sequence):
  1. Creative, flat world, spawn a cow.
  2. Spawn a parasite 15 blocks away. It must path to the cow and latch
     within ~10 seconds.
  3. At 300 ticks the cow visibly slows and staggers.
  4. Save and quit at ~400 ticks. Reload. Timer must resume, not reset.
  5. At 600 ticks the cow is gone and an alien stands in its place at
     full health, immediately hostile to the player.
  6. Repeat, but kill the larva at 300 ticks. The cow must survive and
     its status effects must expire normally.

====================================================================
10. KNOWN HARD PARTS — EXPECT TO SPEND TIME HERE
====================================================================

- Force-mounting. The host's own AI keeps running while carrying a
  passenger. Watch for: larva clipping inside the model, host pathing
  through 1-block gaps, dismount on host death, dismount on chunk
  unload. If startRiding proves unstable, the fallback design is a
  "leash" — larva stays a free entity, snaps its position to the host
  every tick, and tracks the host by UUID in NBT. Do NOT switch to
  the fallback without telling me first.
- convertTo() null return and attribute copying.
- Deepslate replaceables in the ore feature — eggs generating in air
  pockets rather than in stone is the usual failure.
- Gradle. When it breaks, paste the real error; do not guess.

====================================================================
11. WHAT NOT TO DO
====================================================================

- Do not add dependencies beyond Fabric API without asking.
- Do not create an "infected cow" entity type. The larva-as-passenger
  design is deliberate — it works on every animal automatically and
  gives the player a hitbox to attack.
- Do not add a GUI config screen (no Cloth Config) in v1.
- Do not implement player infection until M7 and only with
  infectPlayers explicitly true.
- Do not commit run/ or build/.
