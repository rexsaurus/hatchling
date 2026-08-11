# Hatchling — Four Sessions

*A plan for building a Minecraft monster with a nine-year-old. Four sessions, about 25 minutes each.*

> NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.

---

## How this works

**This guide is for you, not him.** Read it out loud, hand him the keyboard for the parts marked **HIM**, and do the file-shuffling yourself.

Each session has a **Prep** block. Do that alone, before he sits down. If prep leaks into the session, the session dies — a nine-year-old will not wait out a Gradle build.

| | Session | What he does | Ends with |
|---|---|---|---|
| **1** | **The Monster** | Plays. Builds nothing. | A cow explodes |
| **2** | **The Dials** | Changes numbers, watches the game change | A monster he tuned |
| **3** | **The Face** | Repaints the creature in Blockbench | His colors, in the game |
| **4** | **The Wish** | Asks Cursor to add something new | A rule he invented |

Don't skip ahead. The order is the whole design.

For a longer from-zero install + afternoon workshop, see [BUILD_IT_TOGETHER.md](BUILD_IT_TOGETHER.md).

---

## Ground rules

**One win every five minutes.** If five minutes pass with nothing visible happening, you've drifted into your project instead of his. Pull back.

**He types the fun things, you type the boring things.** Typing is slow at nine. `/summon minecraft:cow ~5 ~ ~` is worth him typing. A file path is not.

**"What did you think would happen?"** When something doesn't work, ask that before explaining. The gap between expected and actual is the entire skill. Fixing it for him removes the only part that matters.

**Stop while he still wants more.** Ending at 20 minutes with him asking for another go is a win. Ending at 45 with him bored is not.

---

# Session 1 — The Monster

**Goal:** he sees the whole lifecycle and wants to know how it works.
**He builds nothing.** This is on purpose.

### Prep (you, before he sits down)

```bash
cd ~/Desktop/hatchling
./gradlew build
./gradlew runClient
```

Leave Minecraft **open on the title screen**. The build downloads hundreds of megabytes the first time — that cannot happen while he's watching.

Have a sticky note ready with these three lines. He'll type them.

```
/summon minecraft:cow ~5 ~ ~
/give @s hatchling:hatchling_egg 16
/summon hatchling:alien ~10 ~ ~
```

---

### Run it

**1. Make a world** *(3 min)* — **HIM**

Singleplayer → Create New World.

- **Game Mode: Creative**
- **Difficulty: Peaceful**
- **World Type: Superflat** — this button is on the **World** tab, not More

> "Superflat means no hills and no trees. When you're testing a monster you want a stage, not a jungle."

**2. Summon a cow** *(2 min)* — **HIM**

Press **T**, type the first line, Enter.

> "The squiggles mean *near me*. Five blocks that way."

**3. Get eggs** *(1 min)* — **HIM**

Press T, type the second line.

**4. Throw one** *(1 min)* — **HIM**

Point at the ground near the cow. **Right-click.**

**Then stop talking.** Let him watch. The bug crawls out, finds the cow, climbs on.

**5. Wait** *(1 min)*

Don't narrate. Around 15 seconds the cow starts staggering. At 30 it explodes.

✅ **If the cow explodes and something angry appears — Session 1 has already worked.** Everything after this is bonus.

---

### Now let him mess with it *(10–15 min)* — **HIM**

Hand him the sticky note and get out of the way. Prompts if he stalls:

- **"Try right-clicking the side of a block instead of the ground."** The egg *places* instead of throwing.
- **"Can you save the cow?"** Punch the bug off its back mid-infection. The cow lives.
- **"What if there are two cows?"**
- **"Summon the big one directly."** Third line on the note.
- **"What happens if you throw an egg at a pig?"** Nothing — only cows work. He'll ask why. Tell him he can change that next time.

### The one thing to say at the end

> "Everything you just saw is a rule somebody wrote down. Next time we change some."

**Stop here.** Do not open a code editor.

---

# Session 2 — The Dials

**Goal:** he changes a number and the game obeys.

### Prep

Open `run/config/hatchling.json` in a text editor and leave it on screen next to Minecraft. Launch `./gradlew runClient` and get to a superflat creative world with a cow already summoned.

---

### Run it

**1. Show him the file** *(3 min)*

> "This is the list of every rule. You can change any of them."

Point at:

```json
"incubationTicks": 600,
```

> "Minecraft counts in ticks. Twenty ticks is one second. So how long is 600?"

Let him do the math. Thirty seconds.

**2. First change** *(3 min)* — **HIM**

Change `600` to `100`. Save.

In Minecraft, press T:

```
/hatchling reload
```

Throw an egg at the cow. **Five seconds.** He just made it six times faster.

✅ **This is the moment.** Everything else in this session is repetition of this feeling.

**3. Let him loose** *(15 min)* — **HIM**

Show him the pattern once — *change number, save, `/hatchling reload`, test* — then hand it over.

| Setting | Try | What happens |
|---|---|---|
| `alienSpeed` | `0.6` | Terrifyingly fast |
| `alienHealth` | `100.0` | Nearly unkillable |
| `alienLifespanTicks` | `1200` | Aliens age-die after ~1 minute (worth tuning) |
| `eggThrowVelocity` | `3.0` | Eggs fly like arrows |
| `larvaHostSearchRadius` | `64.0` | Bugs find hosts from way off |
| `alienEggThrowChance` | `0.95` | Alien almost never stops throwing eggs |

(Cow/pig/sheep/chicken are already on the default whitelist — no need to
edit that unless he wants something weirder, like horses.)

**4. Break it on purpose** *(5 min)* — **BOTH**

```json
"incubationTicks": 40,
"maxAliensInRadius": 100,
```

Summon twenty cows:

```
/summon minecraft:cow ~5 ~ ~
```

(He can just spam it, or you can show him `/summon` in a loop is a thing grown-ups do.)

Throw **one** egg. Watch it get completely out of hand.

Then: **"Find the setting that stops it."** It's `reproductionEnabled: false`.

> "That's why those limits are in there. Somebody had to think about what happens when a thing that makes copies of itself doesn't have a limit."

### If it stops working

He broke the JSON. Every line inside a `{ }` needs a comma except the last one. Paste into [jsonlint.com](https://jsonlint.com/) — it points at the exact line. **Let him fix it.** This is a good thirty seconds of frustration, not a bad one.

---

# Session 3 — The Face

**Goal:** his colors on the monster.
**He repaints. He does not model.** Modeling from scratch is a different weekend.

### Prep

1. Install [Blockbench](https://www.blockbench.net/) — free, and it takes two minutes.
2. Open `art/hatchling_larva.bbmodel` in Blockbench and leave it open.
   (Alien stand-in is `art/hatchling_alien.bbmodel` — same paint flow, texture `alien.png`.)
3. **Speed trick:** the game reads textures from `build/resources/main/`, not `src/`. Have that folder open in Finder. Saving straight into it plus **F3+T** in game reloads in about two seconds instead of a 90-second relaunch. Copy back to `src/main/resources/` afterward so the change actually sticks.
4. Print or write out the palette. He needs it in front of him.

| Color | Code |
|---|---|
| Bile green | `#7ea832` |
| Deep rot | `#3f5418` |
| Membrane pink | `#c2708a` |
| Wet highlight | `#d9e8a8` |
| Void black | `#14180d` |
| Egg glow | `#a8ff5c` |

---

### Run it

**1. Spin it around** *(3 min)* — **HIM**

Drag to rotate. Scroll to zoom.

> "Somebody in the world made this and said other people could use it. That's why we can. Their name goes in our credits file."

**2. Show him the flat version** *(2 min)*

Click the texture in the Textures panel. It looks like crumpled paper.

> "That's its skin, cut open and laid flat. Painting on that directly is horrible. So we're not going to."

**3. Paint mode** *(12 min)* — **HIM**

Click **Paint** in the top toolbar. Now he paints **on the 3D model** and Blockbench works out where the flat version needs changing.

**The rules:**
- Six colors, nothing else
- Try to make it look *wet* — a few bright `#d9e8a8` dots do it
- Make it uneven. One side different from the other. Living things are symmetrical; broken things aren't.

Let him work. Don't art-direct.

**4. Export** *(3 min)* — **YOU**

Textures panel → right-click the texture → **Save As** → `hatchling.png` into `build/resources/main/assets/hatchling/textures/entity/`.

**5. See it** *(5 min)* — **HIM**

In Minecraft: **F3+T**, then:

```
/summon hatchling:hatchling ~5 ~ ~
```

✅ **His monster. In the game.**

Copy the file to `src/main/resources/assets/hatchling/textures/entity/hatchling.png` afterward, or it vanishes next build.

### If it goes black and magenta

Wrong filename or wrong folder. Check spelling exactly — `hatchling.png`, all lowercase.

### If he wants to change the shape

Switch to **Edit** mode, add cubes, move things. But that needs a re-export of the model *and* a rebuild, which is a whole extra step. **Say yes and do it next time.** Ending on a working win beats starting a second project.

---

# Session 4 — The Wish

**Goal:** he asks for something that doesn't exist, and it exists.

### Prep — do not skip this

```bash
cd ~/Desktop/hatchling
git add .
git commit -m "Before session 4"
```

**This is your undo button.** If Cursor makes a mess, `git checkout .` puts everything back. Don't run this session without it.

Open Cursor with the project loaded, on the chat panel.

---

### Run it

**1. Explain what this is** *(3 min)*

> "This thing writes the rules for us. But it does exactly what you ask, not what you meant. So we have to be really specific."

**2. The wish template** *(5 min)* — **HIM**

Write this on paper. He fills in the blanks:

```
Make the ______ do ______ when ______.

Keep everything else exactly the same.
Only change this one thing.
Then tell me how to test it.
```

Real examples that work:

- *"Make the **alien** **glow in the dark** when **it's night time**."*
- *"Make the **egg** **make a squishy sound** when **a player walks near it**."*
- *"Make the **bug** **go faster** when **it can see a cow**."*
- *"Make the **alien** **drop a special item** when **you kill it**."*

**The rules for him:**
1. One wish at a time
2. Test it before the next wish
3. If it breaks, say **"undo that"**

**3. Watch it work** *(5 min)* — **BOTH**

Read the changed lines with him. Not all of it — just enough to see that words became instructions.

> "See that number? That's the one you'd change to make it glow brighter."

**4. Test it** *(10 min)* — **HIM**

```bash
./gradlew runClient
```

Does it do what he asked? Usually *almost*. The gap is the interesting part:

> "It did what you said. Did you say what you meant?"

Then refine and go again.

**5. Save his work** *(2 min)* — **YOU**

```bash
git add .
git commit -m "Sam's glowing alien"
```

Use his actual wish as the message. He'll like seeing his name in the history.

### When it goes wrong

It will. Cursor will break the build or produce something odd. That's fine — that's the session.

```bash
git checkout .
```

> "Nothing's ruined. We can always go back to the last time it worked. That's why we saved."

---

# What comes next

Once all four sessions are done, he has the whole loop: **play it → tune it → paint it → change it.** That's the entire craft in miniature.

Good directions from here:

- **His own monster from scratch** in Blockbench (a full session, maybe two)
- **A cure** — feeding the infected cow a golden apple kills the parasite
- **A different monster** depending on which animal it burst out of
- **Show a friend** — this is when the dedicated server finally earns its setup time

**The best ideas come from playing.** When something feels wrong while testing — the monster's too slow, you can't tell when the cow's about to burst — that feeling *is* the next thing to build. Keep a list on paper. Let him add to it.

---

# Quick reference

**Copy-paste card for him:**

```
/summon minecraft:cow ~5 ~ ~
/summon hatchling:hatchling ~5 ~ ~
/summon hatchling:alien ~10 ~ ~
/give @s hatchling:hatchling_egg 16
/hatchling reload
```

**Copy-paste card for you:**

```bash
cd ~/Desktop/hatchling
./gradlew runClient      # play
./gradlew build          # after code changes
git add . && git commit -m "working"   # before letting Cursor loose
git checkout .           # undo everything since last commit
rm -rf run/              # nuke test worlds and start clean
```

**Trouble:**

| What you see | Fix |
|---|---|
| Commands rejected | Esc → Open to LAN → Allow Cheats **ON** → Start LAN World |
| Connection refused | Click **Singleplayer**, not Multiplayer |
| Game won't start after config edit | Broken JSON — [jsonlint.com](https://jsonlint.com/) |
| Black and magenta creature | Texture filename or folder wrong |
| Everything's broken | `git checkout .` then `rm -rf run/` |

---

*Hatchling · Minecraft 1.21.1 · Fabric · MIT License*
