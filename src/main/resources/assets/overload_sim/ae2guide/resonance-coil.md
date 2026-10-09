---
navigation:
  title: Overload Lightning Coil
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

# Overload Lightning Coil

Insert the coil into a networked Overload Device Workbench and install an Overload Core. The workbench binds the coil to its grid. Keep the workbench online and loaded to use network lightning remotely.

Holding the tool or leaving it inside the online workbench recharges it from Applied Flux FE storage cells, by default up to 10,000 FE/tick. Empty FE cells or absent Applied Flux stop this recharge. AE energy cells still power the ME network. External FE charging is also supported.

## Lightning

Tap and release right-click to strike the nearest unobstructed entity or block in sight, within 32 blocks by default. Hold for 30 ticks (1.5 seconds) and release to strike yourself. Each use produces one strike, costs exactly 10 HV and has a 10-tick (0.5-second) cooldown.

Install the Extreme Voltage Module and enable natural mode in G settings to spend exactly 10 EHV instead. Collectors and rituals recognize this as natural lightning. Insufficient stock of the selected voltage prevents use; the other voltage is not substituted. Lightning follows normal damage and fire rules.

With a blank simulation crystal in the offhand and a nearby mob, a charged self-strike attempts the crystal's 33% binding rule. A real hit is enough without taking damage, including Creative mode. Strike bound crystals to cultivate them; see [simulation crystals](overload-simulation.md).

## Modules and FE

Install modules at the workbench. Each of the seven coil modules has a one-item limit. The core is required for all actions.

|Module|Use|
|---|---|
|Extreme Voltage|Enables the natural-lightning setting|
|Mimic Tool|Netherite-tier harvesting, sword and shears abilities; sneak-use to strip logs, till soil or flatten paths|
|Ultimate Destruction|Requires Mimic; ignores harvest tier and can mine unbreakable blocks|
|Efficiency|Requires Mimic; toggle and adjust from 0 to 10, default 10|
|Fortune|Requires Mimic; adjust from 0 to 5, default 5|
|Silk Touch|Requires Mimic; enabled by default and takes precedence over Fortune|
|Wrench|Enables the wrench setting; no Mimic requirement|

Remove the four dependent modules before removing Mimic. Protection events still apply with Ultimate Destruction. Successfully mining bedrock or a Firmament Conversion Core returns one corresponding block item; cancelled drops produce no items. Place a recovered core in the Overworld to process its original recipes without a Firmament Starship. Supply all recipe materials and leave room for outputs.

Mining and melee each cost 200 FE by default. Base capacity is 10,000,000 FE. One upstream T1, T2 or T3 energy module replaces it with 1,000,000,000, 5,000,000,000 or 20,000,000,000 FE. Only one energy module may be installed; removing it retains stored FE and pauses further charging while above the remaining capacity. Lightning uses its lightning cost instead of mining FE.

## G settings and wrench

G opens Lightning Tech's device hub. Select the weapon tab at the top right; it uses the held coil or railgun, main hand first. The module list and settings can scroll.

Wrench mode defaults to off. With a core and Wrench Module installed, enable it in G to rotate AE blocks or sneak-use to disassemble them under native return rules. Wrench mode disables right-click lightning and charged releases. It charges no extra FE or lightning.

With optional Mekanism, choose Wrench, Items, Fluids, Chemical, Energy, Heat, Empty or Rotate. Close the GUI, hold the active wrench coil in the main hand and use Shift + scroll to cycle modes in either direction. Configuration modes use sneak-right-click to cycle side settings. Native security and pipe rules apply; Empty drops inventory contents. AE wrench use remains available without Mekanism. Removing the core or module disables wrench abilities; turning wrench mode off restores lightning.

## Configuration and Java API

Override standard crafting files at `data/<namespace>/recipe/<path>.json`. Set these values under `[resonanceCoil]` in `config/overload_sim-common.toml`:

```toml
[resonanceCoil]
lightningRange = 32
selfStrikeChargeTicks = 30
lightningCooldownTicks = 10
miningEnergyFe = 200
chargeFePerTick = 10000
```

Supported ranges are 1–256 blocks, 1–400 charge ticks, 1–400 cooldown ticks, 0–2,000,000 FE/action and 0–2,000,000 FE/tick respectively.

Java integrations use `CoilModules.install/uninstall` for validated module changes and `CoilConfiguration.apply(stack, action, value)` for settings. Installation returns a tool copy and does not deduct the supplied module; the caller must deduct one item. `CoilEnergy.INSTANCE` exposes FE charging. Read the full reference in `docs/api.md`.
