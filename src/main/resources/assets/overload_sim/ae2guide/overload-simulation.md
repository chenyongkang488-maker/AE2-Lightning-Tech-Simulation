---
navigation:
  title: "Lightning Tech: Simulation"
  icon: overload_sim:overload_simulation_chamber
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
item_ids:
  - overload_sim:blank_simulation_crystal
  - overload_sim:simulation_crystal
  - overload_sim:perfect_simulation_crystal
  - overload_sim:overload_simulation_chamber
---

# Lightning Tech: Simulation

<ItemImage id="overload_sim:blank_simulation_crystal" scale="3" />

This addon requires AE2, AE2 Lightning Tech, Thunderbolt and GuideME. Applied Flux is optional and provides network FE storage; external FE power can be used without it.

Produce one Blank Simulation Crystal in Lightning Tech's Lightning Simulation Chamber from one Electro Chime Crystal, 16 Overload Crystals and 16 Amethyst Shards, using 200,000 FE and 32 HV.

## Binding and cultivation

Place a blank crystal in a Lightning Collector. Fill the 24 positions surrounding the collector in a horizontal 5×5 area at its height with matching raw-material storage blocks. Diamond, redstone, lapis, coal, emerald, quartz and netherite use their corresponding storage blocks. A successfully collected strike consumes the blocks and records the mineral.

For crops, use 24 supported soil blocks at the collector's height with the same crop planted one block above them. Growth stages may differ. Binding consumes the plants, including the upper half of two-block plants, preserves the soil and produces no harvest drops. Trees use matching saplings on dirt-type soil. Common tags support additional minerals and crops. Ambiguous materials or differing ore loot tables need an explicit rule.

Hold a blank crystal in the offhand while struck by real lightning to attempt binding the nearest living eligible mob within a five-block spherical radius, with 33% probability. Damage is unnecessary, including in Creative mode. Players are excluded; only the entity type is recorded, without equipment or individual data.

A bound crystal becomes perfect after ten further valid strikes. Cultivate it in the collector or hold it in the offhand during lightning hits. Natural, command and artificial strikes work unless a matching rule sets `allow_artificial` to false. The binding strike does not count toward cultivation.

## Single-block simulation chamber

Connect the chamber to an online ME network with a channel and HV storage. Put a Perfect Simulation Crystal in the first slot; it is reusable. Default production costs 1000 FE and 1 HV per operation and takes 200 ticks. The netherite profile costs 16,000 FE and produces one Ancient Debris.

The FE buffer holds 2,000,000 FE. While online, it draws up to 10,000 FE/tick from Applied Flux FE cells, including while idle. Empty FE cells or absent Applied Flux stop network FE charging. AE energy cells power the ME network; external FE power also works.

Put Lightning Collapse Matrices in the second slot. With none, the chamber runs one operation at a time; each matrix provides four parallel operations, up to 32 matrices and 128 operations. The upgrade panel accepts four individual AE2 Acceleration Cards. Each halves the recipe duration, rounded up to at least one tick, without changing per-operation cost. Hand-use a card on the chamber or Shift-click it into the panel. Cards inserted during a batch apply to the next batch.

The auxiliary slot supplies materials required by custom recipes. FE, HV and auxiliary materials are paid once when the actual parallel batch starts. Each operation rolls its own loot; results are fixed and saved. A full output pauses delivery, and an offline network pauses work. Breaking the machine discards its unfinished task.

Use the automatic-ejection switch and six direction switches to send products to adjacent inventories. Standard item automation extracts outputs only. The frequency button opens the shared overload frequency interface.

## Production rules

Default mob simulation uses normal loot with a neutral player-kill context, no Looting and zero Luck. Slimes use size 1 and magma cubes size 2. The Wither produces one Nether Star. The Ender Dragon has independent 25% chances for a Dragon Egg and Dragon Head and always produces one Dragon's Breath. The Warden produces one Sculk Catalyst, Echo Shard and Sculk.

Automatic crops produce their mature normal harvest, including seeds and side products, in both simulation chambers. Source soil tier, crux and environmental bonuses are not copied. Special code-based mob drops, equipment and plants with block-entity data require explicit output rules or Java providers.

## Data packs and API

Minecraft 1.21.1 uses `data/<namespace>/recipe/<path>.json`. Override existing IDs or assign a unique higher `priority`; conflicting highest-priority matches stop processing. Apply changes with `/reload`.

|Directory or recipe type|Configuration|
|---|---|
|`simulation_profile/<id>.json`|Stable profile with `kind`, `name` and `icon`; kind is mineral, crop, tree or mob|
|`overload_sim:crystal_binding`|Profile, priority, artificial-lightning permission and `world` material/soil selectors|
|`overload_sim:mob_crystal_binding`|Entity ID, tag or `*`; radius, probability, disabled flag and optional condition|
|`overload_sim:crystal_cultivation`|`cultivation.required` and `increment`, each 1–1,000,000|
|`overload_sim:overload_simulation`|Production ticks, FE, HV, auxiliary input and outputs, entity loot or named provider|

For example, replace `data/overload_sim/recipe/production/iron.json`:

```json
{
  "type": "overload_sim:overload_simulation",
  "profile": "overload_sim:iron",
  "priority": 100,
  "production": {
    "ticks": 200, "fe": 1000, "lightning": 1,
    "outputs": [{"item": "minecraft:raw_iron", "count": 2, "chance": 1}]
  }
}
```

Additional mineral and mob mappings use `simulation_mineral/<id>.json` and `simulation_mob/<id>.json`. Crop and soil aliases use `overload_sim:simulation_crops` and `simulation_farmlands` block tags. Blacklist tags cover minerals, crops and mob types.

Common configuration in `config/overload_sim-common.toml` includes `matrixLimit` (1–32), `accelerationCardLimit` (0–4), `autoEjectIntervalTicks` (1–200) and `networkFeChargePerTick` (0–2,000,000; zero stops new network FE withdrawals).

In Java, register named conditions and output providers through `SimulationExtensions` during common setup. Use `registerOutputV2` for outputs with explicit RNG and machine context, and `registerCropMaturityResolver` for a special crop's mature state. Duplicate IDs are rejected. `CrystalDataAccess.read/bound/perfect` reads or creates crystal data.

Use `/overload_sim explain` with a crystal in the main hand, or `/overload_sim explain_block x y z` for an already loaded block. Administrators can run `/overload_sim audit` to write `overload_sim-audit.json` in the world directory. Detailed field references are in `docs/api.md`, `docs/compatibility-api.md` and `docs/crop-compatibility-api.md`. See [Multiblock Simulation Chamber](multiblock-simulation.md) for its separate production policy.
