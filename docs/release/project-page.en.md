# Lightning Tech: Simulation

By qiqi. An independent Minecraft 1.21.1 / NeoForge addon for AE2 Lightning Tech.

Grow reusable simulation crystals with lightning, then produce minerals, crops, wood and mob loot in simulation chambers. Single-block chambers support up to 128 parallel operations; multiblock chambers hold up to 49 crystals. The modular Overload Thunder Coil offers lightning, mining and wrench functions.

Current version **0.2.1** also includes the complete laser and particle production line, high-energy workstation, Transcendent Crystals and the 36-slot Overload Output Interface. The additions are described below.
## Laser and particle production

Build a high-energy laser emitter and route its beam through constraint, enhancement, burst, deflection and focusing matrices. Deflectors combine multiple inputs and distribute their energy across enabled output faces. A focused beam feeds the collapse core; emitted particles continue towards the particle constrainer, which converts them into FE using compatible consumables.

The line includes survival recipes, operating information and G-key guides. Multiblock automatic construction takes materials from the player's inventory first, then from the connected ME network, with a construction preview showing missing or obstructed positions.

## High-energy workstation and Transcendent Crystal

The 7×7×7 workstation uses the shared laser frame, glass and optical port. By default, a single beam must exceed 1GFE to drive it. It inherits overloaded processing-factory recipes, consumes their materials, and replaces their original FE/lightning costs with laser power; the default limit is 1024 operations every two ticks.

The workstation supports large item buffers, fluid tanks and container interaction. Adjacent-container output and ME export have separate switches. JEI pages show workstation recipes and their laser requirement.

256 Overload Crystals can produce four pale-blue Transcendent Crystals in the workstation. A Tesla coil fitted with a Lightning Collapse Matrix can consume them to produce EHV lightning. They also work as particle-constrainer consumables; G guides and JEI describe the requirements.

## Overload Output Interface

Configure 36 virtual resource slots across two pages. Normal mode supplies adjacent containers; wireless mode sends each source slot's resource to selected target faces. Quantities are a shared per-slot transfer budget, rather than a target stock limit. Supply continues while the network has resources and the target accepts them. The interface has no collection or passive-input path.

Use the connector on the source interface, Shift-scroll to select a slot, then use it on a target face. Using it again on that face disconnects the selected slot. Ctrl bulk binding only adds missing links. The target-management page can remove individual links or clear a slot, and each source slot has its own connection color.

Multiple targets share a slot's transfer budget. Links follow the slot when its template changes; an empty slot pauses resource output. An Induction Card supplies FE to all valid connections, deduplicating repeated target faces. The default same-dimension range is 128 blocks; cross-dimension links are enabled, without force-loading target chunks.

## Configuration and fixes

The native mod configuration screen exposes machine parameters, capacities, wireless range and the particle-constrainer explosion switch. Lowering capacities preserves existing contents; multiplayer uses server settings.

This release fixes the Sodium chunk-mesh crash after automatic construction, caches shell lighting and culls off-screen geometry, and updates construction-preview rendering. Upgrade-card insertion and JEI entry points are improved. Client buttons, key bindings and connector rendering support the relocated APIs in AE2 Lightning Tech 2.1.2-beta2.

## Requirements and installation

Minecraft **1.21.1**, **Java 21**, and **NeoForge 21.1.252 or newer within 21.1.x**.
Recommended stack: NeoForge 21.1.255, AE2 19.2.18, AE2 Lightning Tech 2.1.2-beta2, Thunderbolt Core 2.0.3-beta2 and GuideME 21.1.19.

AE2, AE2 Lightning Tech, Thunderbolt Core and GuideME are required and are separate projects. Install one copy of each dependency; Reborn and the current main projects use the same mod IDs. Applied Flux and its dependencies are optional, but ME-network FE transfer requires Applied Flux FE storage cells. Ordinary AE energy cells only power the network. JEI, Mekanism and Mystical Agriculture integrations are optional; the Packaged Pattern Provider addon is not required.

Back up worlds and remove the older addon JAR before installing `lightning-tech-simulation-0.2.1.jar` on both client and server. Sources JARs and source ZIPs are development files. Existing `overload_sim` registry and save IDs are retained. See README for declared dependency ranges, and [release verification record](https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation/blob/v0.2.1/docs/releases/0.2.1-verification.md) for the actual release checks.

## Source and licenses

Source and issues: https://github.com/chenyongkang488-maker/AE2-Lightning-Tech-Simulation

Original code, documentation and artwork: MIT. Adapted output-interface code and layout: LGPL-3.0-only. Derived crystal, logo, module and output-interface artwork: CC BY-NC-SA 3.0. Full attribution, modification notes and license texts are included in the source repository and JAR.
