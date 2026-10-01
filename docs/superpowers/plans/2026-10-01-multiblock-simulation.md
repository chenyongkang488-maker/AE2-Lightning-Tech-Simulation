# Multiblock Simulation Implementation Plan

> Execute inline with superpowers:executing-plans; one fresh whole-branch review after implementation.

Goal: implement the approved 3–7 block hollow simulation chamber on Minecraft 1.21.1 / NeoForge.
Spec: ../specs/2026-10-01-multiblock-simulation-design.md

Architecture: one AE-connected controller owns a cached structure, legal crystal inventory, a keyed bulk buffer, and a persisted paid batch. Passive members retain ownership and original glass states. Blockbench resources and a controller renderer provide formed visuals.

Global constraints: outer sizes 3–7; capacity N²; air interior; frame edges, five glass faces, upgrade floor; exactly one controller and at most one original overload interface on non-corner bottom edges. Each crystal costs 1000 FE + 1 HV, plus 1 EHV for overload and 2 HV for smelting. Base duration 180, reductions capped 104, overload halves final duration, fortune caps at 1024. Each of 128 bulk slots stores a legal prototype and separate quantity ≤1024. Preserve existing single-block behavior.

## Tasks

1. Production arithmetic and bulk buffer
   - RED JUnit arithmetic tests and GameTests for legal prototypes, component identity, simulation and persistence.
   - Implement MultiblockRules and BulkOutputBuffer; run GREEN; commit.
2. Structure and content
   - RED size/material/duplicate/ownership validation tests.
   - Register eight blocks, controller and passive member entities, hidden formed glass; implement validation, formation/restoration and cached ownership; run GREEN; commit.
3. Production, integration and API
   - RED fees, timing, outputs, persistence and capacity tests.
   - Implement paid batches, data-driven rules/smelting, events, FE/HV/EHV payments and original-interface grid bridge; test GREEN; commit.
4. Menu
   - RED legal-stack extraction and stale action validation.
   - Add native-style paginated crystal and bulk output UI with diagnostics and server validation; test GREEN; commit.
5. Blockbench and rendering
   - Build editable layered Blockbench assets; export and inspect parity.
   - Add connected frame/glass models, floating crystals, four coils, arcs and optional fire; verify client loading; commit.
6. Review and delivery
   - One fresh read-only whole-branch review; fix significant findings.
   - Complete build, GameTests with/without Mek and unit tests; inspect packaged resources.
   - Update docs and alpha.9 metadata, back up/install only addon into PCL, verify hashes; commit and tag.

Review focus: paid job versus full buffer/reload/structure loss; component-sensitive and nonstackable output; glass restoration/unloaded chunks/overlap; separate HV/EHV and partial refunds; module change/shrink/stale UI.
