# SDD ledger — plan: docs/superpowers/plans/2026-10-01-multiblock-simulation.md

Spec approved by user “开始吧”. Base design b626269, runtime f317c18 / alpha.8.

Ruling: execute continuously on the existing independent D:/MinecraftDev/OverloadSimulation checkout and a dedicated codex feature branch — the calling app workspace is an unrelated old Forge repository; no concurrent implementers share this checkout — costs no isolated copy, so preserve clean checkpoints.
Ruling: retain this tracked ledger instead of Unix-only helper workspace scripts — Windows task must remain recoverable — record all tests and decisions here.
Ruling: approval already covers implementation, Blockbench assets and direct PCL test-addon update — no additional plan approval gate.

Preflight: Task 1 owns arithmetic/bulk interfaces consumed by 3/4; Task 2 owns immutable structure and membership consumed by 3/5; Task 3 separates paid job data from client snapshots consumed by 4/5. Task 5 exports must match runtime assets before delivery.

Task 1: complete. RED test/runGameTestServer failed on missing rules and buffer classes. GREEN: 10 JUnit + 57 GameTests passed (multiblock-task1-green.log); legal/nonstackable prototypes, component identity, 1024 quantities and NBT round-trip verified. Commit 8cb (see git log for full identity).

Task 2: complete. Missing validator RED observed. GREEN: 59 GameTests passed; all five outer sizes, capacities, glass removal/restoration, solid interiors and duplicate special modules verified. Fixed AE onRemove visibility from exact compiler diagnostic. Frame/member entities have no ticker.

Task 3: complete. RED missing batch class. GREEN 62 GameTests + 12 JUnit: exact base per-crystal charges; reload/pause preserves fees; full upgrade 49 crystals / 38 ticks / 100352 ingots / 147 HV + 49 EHV; separate partial-payment refund debt. Data policy and raw-smelting/alias extension listener added. Original interface connects through public GridHelper, exports accepted quantities only.
Ruling: formed validation runs once per 100 ticks as a bounded fallback, unformed every 20; part removal invalidates immediately — cached structure avoids scanning every production tick — detects third-party internal placements within five seconds.
