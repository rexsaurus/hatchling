# Credits / asset provenance

All Hatchling art in this repository is original work unless noted below.
Do not copy textures or models out of the Minecraft jar into this repo
(Mojang EULA forbids redistributing those assets; a recolor is still a
derivative).

## Original Hatchling assets

| Asset | Author | Notes |
| --- | --- | --- |
| `assets/hatchling/textures/block/hatchling_egg.png` | Hatchling (original) | 16×16 cluster shell texture; Hatchling palette only |
| `assets/hatchling/textures/item/hatchling_egg.png` | Hatchling (original) | 16×16 single-egg item icon; Hatchling palette only |
| `assets/hatchling/models/block/hatchling_egg_1.json` | Hatchling (original) | Hand-authored elements; turtle-egg-like geometry, not a Mojang file |
| `assets/hatchling/models/block/hatchling_egg_2.json` | Hatchling (original) | Same |
| `assets/hatchling/models/block/hatchling_egg_3.json` | Hatchling (original) | Same |

## Entity models / textures (M9)

| Asset | Author | Notes |
| --- | --- | --- |
| `HatchlingModel` / `art/hatchling_larva.bbmodel` | **UNKNOWN** | **VERIFY BEFORE PUBLIC RELEASE** |
| `AlienModel` / `art/hatchling_alien.bbmodel` | **UNKNOWN** | **VERIFY BEFORE PUBLIC RELEASE** |
| `assets/hatchling/textures/entity/hatchling.png` | Hatchling (placeholder) | 64×64 placeholder palette texture for custom larva model |
| `assets/hatchling/textures/entity/alien.png` | Blockbench export | From export `texture.png`; 128×128 for custom alien model |

Legacy silverfish/enderman geometry (`useCustomModels=false`) still uses the
entity textures under `assets/hatchling/textures/entity/` as project-owned
placeholders — not redistributed Mojang PNGs.

## Sounds

Current `ModSounds` map to vanilla sound events at runtime (no `.ogg`
files shipped yet). Custom audio, when added, should be listed here with
source and license.
