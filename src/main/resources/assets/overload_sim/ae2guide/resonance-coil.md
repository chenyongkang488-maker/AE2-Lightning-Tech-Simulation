---
navigation:
  title: Resonance Lightning Coil
  icon: overload_sim:resonance_coil
  parent: overload_sim:overload-simulation.md
item_ids:
  - overload_sim:resonance_coil
  - overload_sim:extreme_voltage_module
  - overload_sim:mimic_tool_module
  - overload_sim:ultimate_destruction_module
  - overload_sim:efficiency_module
  - overload_sim:fortune_module
  - overload_sim:silk_touch_module
---

# Resonance Lightning Coil

Insert the coil into a networked Overload Device Workbench, install an Overload Core, and remove it. The workbench binds the coil to its grid. Keep the workbench online and loaded. Holding the tool or leaving it inside the online workbench continuously recharges its FE using AE2's standard conversion.

Tap and release right-click to strike the nearest unobstructed entity or block within 32 blocks. Hold for at least 1.5 seconds and release to strike yourself. Each strike costs exactly 10 HV and has a 0.5-second cooldown. The Extreme Voltage Module unlocks natural mode in the G configuration screen, costing exactly 10 EHV. Natural mode uses the upstream natural-weather classification for collectors and rituals. Lightning retains normal damage and fire.

Hold a blank simulation crystal in your offhand near a mob, then charge a self-strike to attempt the crystal's 10% mob binding. Continue striking bound crystals to cultivate them.

The Mimic Tool Module grants netherite-tier universal harvesting and sword combat. Sneak-use strips logs, tills soil or flattens paths. Its four dependent modules provide unrestricted harvesting and unbreakable-block mining, efficiency up to 10, fortune up to 5, and toggleable silk touch. Enabled silk takes priority over fortune. Remove dependent modules before removing Mimic. Each module has a one-item limit.

The core is required for every action. Mining and melee cost 200 FE by default. Base capacity is 10 million FE; upstream T1/T2/T3 energy modules replace capacity with 1, 5 or 20 billion FE. Only one energy module may be installed. Removing it clamps stored FE to the reduced capacity. Normal block loot and protection events remain in effect, including for ultimate mining. Bedrock has no default drop.

Pack authors can override standard crafting recipe JSON and the `resonanceCoil` range, charge, cooldown, mining cost and charge rate in the common configuration.
