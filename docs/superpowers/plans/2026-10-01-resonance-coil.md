# Resonance Coil Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for inline execution. Steps use checkboxes for tracking.

**Goal:** Implement the requested modular lightning tool and its Blockbench model.
**Architecture:** Independent item components and module adapter integrate with the pinned upstream workbench. Server item lifecycle handles lightning targeting and mining; configuration is a validated item menu.
**Tech Stack:** Java 21, Minecraft 1.21.1, NeoForge 21.1.252, AE2 19.2.17, AE2LT 2.1.0, Blockbench.
**Spec:** ../specs/2026-10-01-resonance-coil-design.md

## Global Constraints

- Keep all prior chamber/crystal behavior and PCL Java/memory settings.
- No upstream enum extension or replacement of existing workbench adapters.
- Strict 10 HV or 10 EHV debit; only EHV sets the natural-weather marker.
- All four mining extensions require mimic; efficiency 0–10, fortune 0–5; silk has priority.
- Preserve existing AE2LT/derived-art attribution; new coil art is original.

## Review Focus

- Switching the held stack while configuring must invalidate the menu.
- Network or event failures must not consume lightning or FE without an action.
- A gun or armor on the same workbench must keep its existing adapter.
- Ultimate breaking must honor canceled break events and permission checks.
- Removing an energy module must never leave usable FE above capacity.

### Task 1: Workbench and persistence

Files: `tool/CoilModules.java`, `tool/CoilSettings.java`, `tool/CoilEnergy.java`, `compat/CoilWorkbenchAdapter.java`, `mixin/CoilWorkbenchMixin.java`, `ModContent.java`, `gametest/CoilGameTests.java`.

- [ ] Write and run real-workbench acceptance test; old jar/item registry fails.
- [ ] Add components, module registration, API install/uninstall with count and prerequisite checks, core slot and energy capacity/charging.
- [ ] Verify real workbench, serialization, dependency/energy tests. Save local commit.

### Task 2: Lightning and mining

Files: `tool/ResonanceCoilItem.java`, `tool/CoilLightning.java`, `tool/CoilMining.java`, `tool/CoilEvents.java`, `SimulationConfig.java`.

- [ ] Add GameTests asserting exact HV/EHV stock, no-core/no-target/no-funds, block/entity/self target and unbreakable restrictions.
- [ ] Implement `releaseUsing`, nearest unobstructed targeting, payment rollback, bolt marker, netherite harvest and dynamic enchants, ultimate breaking through server game mode.
- [ ] Run tests and verify actual loot, canceled events, and charge-release boundaries. Save local commit.

### Task 3: Configuration and art

Files: `tool/CoilMenu.java`, `tool/CoilPackets.java`, `client/CoilScreen.java`, `client/CoilClient.java`, `art/resonance_coil_workshop.js`, `art/resonance_coil.bbmodel`, item assets, language/guide/recipe JSON.

- [ ] Validate config clamping, module effects, stale hand/menu rejection.
- [ ] Add G key dispatch using the existing mapping, server menu and control packets, electric particles and charging feedback.
- [ ] Build/draw/export in Blockbench; inspect exported bounds, UVs, animation frame data, hand transforms.
- [ ] Build and launch developer client for model/config/workbench/mining; fix concrete findings.

### Task 4: Review and install

- [ ] Run complete JUnit and GameTests plus jar/resource verification.
- [ ] Request one fresh read-only whole-change reviewer, resolve findings, rerun affected checks.
- [ ] Bump alpha version, record limitations and usage, backup/update PCL test jar and verify hash.
- [ ] Save Git commit and rollback tag. Leave model open in Blockbench.
