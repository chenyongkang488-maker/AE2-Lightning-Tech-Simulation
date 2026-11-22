---
navigation:
  title: Multiblock Simulation Chamber
  icon: overload_sim:simulation_controller
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:simulation_controller
  - overload_sim:simulation_frame
  - overload_sim:simulation_efficiency_t1
  - overload_sim:simulation_efficiency_t2
  - overload_sim:simulation_efficiency_t3
  - overload_sim:simulation_fortune_module
  - overload_sim:simulation_overload_module
  - overload_sim:simulation_smelting_module
  - overload_sim:simulation_primordial_module
---

# Multiblock Simulation Chamber

With Mekanism installed, bind fluorite using 24 fluorite storage blocks. Bind sulfur with 24 blocks in the common sulfur ore tag. Stone and deepslate variants can be mixed. Regular simulation yields 2–4 fluorite gems or one sulfur dust per crystal before upgrades. The Primordial Module prefers the ordinary stone ore. Sulfur ore can come from AllTheOres or another mod providing the common tag.

Build an enclosure with equal external width, height and depth from 3 to 7 blocks. The controller stores reusable Perfect Simulation Crystals.

|External size|Crystal capacity|Floor upgrade positions|
|---|---|---|
|3×3×3|9|1|
|4×4×4|16|4|
|5×5×5|25|9|
|6×6×6|36|16|
|7×7×7|49|25|

Use simulation frames for the twelve edges and the entire roof. Place exactly one controller in a non-corner position of the bottom edge, facing outward. One other bottom-edge position may contain an Overloaded ME Interface. Fill the four side interiors with AE vibrant quartz glass. Fill the floor interior with frames or upgrade modules, and leave the chamber interior empty.

The controller detects the completed structure. Right-click it, insert perfect crystals, and connect an online ME network with HV storage. Hover the progress bar for status and batch cost.

## Production and upgrades

Each participating crystal produces one simulation output roll per batch. The default cycle is 180 ticks, with 1000 FE and 1 HV charged per crystal at completion. Changing any crystal slot cancels the whole unfinished batch without production charges; Shift-click also works during processing.

The controller accepts external FE and draws up to 10,000 FE/tick from online Applied Flux FE cells. Empty FE cells or absent Applied Flux stop network FE charging. Normal AE power is still required for the network.

|Floor module|Effect|
|---|---|
|Efficiency T1 / T2 / T3|Subtracts 2 / 4 / 8 ticks per module, with a total reduction cap of 104 ticks|
|Fortune|Doubles the rolled output per module, up to ×1024 with ten modules|
|Overload, at most one|Halves the final duration and adds 1 EHV per crystal|
|Smelting, at most one|Adds 2 HV per crystal; converts raw iron, copper and gold to two ingots, and Ancient Debris to two Netherite Scraps; extra ores use tags or explicit mappings|

A 7×7×7 floor can hold thirteen T3 modules, ten Fortune modules, one Overload and one Smelting module. This gives a 38-tick cycle. With 49 iron crystals it produces 100,352 iron ingots per batch, costing 49,000 FE, 147 HV and 49 EHV.

## Primordial Module

Install one Primordial Module in a floor upgrade position to produce natural ore blocks from mineral crystals. At most one is allowed, and it cannot coexist with Smelting. Invalid combinations prevent formation; automatic construction reports the conflict and preserves the blocks. Efficiency, Fortune and Overload may coexist. This floor module is for the multiblock chamber.

Each converted mineral crystal produces **one ore block per cycle before the chamber Fortune multiplier**. Redstone and lapis normal loot quantities do not become additional ore blocks. A successful conversion adds **2 HV per crystal**, independent of Fortune; FE and duration are unchanged. Mob, crop and tree outputs incur no surcharge. Mining the produced ore can also benefit from tool Fortune; this combination is intentional.

Iron, copper, gold, coal, diamond, emerald, redstone and lapis default to ordinary stone ores. Quartz produces Nether Quartz Ore; netherite produces Ancient Debris. Gold crystals do not record a source dimension, so they default to overworld Gold Ore. Modded minerals use explicit datapack mappings first, otherwise require exactly one valid ore in the material tag. Missing, ambiguous or invalid targets retain normal production without the extra HV. Hover the progress bar for mode, actual batch fees and fallback details.

Started batches retain fixed outputs and costs across module changes and restarts. An invalid structure pauses processing; re-forming resumes the saved batch. Module changes apply to the next batch. Unstarted batches are replanned when the configuration changes.

Recipe: one Simulation Chamber Frame and one Silk Touch enchanted book in the Lightning Assembly Chamber, consuming 100,000 FE and 32 EHV to make one module. The book is consumed. Renamed books and additional enchantments are accepted; ordinary books and enchanted tools are not substitutes.

A `multiblock_simulation` config may specify `"primordial": [{"profile":"overload_sim:iron","block":"minecraft:deepslate_iron_ore"}]`. Set `policy.primordial_hv` to adjust the surcharge; `/reload` applies changes to new batches.

## Output and stored tasks

The output buffer has 128 slots on four pages. Each slot holds up to 1024 identical items with matching components. Left-click extracts a normal stack, right-click one item, and Shift-click fills player inventory in normal stacks. Pipes also extract normal stacks and cannot remove template crystals.

An installed Overloaded ME Interface connects the structure through its bound frequency and enables automatic product export. Products rejected by the network stay in the buffer. Controller cabling or wireless binding supplies network access but alone does not enable this interface export; pipes and manual output remain available.

Power loss, unloaded structure chunks or an invalid shell pause a started task. Restoring the structure continues with its saved loot roll. Changing a template restarts the batch. Removing the controller in survival saves inventory, FE and unfinished tasks into the controller item; placing it again restores them. If the structure is smaller, use the recovery view to remove crystals beyond its capacity. A missing output item's mod pauses the saved task until that item becomes available.

## Automatic construction

Place and orient the controller, select size 3–7 in its left toolbar and click Automatic build. The shell extends behind it. The controller is positioned at floor-front coordinate `floor((size-1)/2)`, counted from the left corner.

Construction preserves legal modules, frame/glass aliases and one legal interface. It uses player inventory first, then the controller's active AE network; offline inventory construction and creative construction work. Missing materials, obstacles, unloaded positions or ownership conflicts prevent construction. Cancelled placement refunds materials. Repeating a complete build uses no materials. A formed chamber cannot be resized with this button.

## Data packs and Java API

Override `data/overload_sim/multiblock_simulation/default.json` to configure the machine policy:

```json
{
  "policy": {
    "ticks": 180, "reduction_cap": 104, "fortune_cap": 10,
    "t1": 2, "t2": 4, "t3": 8,
    "fe": 1000, "hv": 1, "overload_ehv": 1, "smelting_hv": 2
  },
  "smelting": [
    {"input": "example:raw_metal", "result": "example:metal_ingot", "count": 2}
  ],
  "modules": [
    {"block": "example:fast_floor", "kind": "T3"}
  ]
}
```

Use one policy override. Module kinds are `FRAME`, `T1`, `T2`, `T3`, `FORTUNE`, `OVERLOAD` and `SMELTING`. Frame and glass aliases use block tags `overload_sim:simulation_frames` and `simulation_glass`. Reload with `/reload`.

The chamber shares simulation profiles and output recipes with the single-block machine, but uses the multiblock policy for duration and costs. It ignores the single-block recipe's `production.ticks/fe/lightning/input` fields. Fortune multiplies one rolled result per crystal.

Java providers use `SimulationExtensions.registerOutputV2`; inspect `context.machine()` for `multiblock`. `MultiblockSimulationEvents.BeforeBatchStart` and `BeforeBatchCommit` can cancel a batch; `Completed` reports output after payment. Read buffer quantities with `BulkOutputBuffer.count(slot)`. Do not charge costs or insert Completed output a second time. Full reference: `docs/multiblock-api.md`.

## Layer plans

F=simulation frame, C=controller facing north, G=AE vibrant quartz glass, U=frame or floor module, .=air. One non-corner bottom-edge F may be replaced by an interface.

### 3×3×3 (9 crystals, 1 upgrade positions)

Floor y=0

```
FCF
FUF
FFF
```

Middle layers y=1–1

```
FGF
G.G
FGF
```

Roof y=2

```
FFF
FFF
FFF
```

### 4×4×4 (16 crystals, 4 upgrade positions)

Floor y=0

```
FCFF
FUUF
FUUF
FFFF
```

Middle layers y=1–2

```
FGGF
G..G
G..G
FGGF
```

Roof y=3

```
FFFF
FFFF
FFFF
FFFF
```

### 5×5×5 (25 crystals, 9 upgrade positions)

Floor y=0

```
FFCFF
FUUUF
FUUUF
FUUUF
FFFFF
```

Middle layers y=1–3

```
FGGGF
G...G
G...G
G...G
FGGGF
```

Roof y=4

```
FFFFF
FFFFF
FFFFF
FFFFF
FFFFF
```

### 6×6×6 (36 crystals, 16 upgrade positions)

Floor y=0

```
FFCFFF
FUUUUF
FUUUUF
FUUUUF
FUUUUF
FFFFFF
```

Middle layers y=1–4

```
FGGGGF
G....G
G....G
G....G
G....G
FGGGGF
```

Roof y=5

```
FFFFFF
FFFFFF
FFFFFF
FFFFFF
FFFFFF
FFFFFF
```

### 7×7×7 (49 crystals, 25 upgrade positions)

Floor y=0

```
FFFCFFF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FUUUUUF
FFFFFFF
```

Middle layers y=1–5

```
FGGGGGF
G.....G
G.....G
G.....G
G.....G
G.....G
FGGGGGF
```

Roof y=6

```
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
FFFFFFF
```
