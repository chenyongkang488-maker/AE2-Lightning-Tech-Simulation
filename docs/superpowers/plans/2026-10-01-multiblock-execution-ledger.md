# SDD ledger — plan: docs/superpowers/plans/2026-10-01-multiblock-simulation.md

Spec approved by user “开始吧”. Base design b626269, runtime f317c18 / alpha.8.

Ruling: execute continuously on the existing independent D:/MinecraftDev/OverloadSimulation checkout and a dedicated codex feature branch — the calling app workspace is an unrelated old Forge repository; no concurrent implementers share this checkout — costs no isolated copy, so preserve clean checkpoints.
Ruling: retain this tracked ledger instead of Unix-only helper workspace scripts — Windows task must remain recoverable — record all tests and decisions here.
Ruling: approval already covers implementation, Blockbench assets and direct PCL test-addon update — no additional plan approval gate.

Preflight: Task 1 owns arithmetic/bulk interfaces consumed by 3/4; Task 2 owns immutable structure and membership consumed by 3/5; Task 3 separates paid job data from client snapshots consumed by 4/5. Task 5 exports must match runtime assets before delivery.

Task 1: complete. RED test/runGameTestServer failed on missing rules and buffer classes. GREEN: 10 JUnit + 57 GameTests passed (multiblock-task1-green.log); legal/nonstackable prototypes, component identity, 1024 quantities and NBT round-trip verified. Commit 3cec352.

Task 2: complete. Missing validator RED observed. GREEN: 59 GameTests passed; all five outer sizes, capacities, glass removal/restoration, solid interiors and duplicate special modules verified. Fixed AE onRemove visibility from exact compiler diagnostic. Frame/member entities have no ticker.

Task 3: complete. RED missing batch class. GREEN 62 GameTests + 12 JUnit: exact base per-crystal charges; reload/pause preserves fees; full upgrade 49 crystals / 38 ticks / 100352 ingots / 147 HV + 49 EHV; separate partial-payment refund debt. Data policy and raw-smelting/alias extension listener added. Original interface connects through public GridHelper, exports accepted quantities only.
Ruling: formed validation runs once per 100 ticks as a bounded fallback, unformed every 20; part removal invalidates immediately — cached structure avoids scanning every production tick — detects third-party internal placements within five seconds.

Task 4: complete. RED missing menu. GREEN 63 GameTests including legal left/right/shift transfers and stale revision rejection. AE native layout supports only 9/3/2-column enums; use two nine-column panels rather than unsupported seven/eight-column identifiers. Server quantity snapshots carry legal prototypes, current page and a revision.

Task 5: in progress. Blockbench opened and target window selected with computer-use skill.

Additional integration check: 65 GameTests passed, including original-interface grid bridging, exact 2048-item AE export and packed controller-item restoration.
Blockbench existing visual plugin reload prompts for local-file access. Requested human operation because computer-use guidance forbids acting on security/privacy permission requests. Continue independent validation while pending; no approval inferred from elapsed time.

Task 6 code review: the single fresh read-only reviewer identified member resurrection/replacement loss, stale membership and missing recovery retries, sparse fairness, unpaid shrink, reload caching, channel bypass, removed-port export, original-glass loot, legacy pickaxe-tag loss and event inheritance. Eleven RED tests reproduced the initial causes; GREEN 76 GameTests + 12 JUnit after focused fixes. Two additional lifecycle tests passed in the 78-test Mek run. The actual-shrink test initially recreated the controller in its helper; corrected the helper to preserve the entity and asserted identity explicitly.

Ruling: unpaid caches carry a process UUID and resource generations — JVM-local counters alone collide after offline recipe changes — unpaid outcomes reset after server restart, paid journals retain their saved outputs/time/costs. A regression reproduced the collision with the session token removed, then restored the token for final validation.

Delivery pending: no alpha.9 release tag or PCL addon replacement until Blockbench exports and client verification finish. Keep alpha.8 installed while the human handles the native plugin permission prompt.

Final source GREEN: 79 GameTests + 12 JUnit pass both normal and -PmekTests runs (multiblock-final-source-green.log, multiblock-final-source-mek-green.log). Existing PCL 11 mod hashes match the before snapshot. Authoring JavaScript syntax checks pass; exports are still absent. Task 5 and delivery portions of Task 6 remain pending the human Blockbench permission action.

Task 5: complete after human file-access permission. Executed native Blockbench authoring action; 17 layered projects, 18 embedded PNGs and 16 runtime models verified. Corrected glass UV resolution metadata without changing PNG pixel data. Native client verified formed 5³ geometry, crystals/arcs, production FE refill, four-page UI, legal 64/1-item transfers and GuideME help. Compact 352×240 UI fits 854×480 client; quantity labels draw above item icons. Three native Minecraft screenshots tracked in art/verification. Both runClient sessions exited successfully and saved only the developer world.

Task 6: complete. One whole-branch read-only review and its regression fixes were completed before export; no additional review agents needed. Final packaged resources/client build with Mek: 79 GameTests pass, 12 JUnit pass, BUILD SUCCESSFUL / exit 0 (multiblock-release-alpha9.log). Verified 53 packaged assets byte-for-byte, alpha.9 metadata and no bundled dependency classes. Final jar 466208 bytes / SHA-256 364ad14da5cd8df975b141716b2ececae5953ebb451dd6ff1e5267c0333c6362.

Installed only alpha.9 addon into the established PCL instance; alpha.8 backed up to addon-backups/20261001-164652 with original hash. Other 10 mods and all 46 settings/configuration files unchanged. Keep codex/multiblock-simulation as the local development branch and tag v0.1.0-alpha.9 for the user's requested rollback workflow; no remote publication requested. All approved plan tasks are complete.
