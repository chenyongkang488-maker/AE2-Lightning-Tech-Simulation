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

The High Energy Workstation converts **256 Overload Crystals into 4 Transcendent Crystals**. This dedicated recipe is unavailable in the Overload Processing Factory. It uses the workstation's laser requirement, two-tick batches and up to 1024 parallel operations, consuming only materials. The crystal retains the upstream Overload Crystal silhouette and animation with a pale-blue palette.

Put the crystal into the original Tesla Coil's consumable slot and install a **Lightning Collapse Matrix** in its matrix slot. Each operation takes **five effective ticks**, consumes **one crystal and 500,000 FE**, and exports **64 Extreme High Voltage lightning** to the network. Existing High Voltage lightning is not consumed. The connected network needs room for the output.

It also works as a sealed Particle Constrainer consumable with the same effect as plutonium and polonium pellets: **100 effective working ticks per crystal**, consuming **one particle per tick** to produce **2048G FE**. Missing particles or insufficient buffer space pause the paid window. Mekanism is not required for this fuel.

Packs may override `data/overload_sim/recipe/transcendent_crystal.json` and `data/overload_sim/particle_conversion/transcendent_crystal.json`.
