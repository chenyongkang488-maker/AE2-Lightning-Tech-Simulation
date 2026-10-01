# Multiblock Visual Redesign Implementation Plan

> Execute inline with superpowers:executing-plans. User explicitly requests implementation immediately after the revised design; do not add another approval handoff. One fresh whole-branch reviewer after implementation.

**Goal:** Deliver alpha.10 with a seamless white/pink chamber, frame roof, voxel energy orb, integrated controller/native port and adaptive compact UI.

**Architecture:** Cached shell topology generates static block quads through model data; the existing controller renderer draws only animated orb/arcs/screens. Stable 49 server input slot IDs are arranged by a pure layout helper on the client, preserving inactive item retrieval.

**Tech Stack:** MC 1.21.1, NeoForge 21.1.252, AE2 19.2.17, AE2LT 2.1.0, Java 21, Blockbench desktop.

**Spec:** ../specs/2026-10-02-multiblock-visual-redesign-proposal.md

## Global constraints

- Outer sizes 3..7; capacity N²; frame roof, four vibrant-glass sides, existing upgrade floor.
- Preserve all production fees, policies, saved journals, buffers and native interface functionality.
- Working-only voxel orb; pink normally, orange for a smelting paid batch; no flame; optional overload orbit and corner arcs.
- Inventory IDs stay stable; 32 visible bulk slots / four pages / 1024 quantities; N×N inputs, central vertical progress, no HV/EHV stock totals and no blank backpack side panels.
- All raster painting/3D authoring stays in Blockbench; static assets must match editable project textures.

## Review focus

Roof migration preserving paid batches; hidden inactive inputs after shrink; conditional native-interface skin and original function restoration; multithreaded model cache and cross-chunk/level lifecycle; no transparent depth fighting and working visual states matching saved batch flags.

## Tasks

### Task 1 — Roof and layout contracts
Files: SimulationStructureValidator, MultiblockGameTests, core/MultiblockMenuLayout, test/core/MultiblockMenuLayoutTest, MultiblockSimulationMenu/Screen, multiblock.json.
- [ ] RED: reject a glass roof; accept frame roof for all five sizes and restore old materials without losing paid journals.
- [ ] RED: layout `of(capacity, recovery)` produces square input positions, central progress, eight-column outputs, compact lower inventory and legal viewport height.
- [ ] GREEN: implement roof rule and stable-slot adaptive client arrangement; remove HV/EHV stock querying/display. Native UI verification follows Task 4.
- [ ] Full server/unit suite; checkpoint.

### Task 2 — Shell topology and lifecycle
Files: core/SimulationShellTopology, multiblock/SimulationShellVisuals, client/SimulationShellModel, ClientRegistration, SimulationControllerBlockEntity.
- [ ] RED topology test: each edge axis, corners, roof center, outward special face and invalid interior positions.
- [ ] Implement `role(size,x,y,z)` / `outsideFace(size,x,y,z)` and client visual snapshots, preserving owner-instance identity across update/removal.
- [ ] Decorate formed custom models and the native overloaded_interface model with position-aware cached quads; keep item/unformed native models unchanged.
- [ ] Lifecycle regressions and full suite; checkpoint.

### Task 3 — Blockbench authoring and dynamic renderer
Files: art/multiblock_simulation_workshop.js, authored bbmodels/PNGs/models, MultiblockSimulationRenderer, verification scripts.
- [ ] Redraw casing, continuous rail/corner/roof textures, seamless glass, black controller screen, integrated port, pixel orb with pink/orange flow and ECG layers using native Blockbench action.
- [ ] Extend resource verification for project/PNG parity, animation frame sizes, required topology textures and removed borders.
- [ ] Replace displayed crystal grid/fire with one working voxel orb, colored by batch flags; retain corner and overload arcs, add ECG display.
- [ ] Compile and verify authoring/resource exports; checkpoint.

### Task 4 — Client review and delivery
Files: guides/lang/README/CHANGELOG/verification docs, versions/build/install-test scripts.
- [ ] Prepare developer fixtures for all sizes, shrink inventory retrieval, controller/port and normal/smelt/overload visual states; inspect native client and save screenshots.
- [ ] One fresh read-only whole-branch review, fix Important findings with reproducing tests.
- [ ] Build and test with/without Mek, check packaged assets and no bundled dependencies.
- [ ] Back up and install only alpha.10 addon in existing PCL instance; verify other mods/settings unchanged, commit and tag v0.1.0-alpha.10.
