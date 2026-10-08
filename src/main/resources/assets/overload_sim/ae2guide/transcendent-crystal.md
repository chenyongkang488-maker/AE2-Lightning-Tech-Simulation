---
navigation:
  title: Transcendent Crystal
  icon: overload_sim:transcendent_crystal
  parent: overload_sim:high-energy-workstation.md
item_ids:
  - overload_sim:transcendent_crystal
---

# Transcendent Crystal

<ItemImage id="overload_sim:transcendent_crystal" scale="3" />

## Production

The [High Energy Workstation](high-energy-workstation.md) converts 256 Overload Crystals into 4 Transcendent Crystals. The recipe requires the workstation's individual laser beam above 1,000,000,000 FE and completes in two valid ticks, with up to 1024 parallel operations. It consumes materials only and is unavailable in the Overload Processing Factory.

## Tesla Coil

Place a Transcendent Crystal in the AE2 Lightning Tech Tesla Coil's consumable slot and a Lightning Collapse Matrix (`ae2lt:lightning_collapse_matrix`) in its matrix slot. Connect the coil to an online network with space for 64 Extreme High Voltage lightning.

Each operation uses one crystal and 500,000 FE, takes five effective ticks and outputs 64 EHV. It does not consume existing HV. Missing matrix, energy or network output space prevents completion.

## Particle Constrainer

Insert crystals into the constrainer's sealed consumable slot to convert particles into FE. Inserted consumables cannot be withdrawn. No optional Mekanism installation is required. The fuel rates, paid duration and pause conditions are listed in [Laser and particles](laser-particle.md).

## Data packs

Override `data/overload_sim/recipe/transcendent_crystal.json` to change workstation production. It uses type `overload_sim:high_energy_workstation`, a counted `inputs` array and a `results` array; see the workstation page for the complete format.

Override `data/overload_sim/particle_conversion/transcendent_crystal.json` to change constrainer fuel behavior. That format and its limits are on the laser page. Apply data-pack changes with `/reload`.
