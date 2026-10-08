---
navigation:
  title: Laser and particles
  icon: overload_sim:high_energy_laser_emitter
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:high_energy_laser_emitter
  - overload_sim:laser_constraint_matrix
  - overload_sim:laser_enhancement_matrix
  - overload_sim:laser_burst_matrix
  - overload_sim:laser_deflector_matrix
  - overload_sim:laser_focus_matrix
  - overload_sim:collapse_core
  - overload_sim:particle_constrainer
  - overload_sim:laser_frame
  - overload_sim:laser_glass
  - overload_sim:laser_source_crystal
  - overload_sim:laser_optical_port
---

# Laser and particles

Build the chain in this order: emitter → matrices → focus matrix → collapse core → particle constrainer. Particles leave the far side of the core in the laser's travel direction. Place a constrainer optical port facing the core, on that side and within seven blocks.

Laser strength and the costs below are measured per 20-tick batch. G opens this guide; right-click a controller for settings and exact resource values. The collapse core is passive: it does not accept AE cabling or frequency binding. Other controllers connect through AE cables, an installed Overloaded ME Interface or the shared overload frequency system.

## Construction

Ordinary straight devices send light opposite the controller's facing direction and support all six directions. Leave at least one empty block between neighboring structure surfaces.

The emitter fits within 5×5×5. Its controller is at the rear center; the source crystal is two blocks forward and the output port another two blocks forward. Use the automatic-build projection for the frame positions.

Constraint, enhancement, burst and focus matrices use the same 7×7×1 ring. F=laser frame, C=matrix controller, .=air. Seen from the controller side:

```
..FFF..
.F...F.
F.....F
F.....F
F.....F
.F...F.
..FCF..
```

The optical center is three blocks along construction-up from the controller. Construction-up is world-up for horizontal light and north for vertical light.

The deflector is a world-upright 7×7×7 structure. Its center is three blocks above the bottom controller. Relative to the center, fill shell cells satisfying 7 ≤ x²+y²+z² ≤ 12 with frames. Replace the east, south, west and north face centers with optical ports; the top stays frame and the bottom is the controller. Other cells stay empty. Only horizontal ports transmit light.

The constrainer's default layout is 7×9×7. Build a two-layer 7×7 frame plinth with its controller at the front center of the upper layer. Relative to the controller, the plinth extends 0–6 blocks forward, three blocks to each side and one block down. The sphere center is three blocks forward and four blocks construction-up. Relative to that center, x/y/z range from -3 to 3. Shell cells satisfy 7 ≤ x²+y²+z² ≤ 12: use frames if any coordinate is zero, otherwise glass; leave other cells empty. Replace the upstream face-center frame with an optical port. The default materials are 138 frames, 56 glass, one port and one controller. Additional horizontal sphere-face centers may use optical ports. Align the core with an actual port and aim the port toward it.

## Beam controls

|Device|Per-batch behavior|
|---|---|
|Emitter|Pays 100,000 FE and emits 100,000 FE strength with six-block range|
|Constraint matrix|Pays 100,000 FE and renews six blocks of range|
|Enhancement matrix|Adds and pays the selected 100,000–9,000,000 FE, in steps of 100,000|
|Burst matrix|Pays 64 EHV and doubles beam strength|
|Deflector|Routes horizontal inputs equally across enabled outputs|
|Focus matrix|Focuses on the collapse core at the seventh block ahead|

Enhancement and burst preserve the remaining range. Maximum individual beam strength is 1P FE. Matrices without valid incoming light pay no running cost.

Each deflector direction button cycles off, input and output. New deflectors start with all four directions off. Multiple input and output directions are allowed. Closing all outputs stops downstream light; active upstream devices still follow their own charging rules. Top and bottom cannot be selected.

Valid inputs in the same batch merge into one total, divided equally among enabled outputs. Downstream devices receive the combined beam. Repeated updates do not charge the same batch again. Removing one source leaves the remaining contributions active. A total above 1P FE stops that deflector's outputs.

The focus path clears breakable blocks on its axis before the core. Unbreakable blocks stop it. The core activates at at least 1G FE and creates one particle for each accumulated 512G FE, preserving the remainder. Laser paths and active constraint fields damage entities. Constraint fields have a 2.5-block radius, prevent natural spawning while formed and deal damage only while working. Damage checks run every two ticks; a hit uses the highest overlapping damage, calculated as beam FE / 10,000 × the configured multiplier, capped at 1,000,000 damage points. The default multiplier is 1.

## Particles and sealed consumables

The constrainer holds 125,000 particles and 1024P FE. Each particle represents 512G FE of incoming laser energy. Inserted consumables cannot be withdrawn through the menu or automation. One item pays for 100 effective working ticks. Missing particles or room for a whole conversion pauses the paid timer.

The following are average capacities per 20 effective ticks, with sufficient particles and output space. G=1,000,000,000 FE; P=1,000,000,000,000,000 FE.

|Consumable|Input capacity / 20 ticks|Average output / 20 ticks|Conversion ratio|Output per whole particle|
|---|---|---|---|---|
|Nether Star|1G FE|1G FE|100%|512G FE|
|Netherite Block|16G FE|16G FE|100%|512G FE|
|Singularity|256G FE|128G FE|50%|256G FE|
|Overloaded Singularity|1024G FE|2048G FE|200%|1024G FE|
|Plutonium / Polonium pellet, optional Mekanism|0.125P FE|0.5P FE|400%|2048G FE|
|Transcendent Crystal|0.125P FE|0.5P FE|400%|2048G FE|
|Antimatter pellet, optional Mekanism|1P FE|8P FE|800%|4096G FE|

Divide the two capacity columns by 20 for average FE/tick. Each conversion consumes one whole particle and outputs its whole yield at once. One effective tick can perform multiple conversions when its processing allowance, particle stock and output space permit. Fractional processing credit persists between paid fuel windows and in saves. For example, a Nether Star rule with particles available converts one particle after 10,240 effective ticks and outputs 512G FE then; it does not output 1G FE immediately every 20 ticks. A single 100-tick item does not guarantee its nominal average output. Only the sub-particle fraction is retained; unavailable particles or output space do not bank an unlimited later burst.

Laser devices draw network FE from Applied Flux FE storage cells. The constrainer exports to those cells and debits only accepted FE. Empty source cells, full destination cells or absent Applied Flux stop the respective transaction. AE energy cells power the network. External FE connections and registered energy sinks also work.

When explosions are enabled, particle overflow or dismantling a constrainer containing particles triggers a spherical explosion with a 12-block radius around its optical center, including explosion sound and particles. Stop the upstream beam and consume stored particles before dismantling. Empty structures can be adjusted normally.

## Automatic construction

Place and orient a controller, open its menu and click Automatic build in the left toolbar. Survival uses main inventory and offhand first, then the controller's active connected AE network. Direct cabling works before formation. Inventory-only construction works offline; Creative needs no materials.

Only missing parts are placed and legal interfaces or selected receiver ports are preserved. Missing materials, obstacles or unloaded cells prevent construction. Cancelled placement refunds materials; refunds rejected by both network and player inventory drop for the player. Repeating a complete build uses no materials. The projection button previews positions and clears after formation. Right-click an owned frame to open its controller; sneaking and wrench use keep their normal interactions.

Creative administrators can use `/overload_sim laser build <kind>` in loaded empty space. Kinds are `emitter`, `constraint`, `enhancement`, `burst`, `deflector`, `focus`, `core` and `receiver`. `/overload_sim laser demo` builds a chain supplied with finite resources. See [High Energy Workstation](high-energy-workstation.md) for processing recipes with laser power.

## Configuration and API

`config/overload_sim-laser.toml` contains `emitterFePerBatch`, `constraintFePerBatch`, `burstExtremeLightningPerBatch`, `laserDamageMultiplier`, `explosionEnabled` and `explosionBlockBudgetPerTick`. Defaults are 100,000 FE, 100,000 FE, 64 EHV, 1, true and 1024 scanned cells/tick. Setting `explosionEnabled` to false disables explosion sound, particles, damage and destruction and discards pending work. The legacy `explosionVerticalRadius` no longer changes the spherical range. Explosion work respects unbreakable blocks and protection events, visits loaded chunks only and does not resume after a server restart.

Fuel rules are `data/<namespace>/particle_conversion/<id>.json`, reloaded with `/reload`. For example, override `data/overload_sim/particle_conversion/nether_star.json`:

```json
{
  "type": "overload_sim:particle_conversion",
  "ingredient": {"item": "minecraft:nether_star"},
  "consumable_count": 1,
  "consumable_duration_ticks": 100,
  "input_energy_per_batch": "1GFE",
  "energy_per_particle": "512GFE"
}
```

`ingredient` specifies exactly one item or item tag. `input_energy_per_batch` is the 20-tick input capacity; `energy_per_particle` is the whole-particle output, so their ratio to 512G FE defines conversion efficiency. Energy accepts an exact integer FE value or a unit string such as `kFE`, `GFE` or `PFE`. Input capacity must be from 1 FE to 1P FE per batch, and whole-particle output must be a positive integer FE amount. A yield larger than the 1024P FE buffer cannot process. Paid duration is 1–1,000,000 ticks; each window consumes one item and each completed conversion one particle. Rules match in stable ID order, so override the existing ID when changing a built-in.

For interval-based rules, use `particle_interval_ticks` instead of `input_energy_per_batch`. The fields are mutually exclusive. The interval must not exceed the paid duration. Removed or incompatible active rules pause paid work; missing optional materials skip only their own rules. `ParticleFuelData.diagnostics()` reports rule errors.

Register Java hooks through `dev.overloadsim.api.LaserExtensions` during common setup. `registerModifier` changes beam strength/range; `registerTarget` handles a beam and `deactivate` releases its source; `registerFuelProvider` supplies stable `match/resolve` fuel IDs; `registerEnergySink` returns the actually accepted long FE amount and must not mutate resources when simulating. Duplicate IDs are rejected. The reference is `docs/laser-particle-api.md`.
