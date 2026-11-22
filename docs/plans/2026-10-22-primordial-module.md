# 原初模块 — 0.2.2-alpha.4

Approved specification: physical floor module, max one, mutually exclusive with smelting; mineral output is one natural ore block times chamber fortune. Successful conversions add 2 HV per crystal; unsupported minerals retain normal production without surcharge. Other crystal kinds are unchanged. Explicit datapack mappings precede vanilla defaults and unique material ore tags. Started batches retain fixed outcomes and costs across module changes and reloads. Recipe: frame + silk-touch book, 100000 FE + 32 EHV. Preserve public overloads and old saves. No instance installation or publication; ask before windowed acceptance.

## Execution ledger

- Baseline: 7ee6724, clean codex/matrix-six-ports worktree reused.
- Pre-flight: structure state feeds planner, policy signature, persistence and menu. Converted counts must accompany the fixed batch so mixed fees and fallback diagnostics agree after restart.
- Ruling: fees remain debited at completion, matching current settlement version 2; an already started unpaid batch still retains its fixed outcome, as explicitly approved.
- Task 1: RED registration/formation test, then module identity and mutual exclusion.
- Task 2: ore resolution, mixed planning, snapshots and compatibility tests.
- Task 3: resources, recipe, UI and bilingual documentation.
- Task 4: current/legacy runtime suites, release integrity and independent review.

## Final automated verification (2026-10-22)

- Implemented registration, floor/alias constraints, resolver, mixed batch fees, persistence, UI, recipe, native texture and editable Blockbench model, bilingual guides and API documentation.
- RED: missing registration in `build/primordial-red.log`; dimension-dependent Java material cache in `build/primordial-dimension-red.log`. Both were reproduced before implementation/fix.
- Fixed existing GameTest global configuration leakage with resource-manager snapshot/restore in finally.
- Independent review found the dimension cache issue; ServerLevel identity plus generation now invalidates the cache. Follow-up review found no remaining important issues.
- Final current stack (AE2 19.2.18 / AE2LT 2.1.2-beta2 / Thunderbolt 2.0.3-beta2, JEI and AppliedFlux): 558/558 required GameTests passed.
- Final legacy stack (AE2 19.2.17 / AE2LT 2.1.0 / Thunderbolt 2.0.0, JEI): 558/558 required GameTests passed.
- JUnit: 143 tests, zero failures/errors/skips. All six resource check scripts, verifyReleaseVersion, build, sourcesJar and verify-release.py passed.
- Final packaging rebuilt after restoring semantically unchanged existing recipe line endings. No production behavior changes after the final test runs.
- Runtime Java: D:/DevTools/Java/jdk-21.0.12.1+1; GRADLE_USER_HOME=D:/DevTools/GradleCache. Offline commands, quote full PowerShell -Pname=version arguments.
- Final logs: `build/verification/primordial-alpha4/{current,legacy,package}.log`; evidence and hashes: `docs/releases/0.2.2-alpha.4-verification.{md,json}`.
- Source ZIP includes code, resources, editable art, scripts and docs; excludes this release's verification records to avoid a self-referential artifact hash.
- Window acceptance authorized anew on 2026-10-22 and completed in a copy named alpha24-primordial-review. Verified mixed ore/crop/fallback output, actual fees, duplicate/smelting constraints, JEI assembly recipe including Silk Touch book, native model and icon, and production after save/restart/reload. Client saved and closed normally.
- Window discovery: chamber status 6 reused the single-block missing-profile label. Corrected only MultiblockSimulationScreen status 6 to a new chamber-specific translated label; reproduced incorrect label before fix and observed corrected label after restart. Original pre-visual artifacts preserved in build/verification/primordial-alpha4/before-visual-fix/.
- Final full current/legacy regression after this presentation correction passed 558/558 each; logs current-final.log / legacy-final.log. JUnit remains 143 tests, no failures/errors/skips. No instance JAR install, push, tag or remote publication.
