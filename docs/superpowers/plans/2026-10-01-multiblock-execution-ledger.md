# SDD ledger — plan: docs/superpowers/plans/2026-10-01-multiblock-simulation.md

Spec approved by user “开始吧”. Base design b626269, runtime f317c18 / alpha.8.

Ruling: execute continuously on the existing independent D:/MinecraftDev/OverloadSimulation checkout and a dedicated codex feature branch — the calling app workspace is an unrelated old Forge repository; no concurrent implementers share this checkout — costs no isolated copy, so preserve clean checkpoints.
Ruling: retain this tracked ledger instead of Unix-only helper workspace scripts — Windows task must remain recoverable — record all tests and decisions here.
Ruling: approval already covers implementation, Blockbench assets and direct PCL test-addon update — no additional plan approval gate.

Preflight: Task 1 owns arithmetic/bulk interfaces consumed by 3/4; Task 2 owns immutable structure and membership consumed by 3/5; Task 3 separates paid job data from client snapshots consumed by 4/5. Task 5 exports must match runtime assets before delivery.

Task 1: complete. RED test/runGameTestServer failed on missing rules and buffer classes. GREEN: 10 JUnit + 57 GameTests passed (multiblock-task1-green.log); legal/nonstackable prototypes, component identity, 1024 quantities and NBT round-trip verified. Commit 8cb (see git log for full identity).

Task 2: in progress. Immutable validator and passive members; controller is the only ticker.
