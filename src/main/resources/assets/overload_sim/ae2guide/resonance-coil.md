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
  - overload_sim:wrench_module
---

# Resonance Lightning Coil

Insert the coil into a networked Overload Device Workbench, install an Overload Core, and remove it. The workbench binds the coil to its grid. Keep the workbench online and loaded. Holding the tool or leaving it inside the online workbench continuously recharges its FE using AE2's standard conversion.

Tap and release right-click to strike the nearest unobstructed entity or block within 32 blocks. Hold for at least 1.5 seconds and release to strike yourself. Each strike costs exactly 10 HV and has a 0.5-second cooldown. The Extreme Voltage Module unlocks natural mode in the G configuration screen, costing exactly 10 EHV. Natural mode uses the upstream natural-weather classification for collectors and rituals. Lightning retains normal damage and fire.

Hold a blank simulation crystal in your offhand near a mob, then charge a self-strike to attempt the crystal's 33% mob binding. A real lightning hit is sufficient without taking damage, including Creative mode and canceled vanilla strike effects. Continue striking bound crystals with artificial or natural lightning to cultivate them.

The Mimic Tool Module grants netherite-tier universal harvesting and sword combat. Sneak-use strips logs, tills soil or flattens paths. Its four dependent modules provide unrestricted harvesting and unbreakable-block mining, efficiency up to 10, fortune up to 5, and toggleable silk touch. Enabled silk takes priority over fortune. Remove dependent modules before removing Mimic. Each module has a one-item limit.

The core is required for every action. Mining and melee cost 200 FE by default. Base capacity is 10 million FE; upstream T1/T2/T3 energy modules replace capacity with 1, 5 or 20 billion FE. Only one energy module may be installed. Removing it clamps stored FE to the reduced capacity. Normal block loot and protection events remain in effect, including for ultimate mining. Bedrock has no default drop.

Pack authors can override standard crafting recipe JSON and the `resonanceCoil` range, charge, cooldown, mining cost and charge rate in the common configuration.


## Shared device hub and wrench module

G opens the original Lightning Tech device hub. Its top-right weapon tab displays the held coil or railgun, main hand first. The module list and settings scroll.

Install the Wrench Module at the workbench; Mimic is unnecessary, but a core is required. Enable it in G to disable all right-click lightning, including an already charged release. AE rotation and sneak-disassembly follow the native rules and return stored contents. No additional FE or lightning is charged.

With optional Mekanism installed, choose Wrench, Items, Fluids, Chemical, Energy, Heat, Empty or Rotate. Close the GUI, hold an enabled wrench coil in your main hand, and use Shift + scroll to cycle forward or backward; the action bar shows the current mode and the hotbar stays selected. Native configurator actions preserve security and pipe behavior; configuration modes cycle sides on sneak-use. Empty mode drops inventory contents. Without Mekanism, AE wrench use remains available. Turning wrench mode off restores lightning; removing its module or core removes the wrench abilities.
