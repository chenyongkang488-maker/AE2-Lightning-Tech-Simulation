# Laser prototype Blockbench workshop

Open `laser-prototype.bbmodel` in Blockbench. Each named group is the geometry of
one matching `assets/overload_sim/models/block/<group>.json` export. Groups are
spaced apart for editing; they are individual blocks, not a multiblock blueprint.
The existing perfect crystal item and animated energy orb are rendered in-world
by the block entity renderer. The core orb is three blocks across when active.

Run `python scripts/generate-laser-prototype.py` from the repository to reproduce
the workshop, voxel models, blockstates, item models, loot tables, tags, guide
pages and language snippets. Run `python scripts/verify-laser-resources.py` to
check their parity and references. The new pixel atlas is painted in native
Blockbench by loading art/laser_art_workshop.js and using its Tools action.
laser-pixel-atlas.bbmodel preserves its editable layer. Existing PNGs are unchanged. The scripts do not change registries,
central language files, recipes, or existing tags. Merge the two language JSON
snippets into the central language files; append `#overload_sim:laser_mineable`
to Minecraft's `mineable/pickaxe` block tag during integration.

Texture attribution and licenses remain in `LICENSES.md`, `THIRD_PARTY_NOTICES.md`
and `art/licenses/AE2LT-LICENSE_ASSETS.md`. Reusing these textures does not change
their license. White concrete is used for runtime destruction particles. Glass
uses the existing borderless pale glass texture. All controller models support six
facing directions. Formed/active properties, if added, can reuse these variants.
