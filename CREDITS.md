# Credits / asset provenance

All Hatchling art in this repository is original work unless noted below.
Do not copy textures or models out of the Minecraft jar into this repo
(Mojang EULA forbids redistributing those assets; a recolor is still a
derivative).

## Original Hatchling assets

| Asset | Author | Notes |
| --- | --- | --- |
| `assets/hatchling/textures/block/parasite_egg.png` | Hatchling (original) | 16×16 cluster shell texture; Hatchling palette only |
| `assets/hatchling/textures/item/parasite_egg.png` | Hatchling (original) | 16×16 single-egg item icon; Hatchling palette only |
| `assets/hatchling/models/block/parasite_egg_1.json` | Hatchling (original) | Hand-authored elements; turtle-egg-like geometry, not a Mojang file |
| `assets/hatchling/models/block/parasite_egg_2.json` | Hatchling (original) | Same |
| `assets/hatchling/models/block/parasite_egg_3.json` | Hatchling (original) | Same |

## Stand-in / temporary visuals

Phase-1 entity renderers reuse vanilla **model classes** (silverfish /
enderman geometry) with Hatchling textures under
`assets/hatchling/textures/entity/`. Those textures are project-owned
placeholders; they are not redistributed Mojang PNGs.

## Sounds

Current `ModSounds` map to vanilla sound events at runtime (no `.ogg`
files shipped yet). Custom audio, when added, should be listed here with
source and license.
