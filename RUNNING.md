# Hatchling — Running From Zero

Complete guide for someone who has never built a Minecraft Fabric mod.
Primary OS: **macOS**. Windows notes are inline.

Pinned toolchain (do not bump):

| Piece | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Yarn | 1.21.1+build.3 |
| Fabric Loader | 0.16.14 |
| Fabric API | 0.102.1+1.21.1 |
| Loom | 1.7.4 |
| Java | 21 |
| Gradle | 8.10 (wrapper) |

Related: [SPEC.md](SPEC.md) · [LIFECYCLE.md](LIFECYCLE.md) · [README.md](README.md)

====================================================================
1. Prerequisites
====================================================================

You need a **JDK 21** (full JDK, not a JRE-only install, not Java 8).

### Why not Java 8?

Old “Java for Minecraft” / browser applet installs are Java 8 (or older).
This mod and modern Fabric Loom **require Java 21**. If `java -version`
shows `1.8.x`, builds will fail with unsupported class-file / toolchain
errors. Uninstall or ignore the applet JDK; install Temurin 21 instead.

### macOS — install JDK 21

With Homebrew:

```bash
brew install --cask temurin@21
# or: brew install --cask temurin
# Adoptium/Temurin 21 is the recommended distribution
```

List installed JVMs:

```bash
/usr/libexec/java_home -V
```

Pick 21 and export `JAVA_HOME` (Apple Silicon Homebrew path may vary;
`java_home` is authoritative):

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Persist for bash:

```bash
echo 'export JAVA_HOME="$(/usr/libexec/java_home -v 21)"' >> ~/.bash_profile
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.bash_profile
source ~/.bash_profile
```

Persist for zsh (default on modern macOS):

```bash
echo 'export JAVA_HOME="$(/usr/libexec/java_home -v 21)"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

Verify:

```bash
java -version
# Expect: openjdk version "21.x.x" (Temurin/Adoptium/OpenJDK 21)
javac -version
```

### Windows notes

1. Download **Eclipse Temurin 21 (JDK)** from [Adoptium](https://adoptium.net/).
2. Install with “Set JAVA_HOME variable” / add to PATH checked if offered.
3. Or set manually: System Properties → Environment Variables →
   `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-21.x.x-hotspot`
   and add `%JAVA_HOME%\bin` to Path.
4. Open a **new** PowerShell/cmd and run `java -version`.

Also useful: Git for Windows, and a terminal that can run `.\gradlew.bat`.

====================================================================
2. Clone and first build
====================================================================

```bash
git clone git@github.com:rexsaurus/hatchling.git
cd hatchling
```

HTTPS alternative:

```bash
git clone https://github.com/rexsaurus/hatchling.git
cd hatchling
```

Ensure Java 21 is active, then:

```bash
# macOS / Linux
chmod +x gradlew
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

First run downloads Gradle 8.10, Minecraft, Yarn, Fabric — can take several
minutes and looks idle. Success ends with `BUILD SUCCESSFUL`.

Output jar (example):

```
build/libs/hatchling-0.1.0.jar
```

Dev play uses Loom’s `runClient` / `runServer` (next sections), not a
manual launcher install.

====================================================================
3. Running the client
====================================================================

From the repo root:

```bash
./gradlew runClient
```

Windows: `gradlew.bat runClient`

A Minecraft window opens with Hatchling already loaded (Fabric dev env).

Create a test world optimized for acceptance:

1. **Singleplayer** → **Create New World**
2. **Game Mode:** Creative
3. **Difficulty:** Peaceful (larvae/aliens are allowed in Peaceful in this mod)
4. Open **World** tab → **World Type:** Superflat
5. Create the world

Config is written on first launch to:

```
run/config/hatchling.json
```

Creative inventory → **Hatchling** tab for Hatchling Egg, spawn eggs, chitin.

====================================================================
4. Running dedicated server (optional)
====================================================================

**Most testers should skip this.** Singleplayer `runClient` is easier for
lifecycle acceptance. Use a dedicated server only if you need multiplayer
or server-only repro.

### Start the server

```bash
./gradlew runServer
```

Windows: `gradlew.bat runServer`

### EULA

On first run the server stops until you accept the EULA.

**macOS / Linux** — from the repo root, after `run/eula.txt` exists:

```bash
sed -i '' 's/eula=false/eula=true/' run/eula.txt   # macOS BSD sed
# Linux GNU sed: sed -i 's/eula=false/eula=true/' run/eula.txt
```

**Windows** — open `run\eula.txt` in Notepad and change:

```
eula=false
```

to:

```
eula=true
```

Save, then run `gradlew.bat runServer` again.

### “Stuck” at ~87% EXECUTING

Seeing Gradle sit at something like **87% EXECUTING** while the server
console is up is **normal**. The Gradle task stays alive for the whole
server process. Watch the **Minecraft server log** in that same terminal
(`Done (` … `For help, type "help"`), not the Gradle percentage.

### Join from a second terminal

Leave `runServer` running. In another terminal:

```bash
./gradlew runClient
```

Connect to **`localhost`** (Multiplayer → Direct Connection → `localhost`).

### online-mode and ops

For local offline testing, edit `run/server.properties`:

```
online-mode=false
```

Restart the server after changing it. Then op yourself from the server
console (exact name as shown when you join):

```
op YourPlayerName
```

Again: for Hatchling lifecycle work, **singleplayer is easier**.

====================================================================
5. Spawning and testing commands
====================================================================

In a Creative world (cheats on), useful commands:

```mcfunction
# Host
/summon minecraft:cow ~10 ~ ~

# Non-host (should be ignored when whitelist is cow-only)
/summon minecraft:pig ~5 ~ ~

# Larva / alien directly
/summon hatchling:hatchling ~ ~ ~
/summon hatchling:alien ~ ~ ~

# Items
/give @s hatchling:hatchling_egg 16
/give @s hatchling:hatchling_spawn_egg 1
/give @s hatchling:alien_spawn_egg 1

# Reload config after editing run/config/hatchling.json
/hatchling reload
```

Manual throw test: take **Hatchling Egg** from the Hatchling tab.
- Right-click a **block face** → places egg block.
- Right-click **air** → throws projectile (must be visible in flight).

Break a placed egg in survival (or Adventure with drops) → item returns
when `eggAlwaysDrops` is true (default).

**Custom models:** `feedback.useCustomModels` (default `true`) selects
`HatchlingModel` / `AlienModel` at client init. Changing it requires a
**full client restart** — `/hatchling reload` does **not** re-register
renderers.

====================================================================
6. Watching the full lifecycle
====================================================================

Use the acceptance sequence from SPEC.md (M5–M9 fold-in):

1. Creative Superflat, **Peaceful** OK.
2. `/summon minecraft:cow ~10 ~ ~`
3. Throw a Hatchling Egg near the cow (or `/summon hatchling:hatchling`).
4. Larva paths, latches, sits **on** the cow’s back (not floating).
5. Optional: summon a pig — larva should **ignore** it (whitelist).
6. ~15s (`sicknessOnsetFraction` 0.5 of 600 ticks): cow slows / nausea.
7. Save & quit mid-timer; reload — infection must **resume**, not reset.
8. ~30s total: burst VFX + knockback; cow becomes alien at full health.
9. Alien eventually throws/lays eggs until population/generation caps
   (and later lifespan/decay) stop it.
10. Repeat once, killing the riding larva at ~15s — cow must survive.

Timing details and presets: [LIFECYCLE.md](LIFECYCLE.md).

====================================================================
7. Troubleshooting
====================================================================

### `java -version` shows 1.8 / “Java 8 applet” / old Oracle install

**Fix:** Install Temurin JDK 21; set `JAVA_HOME` to that JDK; open a new
terminal. Java 8 cannot build this project.

### `JAVA_HOME` wrong or unset

**Symptoms:** Gradle uses a different JDK than you expect; cryptic
toolchain errors.

**Fix (macOS):**

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
java -version
```

Add the export to `~/.zshrc` or `~/.bash_profile` as in §1.

**Fix (Windows):** Set system `JAVA_HOME` to the Temurin 21 folder; restart
the terminal; `echo %JAVA_HOME%`.

### `Unsupported class file major version` / invalid target release 21

**Cause:** Gradle is running on Java &lt; 21.

**Fix:** Point `JAVA_HOME` at JDK 21, then `./gradlew --stop` and rebuild.

### `Permission denied: ./gradlew`

```bash
chmod +x gradlew
./gradlew build
```

Windows users should call `gradlew.bat`, not `./gradlew`.

### First `./gradlew build` downloads forever / looks hung

**Normal** on first run (Minecraft + mappings). Wait; check network.
Retry with `./gradlew build --info` if it truly stalls with no new output
for 15+ minutes.

### `Could not resolve` Fabric / Loom / Yarn artifacts

**Fix:** Check internet; do not change versions in `gradle.properties`.
Confirm pins match SPEC. Clear only if instructed: stop daemons
(`./gradlew --stop`) and retry. Avoid deleting the whole Gradle cache
unless a maintainer asks.

### Client opens but Hatchling items missing

**Fix:** Confirm log line `Hatchling initialized`. Creative → search
“Hatchling” item group. If using a non-Loom launcher, you must install
the built jar + Fabric API for 1.21.1 yourself (dev path is `runClient`).

### Crash on spawn: missing entity attributes

**Cause:** `FabricDefaultAttributeRegistry.register` omitted for hatchling/alien.

**Fix:** Already present in `Hatchling.onInitialize()` — if you branched
code, restore those two register calls; rebuild.

### Entities vanish immediately on Peaceful

**Historical vanilla behavior for hostiles.** This mod intentionally sets
`isDisallowedInPeaceful()` to **false** on larva and alien.

**Fix:** Update to current code; do not “fix” by forcing Easy if you are
running the documented Peaceful acceptance test.

### Invisible larva / alien (custom models)

**Cause:** Entity model layer not registered (`HATCHLING_LAYER` /
`ALIEN_LAYER` in `HatchlingClient`), or `useCustomModels` mismatch after
editing config without restart.

**Fix:** Confirm `EntityModelLayerRegistry.registerModelLayer` runs for
both layers; restart client after changing `useCustomModels`.

### Black-and-magenta missing texture

**Cause:** Wrong texture path or size for the active model.

**Fix:** Custom path expects
`assets/hatchling/textures/entity/hatchling.png` (**64×64**) and
`alien.png` (**128×128`). Rebuild or F3+T after fixing paths.

### Entity slides without animating

**Cause:** Empty / stub `setAngles` in `HatchlingModel` / `AlienModel`.

**Fix:** Export animation posing from Blockbench into `setAngles`; rebuild.

### Larva latches but floats 1–2 blocks above the cow

**Cause:** Double offset (renderer translate stacked on vanilla passenger
attachment). **M6 FIX A** removed `hostHeight * 0.75`.

**Fix:** Current `HatchlingRenderer` only applies optional
`feedback.hatchlingRenderYOffset` (default `0.0`) + 0.8 scale. If still high,
set a small negative Y offset in config and `/hatchling reload`.

### Larva attacks / latches pigs or sheep

**Cause:** Empty or widened `hostWhitelist`, or whitelist cleared so
blacklist mode returns.

**Fix:** Ensure `targeting.hostWhitelist` includes `"minecraft:cow"`
(default). Reload config.

### Thrown egg invisible in flight

**Cause:** Missing client renderer / spawn packet for the thrown entity.

**Fix:** `HatchlingClient` must register `FlyingItemEntityRenderer` for
`THROWN_HATCHLING_EGG`. Rebuild; do not “fix” by skipping the renderer.

### Egg places but does not throw

Aim at **air** (not a block face). Block targeting always places.
Check `eggThrowCooldownTicks` if spam-clicking feels ignored.

### Timer resets after save/reload

**Cause:** `InfectionTicks` / `Generation` not persisted.

**Fix:** Current entities write NBT keys `InfectionTicks` and `Generation`.
If you are mid-dev on a branch, verify those round-trips.

### Alien never lays or throws

Check logs for `Population cap hit`. Defaults: max 6 aliens / 8 larvae /
5 eggs in radius 48, `generationCap` 4, `reproductionEnabled` true.
Also wait for intervals (throw 600 ticks, lay 2400 ticks) and chance rolls.
Kill extras or raise caps in config; `/hatchling reload`.

### Burst destroys terrain

Defaults use `burstExplosionPower: 0.0` and `burstDamagesBlocks: false`
(`ExplosionSourceType.NONE`). If terrain breaks, you raised power or
enabled block damage — set them back and reload.

### Config edits do nothing

1. Edit `run/config/hatchling.json` (dev) not a random copy.
2. JSON must be valid — parse errors keep old/default in memory and
   **do not** overwrite your file.
3. Run `/hatchling reload` (op level 2) or restart the client.
4. Exception: `feedback.useCustomModels` needs a **restart**, not reload.

### Dedicated server: EULA / can’t start

Accept EULA (§4). macOS `sed` vs Windows hand-edit.

### Dedicated server: can’t join / authentication

Set `online-mode=false` for local offline testing; restart; `op` yourself.
For real online servers leave `online-mode=true` and use a paid account.

### Gradle shows ~87% EXECUTING forever with server

**Not a hang** — the `runServer` task stays active while the server runs.
Use the server log; stop with Ctrl+C when done.

### Dedicated server crash mentioning `net.minecraft.client`

**Cause:** Client-only class imported from common code.

**Fix:** Move renderer/model code under `src/client/java/...` and register
only from `HatchlingClient`.

### Changing `eggClusterSize` in config does nothing

**Expected.** Ore feature JSON hardcodes `"size": 4`. See SPEC.md §8.
`eggClusterSize` is reserved for a future code-driven feature.

### Worldgen eggs not found

Search deepstone Y -60..20 in Overworld caves; confirm
`worldgen.generateEggs` true; fresh chunks after enabling. Superflat
test worlds often have **no** underground ore — use throw/spawn eggs
for lifecycle tests instead.
