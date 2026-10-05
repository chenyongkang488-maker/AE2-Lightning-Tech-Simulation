# Lightning Tech: Simulation

Author: qiqi

**Summary:** Grow simulation crystals with lightning and produce resources through AE2-powered chambers.

Lightning Tech: Simulation is an independent addon for Minecraft 1.21.1
and AE2 Lightning Tech / Reborn. Bind crystals to minerals, crops, trees or mobs,
cultivate them with lightning, and reuse perfect crystals as production templates.

## Features

- Single-block chambers with up to 128 parallel operations, acceleration cards and automatic output.
- Pink/white multiblock chambers from 3×3×3 to 7×7×7, holding up to 49 crystals.
- Efficiency, fortune, overload and smelting upgrades; large output buffers and ME export.
- A modular Overload Thunder Coil for artificial/natural lightning, mining and wrench use.
- Generic mineral tags, farmland crop matching and spawn-egg-backed mob support.
- Datapack recipes and documented Java hooks for modpack and addon authors.
- In-game G-key guides and native Lightning Tech equipment configuration.

## Requirements

Minecraft 1.21.1, Java 21 and NeoForge 21.1.252 or later within 21.1.x.
Required mods: **AE2 19.2.17**, **GuideME 21.1.19**,
[AE2 Lightning Tech 2.1.1](https://www.curseforge.com/minecraft/mc-mods/ae2-lightning-tech/files/9024605),
and [Thunderbolt Core 2.0.2](https://www.curseforge.com/minecraft/mc-mods/thunderbolt-core/files/9055479).
The original Reborn 2.1.0 / 2.0.0 pair is also supported. Reborn migrated to the main
projects and retains the same mod IDs: install only one copy of each dependency.
Pre-Reborn Lightning Tech 2.0.x is unsupported. AE2 and GuideME alone are insufficient.
Mekanism support is optional, tested on 10.7.19.85.
Mystical Agriculture 8.0.28 / Cucumber 8.0.16 were used for optional crop tests.

Install the ordinary mod JAR on both client and server. Back up worlds before
upgrading and remove older addon JARs. Development source archives do not go in mods/.

## Beta status and compatibility

Version 0.1.1-beta.2 fixes compatibility with current dependencies and the coil
device hub while retaining beta.1 gameplay. Verified locally: 24 unit tests,
153 GameTests per three runtime profiles, and old/current client device-hub checks.
Generic matching is extensible
but does not promise support for every mod: entity equipment/NBT, special crop
environments and block entities are not cloned. Long-term and multiplayer testing
is still needed. Please report exact versions, reproduction steps and a log.

## License and credits

Code, documentation and original artwork: **MIT**, by qiqi and contributors.
Adapted crystal sprites, the crystal logo and seven module icons:
**CC BY-NC-SA 3.0**, based on AE2 Lightning Tech Reborn contributor artwork.
Crystal palettes and module symbols were changed; see LICENSES.md and
THIRD_PARTY_NOTICES.md in the source repository and JAR for full attribution.

Upstream: https://github.com/AE2-Lightning-Tech-Reborn/AE2-Lightning-Tech-Reborn

Source and issues: https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation
