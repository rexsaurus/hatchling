# Hatchling — Build It Together

*A guide for making a Minecraft monster with your kid, starting from a computer with nothing on it.*

> NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

---

## What we're making

A parasite.

It starts as an egg. You can throw the egg. When it lands, it cracks open and something small and fast crawls out. It goes looking for the nearest cow.

When it finds one, it climbs on its back and holds on. The cow starts to stumble. It gets slower. Something is wrong with it.

Thirty seconds later the cow explodes, and standing where the cow used to be is something much bigger and much angrier. It doesn't like you. And every so often, it throws an egg of its own.

That's the whole game loop. Egg to bug to cow to monster to egg again.

---

## How to use this guide

**Read the parts in order, but don't do them all in one sitting.** Here's a realistic plan:

| Session | What happens | How long |
|---|---|---|
| **1** | Grown-up installs the tools | 30–45 min, mostly waiting |
| **2** | Play with the monster. Don't build anything. | 30 min |
| **3** | Change numbers in the config file and watch the game change | 30 min |
| **4** | Draw the monsters on paper | As long as you want |
| **5** | Turn the drawings into game art | 1–2 hours |
| **6** | Invent something new | Forever |

**The order matters.** Play with it first. A kid who has watched a cow explode is a kid who wants to know how the explosion works. A kid who has been shown a config file for twenty minutes has stopped listening.

Session 1 is the only boring one. Do it alone, ahead of time, if you can.

---

# Part 1 — Get the tools

*Grown-up drives this part.*

## What you need

- A Mac or PC
- **Minecraft: Java Edition** — this is important. The Windows Store version (Bedrock) cannot run mods like this one. If you're buying it, go to minecraft.net and make sure the checkout says **Java**.
- About 3 GB of free disk space
- Internet

## Step 1.1 — Install Java 21

This is the single most common place people get stuck, so read carefully.

The "Java" you might get from java.com is **Java 8**. It is fifteen years old and it will not work. You need **Java 21**, and specifically a JDK (Java Development Kit), not a JRE.

**On a Mac:**

Open Terminal (Cmd+Space, type "Terminal", Enter). Then:

```bash
brew install --cask temurin@21
```

If it says `brew: command not found`, download the installer instead from [adoptium.net](https://adoptium.net/temurin/releases/?version=21). Choose:
- Operating System: **macOS**
- Architecture: **aarch64** for any Mac from 2020 or later, **x64** for older Intel Macs
- Package Type: **JDK**

Double-click the `.pkg` and follow the installer.

**On Windows:**

Same [adoptium.net](https://adoptium.net/temurin/releases/?version=21) page. Choose Windows, x64, JDK, and download the `.msi`. During install, tick the box that says **Set JAVA_HOME variable**.

## Step 1.2 — Check it worked

**Mac:**

```bash
/usr/libexec/java_home -V
```

You want to see a line with **21** in it. If you also see an old `1.8` entry pointing at something called `JavaAppletPlugin` — that's fine, just ignore it for now.

Now tell your Terminal to use 21 by default:

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

*(If your Terminal prompt ends in `$` and says `bash`, use `~/.bash_profile` instead of `~/.zshrc` in those two lines.)*

**Windows:** open Command Prompt and run `java -version`.

**Either way, you should see `21` something.** If you see `1.8`, the shell is still finding the old one — go back and redo the JAVA_HOME lines.

## Step 1.3 — Get the code

```bash
cd ~/Desktop
git clone git@github.com:rexsaurus/hatchling.git
cd hatchling
```

If `git` isn't installed, the Mac will offer to install it. Say yes.

## Step 1.4 — First build

```bash
./gradlew build
```

**This will take several minutes and download a few hundred megabytes.** It's downloading Minecraft itself, plus the modding toolkit. It only does this once.

You're looking for:

```
BUILD SUCCESSFUL
```

If you get an error instead, jump to **Trouble** at the bottom of this guide.

---

# Part 2 — Meet the parasite

*Both of you. This is the fun one.*

## Step 2.1 — Launch it

```bash
./gradlew runClient
```

Minecraft opens. It'll look normal — the mod is loaded, you just can't see it yet.

## Step 2.2 — Make a test world

Click **Singleplayer** → **Create New World**.

Set these:

- **Game Mode: Creative** (so you can fly and can't die)
- **Difficulty: Peaceful** (so random zombies don't confuse things)
- **World Type: Superflat** — this button is on the **World** tab, not the More tab. Click it until it says Superflat.

Click **Create New World**.

> **Why superflat?** No hills, no trees, no caves. You can see everything that happens. When you're testing a monster, you want a stage, not a jungle.

## Step 2.3 — Summon a cow

Press **T** to open the chat box. Type this and press Enter:

```
/summon minecraft:cow ~5 ~ ~
```

A cow appears five blocks in front of you. The `~` means "near me."

## Step 2.4 — Get some eggs

```
/give @s hatchling:parasite_egg 16
```

Sixteen parasite eggs go into your inventory.

## Step 2.5 — Throw one

Point at the ground near the cow. **Right-click.**

Watch what happens:

1. The egg flies in an arc
2. It cracks when it lands
3. Something small crawls out
4. It runs at the cow
5. It climbs on

**Now wait and watch the cow.** Don't do anything. After about fifteen seconds it starts staggering. After thirty, it explodes.

**Stay back for that part.**

## Step 2.6 — Things to try

Once you've seen it once, mess with it:

- **Right-click a block face instead of the ground.** The egg *places* like a block instead of throwing. Break it and you get it back.
- **Summon a pig** (`/summon minecraft:pig ~5 ~ ~`) and throw an egg at it. The parasite ignores it. Only cows work — you'll change that later.
- **Kill the parasite while it's riding the cow.** Hit it with your fist. The cow lives! You saved it.
- **Summon a second cow near the new monster** and wait. It'll throw an egg at it.
- **Summon the big one directly:** `/summon hatchling:alien ~10 ~ ~`

### The whole lifecycle

```
   EGG  ──throw or place──►  cracks open
    ▲                             │
    │                             ▼
    │                        LARVA  ──finds a cow──►  climbs on
    │                                                      │
    │                                              30 seconds
    │                                                      │
    │                                                      ▼
    └──────  throws eggs  ◄──  ALIEN  ◄──────────────  BOOM
```

Every arrow in that diagram is a rule someone wrote. You're about to change some.

---

# Part 3 — Change the rules

*This is the first real "I made the game do something" moment. Nothing here can break anything permanently.*

## Step 3.1 — Find the config file

Quit Minecraft. In your project folder, open:

```
run/config/hatchling.json
```

Open it in any text editor. It's a long list of settings that look like this:

```json
"incubationTicks": 600,
"larvaHostSearchRadius": 24.0,
"eggThrowVelocity": 1.5,
```

**Ticks are Minecraft's unit of time. 20 ticks = 1 second.** So `600` means 30 seconds.

## Step 3.2 — Your first change

Find `incubationTicks` and change `600` to `100`.

Save the file. Launch the game again (`./gradlew runClient`), make a world, throw an egg at a cow.

**Five seconds.** You just made the parasite four times faster.

## Step 3.3 — Change it while the game is running

You don't have to restart every time. Edit the file, save it, then in Minecraft press T and type:

```
/hatchling reload
```

Changes apply immediately.

## Step 3.4 — Good things to try changing

| Setting | Try this | What happens |
|---|---|---|
| `incubationTicks` | `100` | The cow bursts in 5 seconds |
| `alienHealth` | `100.0` | Very hard to kill |
| `alienSpeed` | `0.6` | Terrifyingly fast |
| `larvaHostSearchRadius` | `64.0` | Parasites find cows from far away |
| `eggThrowVelocity` | `3.0` | Eggs fly like arrows |
| `hostWhitelist` | `["minecraft:cow", "minecraft:pig", "minecraft:sheep"]` | Now pigs and sheep can be infected too |
| `maxAliensInRadius` | `2` | Keeps the population under control |
| `reproductionEnabled` | `false` | Panic button — stops the spread |

> **If it stops working:** you probably broke the JSON. Every line inside a `{ }` block needs a comma at the end *except the last one*. Paste the file into [jsonlint.com](https://jsonlint.com/) and it'll point at the mistake.

## Step 3.5 — Break it on purpose

Set `incubationTicks` to `20` and `maxAliensInRadius` to `100`. Summon twenty cows. Throw one egg.

Watch it get out of hand.

Then find the setting that stops it. (It's `reproductionEnabled`.)

**That's what those population limits are for.** Somebody had to think about what happens when a thing that makes copies of itself doesn't have a limit.

---

# Part 4 — Draw the monsters

*Paper and pencil. No computers.*

Right now the parasite looks like a silverfish and the monster looks like an Enderman. They're placeholders. Time to fix that.

## The rules

**Three drawings:** the egg, the larva, and the alien.

**Six colors. No others.** This is the palette:

| Color | Code | Where to use it |
|---|---|---|
| Bile green | `#7ea832` | Main body |
| Deep rot | `#3f5418` | Shadows, cracks |
| Membrane pink | `#c2708a` | Wet parts, insides |
| Wet highlight | `#d9e8a8` | Shiny spots |
| Void black | `#14180d` | Outlines, eyes |
| Egg glow | `#a8ff5c` | The glow only |

Six colors sounds limiting. It isn't — it's why every Minecraft mob looks like it belongs in the same world. Pick six and everything you make matches.

## What makes a monster look wrong

Three tricks that work every time:

1. **Asymmetry.** One side bigger than the other. One eye higher. Living things are symmetrical; broken things aren't.
2. **Too many or too few.** Three legs. Seven eyes. One arm.
3. **Wet.** A few bright highlights make something look slimy instead of dry. That's the difference between a rock and a thing that's alive.

## Questions to answer while drawing

- The alien was inside a cow thirty seconds ago. Does any part of it still look like a cow?
- Does the larva have eyes? Does it need them, if it just finds warm things?
- Is the egg soft or hard? Can you see something moving inside it?

Take photos of the drawings when you're done. You'll need them next.

---

# Part 5 — Put your art in the game

*Grown-up helps here. This is the longest part.*

Two paths. **Do Path A first** — it gets your colors on screen in about twenty minutes and it's the same skill, just easier.

## Path A — Repaint the existing shapes

The larva already uses the silverfish shape and the alien uses the Enderman shape. You're going to repaint them without changing the shapes.

**Get the original textures:**

```bash
mkdir -p ~/Desktop/vanilla-textures && cd ~/Desktop/vanilla-textures
find ~/.gradle -name "minecraft-client.jar" 2>/dev/null | head -1
```

Copy the path it prints, then:

```bash
unzip -o <PASTE_PATH_HERE> \
  "assets/minecraft/textures/entity/silverfish.png" \
  "assets/minecraft/textures/entity/enderman/enderman.png"
```

You now have two PNG files. **They look like flat crumpled paper, not creatures.** That's normal — they're the monster's skin, cut open and laid flat.

**Repaint them:**

Open them in a pixel editor:
- [Piskel](https://www.piskelapp.com/) — free, works in a browser, good enough
- [Aseprite](https://www.aseprite.org/) — $20, much better if you get into this
- Any image editor works, as long as it doesn't blur when you zoom in

Recolor to your six colors. Don't move anything — just change colors. Add cracks and highlights.

**Put them in the game:**

Save over these files in the project:

```
src/main/resources/assets/hatchling/textures/entity/parasite.png
src/main/resources/assets/hatchling/textures/entity/alien.png
```

Run `./gradlew runClient` and summon them. **Your colors, in the game.**

## Path B — Build the real shapes

Now the actual monster.

**Get Blockbench.** Free, from [blockbench.net](https://www.blockbench.net/).

**The key thing to know:** Blockbench has a **Paint mode** where you paint directly onto the 3D model, and it handles the flat-net part for you. You never have to think about the unwrapping. This is why Blockbench exists.

**Rough steps:**

1. **File → New → Minecraft Entity**
2. Set texture size to **64 × 64**
3. Add cubes with the **+** button. Drag to position and resize. Build the body out of boxes — that's all a Minecraft mob is.
4. Keep your kid's drawing next to you and try to match it
5. Switch to **Paint** mode (top toolbar) and paint on the model itself
6. **File → Export → Export Java Entity** for the model, and export the texture as PNG

**Honest warning:** getting a custom Blockbench model wired into the mod's code takes a real chunk of work — more than repainting does. If you're doing this in one afternoon, do Path A and save Path B for another day. The texture is 80% of what people notice anyway.

**Start with the egg block.** It's a 16×16 square — 256 pixels total. It's finite, it's fast, and finishing something feels good. Save it here:

```
src/main/resources/assets/hatchling/textures/block/parasite_egg.png
```

---

# Part 6 — Now invent something

The mod is a starting point, not a finished thing. Some directions:

**Easy (config only, no code):**
- Make the parasite infect every animal
- Make aliens so rare that finding one is an event
- Make eggs that hatch instantly

**Medium (a little code):**
- A cure — feeding the infected cow a golden apple kills the parasite
- Eggs that glow brighter as they get closer to hatching
- A different monster depending on what animal it burst out of

**Hard (real projects):**
- The alien digs underground and builds a nest
- Infected animals infect other animals by touching them
- A boss that appears if too many aliens exist at once

**The best ideas come from playing.** If something feels wrong while you're testing — the monster's too slow, the egg's too easy to dodge, you can't tell when the cow is about to burst — that feeling *is* the next thing to build. Write it down when you notice it.

---

# Trouble

| What you see | What's wrong | Fix |
|---|---|---|
| `Unable to locate a Java Runtime` | No Java installed | Part 1, Step 1.1 |
| `You are using an outdated version of Java (8)` | Found the old one | Redo the JAVA_HOME lines, then run `./gradlew --stop` |
| `Connection refused: localhost:25565` | You clicked Multiplayer | Click **Singleplayer** instead |
| Commands don't work, red text about permissions | Not in Creative | Esc → Open to LAN → Allow Cheats **ON** → Start LAN World |
| `Unknown or incomplete command` | Typo | Check spelling — it's `hatchling:parasite_egg` |
| Parasite ignores the animal | Not on the whitelist | `hostWhitelist` in the config |
| Parasite floats above the cow | Render offset | See SPEC.md |
| Game won't start after editing config | Broken JSON | Paste into [jsonlint.com](https://jsonlint.com/) |
| Everything is broken | | Delete the `run/` folder and start fresh — it only holds test worlds |
| Textures show as black and pink squares | Wrong filename or folder | Check spelling and path exactly |

**Reset the whole thing:**

```bash
rm -rf run/
./gradlew --stop
./gradlew build
```

---

# For grown-ups

## Where this actually gets hard

The install is the hard part, and it's front-loaded — which is backwards for keeping a kid engaged. Do Part 1 alone, before you sit down together. Start the shared session at Part 2, where a cow explodes in the first five minutes.

## What's worth pointing out

Try to catch these when they come up naturally, not as a lecture:

- **`600` means 30 seconds because there are 20 ticks in a second.** Units, conversion, and why computers count in weird ways.
- **Population caps exist because things that copy themselves grow exponentially.** They'll see it happen in Part 3.5. Twenty cows, one egg, ten minutes. That's a better explanation of exponential growth than any worksheet.
- **The config file separates *rules* from *tuning*.** The code says what a parasite does; the config says how fast. Professional software is built exactly this way.
- **The texture looks like nothing until it's wrapped around the model.** That's a real 3D graphics concept and it's genuinely surprising the first time.

## The mistake to avoid

Don't fix their bugs.

When the parasite doesn't do what they expected, the useful question is *"what did you think would happen, and what actually happened?"* — not *"here's what's wrong."* The gap between those two things is the entire skill. Fixing it for them removes the only part that matters.

Also: let the monster be theirs. If they want it to have nine eyes and be bright pink, it has nine eyes and it's bright pink. Ownership is what brings them back to it next weekend.

---

*Hatchling · Minecraft 1.21.1 · Fabric · MIT License*

*NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.*
